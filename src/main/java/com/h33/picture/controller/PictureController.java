package com.h33.picture.controller;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.h33.picture.annotation.AuthCheck;
import com.h33.picture.api.aliyunai.AliYunAiApi;
import com.h33.picture.api.aliyunai.model.CreateOutPaintingTaskResponse;
import com.h33.picture.api.aliyunai.model.GetOutPaintingTaskResponse;
import com.h33.picture.api.imagesearch.ImageSearchApiFacade;
import com.h33.picture.api.imagesearch.ImageSearchResult;
import com.h33.picture.common.BaseResponse;
import com.h33.picture.common.DeleteRequest;
import com.h33.picture.common.ResultUtils;
import com.h33.picture.constant.UserConstant;
import com.h33.picture.exception.BusinessException;
import com.h33.picture.exception.ErrorCode;
import com.h33.picture.exception.ThrowUtils;
import com.h33.picture.manager.auth.SpaceUserAuthManager;
import com.h33.picture.manager.auth.StpKit;
import com.h33.picture.manager.auth.annotation.SaSpaceCheckPermission;
import com.h33.picture.manager.auth.model.SpaceUserPermissionConstant;
import com.h33.picture.model.dto.picture.*;
import com.h33.picture.model.entity.Picture;
import com.h33.picture.model.entity.Space;
import com.h33.picture.model.entity.User;
import com.h33.picture.model.enums.PictureReviewStatusEnum;
import com.h33.picture.model.vo.PictureTagCategory;
import com.h33.picture.model.vo.PictureV0;
import com.h33.picture.service.PictureService;
import com.h33.picture.service.SpaceService;
import com.h33.picture.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.data.redis.core.StringRedisTemplate;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@RestController
@RequestMapping("/picture")
public class PictureController {
    @Resource
    private UserService userService;
    @Resource
    private PictureService pictureService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private SpaceService spaceService;

    /*
    * 上传图片 可重新上传
    * */
    @PostMapping("/upload")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_UPLOAD)
//    @AuthCheck(mustRole =UserConstant.ADMIN_ROLE)
    public BaseResponse<PictureV0> uploadPicture(@RequestPart("file") MultipartFile multipartFile, PictureUploadRequest pictureUploadRequest,
                                                 HttpServletRequest httpServletRequest){
        User loginUser=userService.getLoginUser(httpServletRequest);

        // 空间权限校验
        Long spaceId = pictureUploadRequest.getSpaceId();
        if (spaceId != null) {
            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
            // 必须空间创建人（管理员）才能上传
            if (!loginUser.getId().equals(space.getUserId())) {
                throw new BusinessException(ErrorCode.NOT_AUTH_ERROE, "没有空间权限");
            }
            // 校验额度
            if (space.getTotalCount() >= space.getMaxCount()) {
                throw new BusinessException(ErrorCode.OPERRATION_ERROR, "空间条数不足");
            }
            if (space.getTotalSize() >= space.getMaxSize()) {
                throw new BusinessException(ErrorCode.OPERRATION_ERROR, "空间大小不足");
            }
        }

        PictureV0 pictureV0=pictureService.uploadPicture(multipartFile,pictureUploadRequest,loginUser);
        return ResultUtils.sucess(pictureV0);
    }


    /**
     * 删除图片
     */
    @PostMapping("/delete")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_DELETE)
    public BaseResponse<Boolean> deletePicture(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        long id = deleteRequest.getId();
        // 判断是否存在
        Picture oldPicture = pictureService.getById(id);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可删除
        if (!oldPicture.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NOT_AUTH_ERROE);
        }
        // 操作数据库
        boolean result = pictureService.removeById(id);
        ThrowUtils.throwIf(!result, ErrorCode.OPERRATION_ERROR);
        return ResultUtils.sucess(true);
    }

    /**
     * 更新图片（仅管理员可用）
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updatePicture(@RequestBody PictureUpdateRequest pictureUpdateRequest,HttpServletRequest request) {
        if (pictureUpdateRequest == null || pictureUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 将实体类和 DTO 进行转换
        Picture picture = new Picture();
        BeanUtils.copyProperties(pictureUpdateRequest, picture);
        // 注意将 list 转为 string
        picture.setTags(JSONUtil.toJsonStr(pictureUpdateRequest.getTags()));
        // 数据校验
        pictureService.validPicture(picture);
        // 判断是否存在
        long id = pictureUpdateRequest.getId();
        Picture oldPicture = pictureService.getById(id);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);



        // 补充审核参数
        User loginUser = userService.getLoginUser(request);
        pictureService.fillReviewParams(picture, loginUser);



        // 操作数据库
        boolean result = pictureService.updateById(picture);
        ThrowUtils.throwIf(!result, ErrorCode.OPERRATION_ERROR);
        return ResultUtils.sucess(true);
    }

    /**
     * 根据 id 获取图片（仅管理员可用）
     */
    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Picture> getPictureById(long id, HttpServletRequest request) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        Picture picture = pictureService.getById(id);
        ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR);
        // 获取封装类
        return ResultUtils.sucess(picture);
    }

    /**
     * 根据 id 获取图片（封装类）
     */
//    @GetMapping("/get/vo")
//    public BaseResponse<PictureV0> getPictureVOById(long id, HttpServletRequest request) {
//        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
//
//        // 查询数据库
//        Picture picture = pictureService.getById(id);
//        ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR);
//
//        // 空间权限校验
//        Long spaceId = picture.getSpaceId();
//        if (spaceId != null) {
//            User loginUser = userService.getLoginUser(request);
//            pictureService.checkPictureAuth(loginUser, picture);
//        }
//
//
//        // 获取封装类
//        return ResultUtils.sucess(pictureService.getPictureV0(picture, request));
//    }

    @Resource
    private SpaceUserAuthManager spaceUserAuthManager;

    @GetMapping("/get/vo")
    public BaseResponse<PictureV0> getPictureVOById(long id, HttpServletRequest request) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        Picture picture = pictureService.getById(id);
        ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR);
        // 空间的图片，需要校验权限
        Space space = null;
        Long spaceId = picture.getSpaceId();
        if (spaceId != null) {
            boolean hasPermission = StpKit.SPACE.hasPermission(SpaceUserPermissionConstant.PICTURE_VIEW);
            ThrowUtils.throwIf(!hasPermission, ErrorCode.NOT_AUTH_ERROE);
            space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
        }
        // 获取权限列表
        User loginUser = userService.getLoginUser(request);
        List<String> permissionList = spaceUserAuthManager.getPermissionList(space, loginUser);
        PictureV0 pictureVO = pictureService.getPictureV0(picture, request);
        pictureVO.setPermissionList(permissionList);
        // 获取封装类
        return ResultUtils.sucess(pictureVO);
    }


    @PostMapping("/list/page/vo")
    public BaseResponse<Page<PictureV0>> listPictureVOByPage(@RequestBody PictureQueryRequest pictureQueryRequest, HttpServletRequest request) {
        long current = pictureQueryRequest.getCurrent();
        long size = pictureQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        // 空间权限校验
        Long spaceId = pictureQueryRequest.getSpaceId();
        // 公开图库
        if (spaceId == null) {
            // 普通用户默认只能查看已过审的公开数据
            pictureQueryRequest.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
            pictureQueryRequest.setNullSpaceId(true);
        } else {
            boolean hasPermission = StpKit.SPACE.hasPermission(SpaceUserPermissionConstant.PICTURE_VIEW);
            ThrowUtils.throwIf(!hasPermission, ErrorCode.NOT_AUTH_ERROE);
        }
        // 查询数据库
        Page<Picture> picturePage = pictureService.page(new Page<>(current, size), pictureService.getQueryWrapper(pictureQueryRequest));
        // 获取封装类
        return ResultUtils.sucess(pictureService.getPictureVOPage(picturePage, request));
    }


    /**
     * 分页获取图片列表（仅管理员可用）
     */
    @PostMapping("/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<Picture>> listPictureByPage(@RequestBody PictureQueryRequest pictureQueryRequest) {
        long current = pictureQueryRequest.getCurrent();
        long size = pictureQueryRequest.getPageSize();

        // 普通用户默认只能查看已过审的数据
        pictureQueryRequest.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());



        // 查询数据库
        Page<Picture> picturePage = pictureService.page(new Page<>(current, size),
                pictureService.getQueryWrapper(pictureQueryRequest));
        return ResultUtils.sucess(picturePage);
    }

    /**
     * 分页获取图片列表（封装类）
     */
//    @PostMapping("/list/page/vo")
//    public BaseResponse<Page<PictureV0>> listPictureVOByPage(@RequestBody PictureQueryRequest pictureQueryRequest,
//                                                             HttpServletRequest request) {
//
//        // 空间权限校验
//        Long spaceId = pictureQueryRequest.getSpaceId();
//        // 公开图库
//        if (spaceId == null) {
//            // 普通用户默认只能查看已过审的公开数据
//            pictureQueryRequest.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
//            pictureQueryRequest.setNullSpaceId(true);
//        } else {
//            // 私有空间
//            User loginUser = userService.getLoginUser(request);
//            Space space = spaceService.getById(spaceId);
//            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
//            if (!loginUser.getId().equals(space.getUserId())) {
//                throw new BusinessException(ErrorCode.NOT_AUTH_ERROE, "没有空间权限");
//            }
//        }
//
//
//        long current = pictureQueryRequest.getCurrent();
//        long size = pictureQueryRequest.getPageSize();
//        // 限制爬虫
//        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
//        // 查询数据库
//        Page<Picture> picturePage = pictureService.page(new Page<>(current, size),
//                pictureService.getQueryWrapper(pictureQueryRequest));
//        // 获取封装类
//        return ResultUtils.sucess(pictureService.getPictureVOPage(picturePage, request));
//    }

    /**
     * 编辑图片（给用户使用）
     */
    @PostMapping("/edit")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_EDIT)
    public BaseResponse<Boolean> editPicture(@RequestBody PictureEditRequest pictureEditRequest, HttpServletRequest request) {
        if (pictureEditRequest == null || pictureEditRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 在此处将实体类和 DTO 进行转换
        Picture picture = new Picture();
        BeanUtils.copyProperties(pictureEditRequest, picture);
        // 注意将 list 转为 string
        picture.setTags(JSONUtil.toJsonStr(pictureEditRequest.getTags()));
        // 设置编辑时间
        picture.setEditTime(new Date());
        // 数据校验
        pictureService.validPicture(picture);
        User loginUser = userService.getLoginUser(request);
        // 判断是否存在
        long id = pictureEditRequest.getId();
        Picture oldPicture = pictureService.getById(id);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可编辑
        if (!oldPicture.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NOT_AUTH_ERROE);
        }

        // 补充审核参数
        pictureService.fillReviewParams(picture, loginUser);

        // 操作数据库
        boolean result = pictureService.updateById(picture);
        ThrowUtils.throwIf(!result, ErrorCode.OPERRATION_ERROR);
        return ResultUtils.sucess(true);
    }



    @GetMapping("/tag_category")
    public BaseResponse<PictureTagCategory> listPictureTagCategory() {
        PictureTagCategory pictureTagCategory = new PictureTagCategory();
        List<String> tagList = Arrays.asList("热门", "搞笑", "生活", "高清", "艺术", "校园", "背景", "简历", "创意");
        List<String> categoryList = Arrays.asList("模板", "电商", "表情包", "素材", "海报");
        pictureTagCategory.setTagList(tagList);
        pictureTagCategory.setCategoryList(categoryList);
        return ResultUtils.sucess(pictureTagCategory);
    }


    @PostMapping("/review")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> doPictureReview(@RequestBody PictureReviewRequest pictureReviewRequest,
                                                 HttpServletRequest request) {
        ThrowUtils.throwIf(pictureReviewRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        pictureService.doPictureReview(pictureReviewRequest, loginUser);
        return ResultUtils.sucess(true);
    }


    /**
     * 通过 URL 上传图片（可重新上传）
     */
    @PostMapping("/upload/url")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_UPLOAD)
    public BaseResponse<PictureV0> uploadPictureByUrl(
            @RequestBody PictureUploadRequest pictureUploadRequest,
            HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        String fileUrl = pictureUploadRequest.getFileUrl();
        PictureV0 pictureVO = pictureService.uploadPicture(fileUrl, pictureUploadRequest, loginUser);
        return ResultUtils.sucess(pictureVO);
    }

    @PostMapping("/upload/batch")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Integer> uploadPictureByBatch(
            @RequestBody PictureUploadByBatchRequest pictureUploadByBatchRequest,
            HttpServletRequest request
    ) {
        ThrowUtils.throwIf(pictureUploadByBatchRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        int uploadCount = pictureService.uploadPictureByBatch(pictureUploadByBatchRequest, loginUser);
        return ResultUtils.sucess(uploadCount);
    }

//    @PostMapping("/list/page/vo/cache")
//    public BaseResponse<Page<PictureV0>> listPictureVOByPageWithCache(@RequestBody PictureQueryRequest pictureQueryRequest,
//                                                                      HttpServletRequest request) {
//        long current = pictureQueryRequest.getCurrent();
//        long size = pictureQueryRequest.getPageSize();
//        // 限制爬虫
//        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
//        // 普通用户默认只能查看已过审的数据
//        pictureQueryRequest.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
//
//        // 构建缓存 key
//        String queryCondition = JSONUtil.toJsonStr(pictureQueryRequest);
//        String hashKey = DigestUtils.md5DigestAsHex(queryCondition.getBytes());
//        String redisKey = "picture:listPictureVOByPage:" + hashKey;
//        // 从 Redis 缓存中查询
//        ValueOperations<String, String> valueOps = stringRedisTemplate.opsForValue();
//        String cachedValue = valueOps.get(redisKey);
//        if (cachedValue != null) {
//            // 如果缓存命中，返回结果
//            Page<PictureV0> cachedPage = JSONUtil.toBean(cachedValue, Page.class);
//            return ResultUtils.sucess(cachedPage);
//        }
//
//        // 查询数据库
//        Page<Picture> picturePage = pictureService.page(new Page<>(current, size),
//                pictureService.getQueryWrapper(pictureQueryRequest));
//        // 获取封装类
//        Page<PictureV0> pictureVOPage = pictureService.getPictureVOPage(picturePage, request);
//
//        // 存入 Redis 缓存
//        String cacheValue = JSONUtil.toJsonStr(pictureVOPage);
//        // 5 - 10 分钟随机过期，防止雪崩
//        int cacheExpireTime = 300 +  RandomUtil.randomInt(0, 300);
//        valueOps.set(redisKey, cacheValue, cacheExpireTime, TimeUnit.SECONDS);
//
//        // 返回结果
//        return ResultUtils.sucess(pictureVOPage);
//    }

    private final Cache<String, String> LOCAL_CACHE =
            Caffeine.newBuilder().initialCapacity(1024)
                    .maximumSize(10000L)
                    // 缓存 5 分钟移除
                    .expireAfterWrite(5L, TimeUnit.MINUTES)
                    .build();


//    @PostMapping("/list/page/vo/cache")
//    public BaseResponse<Page<PictureV0>> listPictureVOByPageWithCache(@RequestBody PictureQueryRequest pictureQueryRequest,
//                                                                      HttpServletRequest request) {
//
//        long current = pictureQueryRequest.getCurrent();
//        long size = pictureQueryRequest.getPageSize();
//        // 限制爬虫
//        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
//        // 普通用户默认只能查看已过审的数据
//        pictureQueryRequest.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
//        // 构建缓存 key
//        String queryCondition = JSONUtil.toJsonStr(pictureQueryRequest);
//        String hashKey = DigestUtils.md5DigestAsHex(queryCondition.getBytes());
//        String cacheKey = "listPictureVOByPage:" + hashKey;
//        // 从本地缓存中查询
//        String cachedValue = LOCAL_CACHE.getIfPresent(cacheKey);
//        if (cachedValue != null) {
//            // 如果缓存命中，返回结果
//            Page<PictureV0> cachedPage = JSONUtil.toBean(cachedValue, Page.class);
//            return ResultUtils.sucess(cachedPage);
//        }
//        // 查询数据库
//        Page<Picture> picturePage = pictureService.page(new Page<>(current, size),
//                pictureService.getQueryWrapper(pictureQueryRequest));
//// 获取封装类
//        Page<PictureV0> pictureVOPage = pictureService.getPictureVOPage(picturePage, request);
//
//// 存入本地缓存
//        String cacheValue = JSONUtil.toJsonStr(pictureVOPage);
//        LOCAL_CACHE.put(cacheKey, cacheValue);
//
//        // 返回结果
//        return ResultUtils.sucess(pictureVOPage);
//
//    }

//    @PostMapping("/list/page/vo/cache")
//    public BaseResponse<Page<PictureV0>> listPictureVOByPageWithCache(@RequestBody PictureQueryRequest pictureQueryRequest,
//                                                                      HttpServletRequest request) {
//
//        long current = pictureQueryRequest.getCurrent();
//        long size = pictureQueryRequest.getPageSize();
//        // 限制爬虫
//        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
//        // 普通用户默认只能查看已过审的数据
//        pictureQueryRequest.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
//
//
//        // 构建缓存 key
//        String queryCondition = JSONUtil.toJsonStr(pictureQueryRequest);
//        String hashKey = DigestUtils.md5DigestAsHex(queryCondition.getBytes());
//        String cacheKey = "picture:listPictureVOByPage:" + hashKey;
//
//    // 1. 查询本地缓存（Caffeine）
//            String cachedValue = LOCAL_CACHE.getIfPresent(cacheKey);
//            if (cachedValue != null) {
//                Page<PictureV0> cachedPage = JSONUtil.toBean(cachedValue, Page.class);
//                return ResultUtils.sucess(cachedPage);
//        }
//
//
//        // 2. 查询分布式缓存（Redis）
//        ValueOperations<String, String> valueOps = stringRedisTemplate.opsForValue();
//        cachedValue = valueOps.get(cacheKey);
//        if (cachedValue != null) {
//            // 如果命中 Redis，存入本地缓存并返回
//            LOCAL_CACHE.put(cacheKey, cachedValue);
//            Page<PictureV0> cachedPage = JSONUtil.toBean(cachedValue, Page.class);
//            return ResultUtils.sucess(cachedPage);
//        }
//
//        // 3. 查询数据库
//        Page<Picture> picturePage = pictureService.page(new Page<>(current, size),
//                pictureService.getQueryWrapper(pictureQueryRequest));
//        Page<PictureV0> pictureVOPage = pictureService.getPictureVOPage(picturePage, request);
//
//        // 4. 更新缓存
//                String cacheValue = JSONUtil.toJsonStr(pictureVOPage);
//        // 更新本地缓存
//                LOCAL_CACHE.put(cacheKey, cacheValue);
//        // 更新 Redis 缓存，设置过期时间为 5 分钟
//                valueOps.set(cacheKey, cacheValue, 5, TimeUnit.MINUTES);
//
//
//        // 返回结果
//        return ResultUtils.sucess(pictureVOPage);
//
//    }


    /**
     * 以图搜图
     */
    @PostMapping("/search/picture")
    public BaseResponse<List<ImageSearchResult>> searchPictureByPicture(@RequestBody SearchPictureByPictureRequest searchPictureByPictureRequest) {
        ThrowUtils.throwIf(searchPictureByPictureRequest == null, ErrorCode.PARAMS_ERROR);
        Long pictureId = searchPictureByPictureRequest.getPictureId();
        ThrowUtils.throwIf(pictureId == null || pictureId <= 0, ErrorCode.PARAMS_ERROR);
        Picture oldPicture = pictureService.getById(pictureId);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        List<ImageSearchResult> resultList = ImageSearchApiFacade.searchImage(oldPicture.getUrl());
        return ResultUtils.sucess(resultList);
    }

    @PostMapping("/search/color")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_VIEW)
    public BaseResponse<List<PictureV0>> searchPictureByColor(@RequestBody SearchPictureByColorRequest searchPictureByColorRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(searchPictureByColorRequest == null, ErrorCode.PARAMS_ERROR);
        String picColor = searchPictureByColorRequest.getPicColor();
        Long spaceId = searchPictureByColorRequest.getSpaceId();
        User loginUser = userService.getLoginUser(request);
        List<PictureV0> result = pictureService.searchPictureByColor(spaceId, picColor, loginUser);
        return ResultUtils.sucess(result);
    }

    @PostMapping("/edit/batch")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_EDIT)
    public BaseResponse<Boolean> editPictureByBatch(@RequestBody PictureEditByBatchRequest pictureEditByBatchRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(pictureEditByBatchRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        pictureService.editPictureByBatch(pictureEditByBatchRequest, loginUser);
        return ResultUtils.sucess(true);
    }

    /**
     * 创建 AI 扩图任务
     */
    @Resource
    private AliYunAiApi aliYunAiApi;

    @PostMapping("/out_painting/create_task")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_EDIT)
    public BaseResponse<CreateOutPaintingTaskResponse> createPictureOutPaintingTask(
            @RequestBody CreatePictureOutPaintingTaskRequest createPictureOutPaintingTaskRequest,
            HttpServletRequest request) {
        if (createPictureOutPaintingTaskRequest == null || createPictureOutPaintingTaskRequest.getPictureId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        CreateOutPaintingTaskResponse response = pictureService.createPictureOutPaintingTask(createPictureOutPaintingTaskRequest, loginUser);
        return ResultUtils.sucess(response);
    }

    /**
     * 查询 AI 扩图任务
     */
    @GetMapping("/out_painting/get_task")
    public BaseResponse<GetOutPaintingTaskResponse> getPictureOutPaintingTask(String taskId) {
        ThrowUtils.throwIf(StrUtil.isBlank(taskId), ErrorCode.PARAMS_ERROR);
        GetOutPaintingTaskResponse task = aliYunAiApi.getOutPaintingTask(taskId);
        return ResultUtils.sucess(task);
    }










}
