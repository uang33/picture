package com.h33.picture.manager;

import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.file.FileNameUtil;
import cn.hutool.core.lang.copier.SrcToDestCopier;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpStatus;
import cn.hutool.http.HttpUtil;
import cn.hutool.http.Method;
import com.h33.picture.config.CosClientConfig;
import com.h33.picture.exception.BusinessException;
import com.h33.picture.exception.ErrorCode;
import com.h33.picture.exception.ThrowUtils;
import com.h33.picture.model.dto.file.UploadPictureResult;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.ImageInfo;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.swing.*;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * 文件服务
 * @deprecated 已废弃，改为使用 upload 包的模板方法优化
 */
@Deprecated


@Service
@Slf4j
public class FileManager {
    @Resource
    private CosClientConfig cosClientConfig;

    @Resource
    private CosManager cosManager;
    @Autowired
    private StringHttpMessageConverter stringHttpMessageConverter;

    //校验图片
    //@param multipartFile multipart 文件

    public void validPicture(MultipartFile multipartFile){
        ThrowUtils.throwIf(multipartFile==null, ErrorCode.PARAMS_ERROR,"文件为空");
        //校验大小
        long fileSize=multipartFile.getSize();
        final long ONE_M=1024*1024L;
        ThrowUtils.throwIf(fileSize>2*ONE_M,ErrorCode.PARAMS_ERROR,"文件大小不能超过2M");

        //校验后缀
        String fileSuffix=FileNameUtil.getSuffix(multipartFile.getOriginalFilename());

        //允许上传的后缀
        final List<String> ALLOW_FORMAT_LIST= Arrays.asList("jpeg","jpg","png","webp");
        ThrowUtils.throwIf(!ALLOW_FORMAT_LIST.contains(fileSuffix),ErrorCode.PARAMS_ERROR,"文件类型错误");


    }

    //删除临时文件

    public void deleteTempFile(File file){
        if(file==null) return;
        boolean deleteResult=file.delete();
        if(!deleteResult){
            log.error("文件删除失败，地址为",file.getAbsolutePath());
        }

    }

    /*
    * 上传图片
    * @param multipartFile 文件
    * @param uploadPathPrefix 上传路径前缀
    * return
    *
    * */

    public UploadPictureResult uploadPictureResult(MultipartFile multipartFile,String uploadPathprefix){
        //校验图片
        validPicture(multipartFile);

        //图片上传地址
        String uuid= RandomUtil.randomString(16);
        String originFilename=multipartFile.getOriginalFilename();
        String uploadFilename=String.format("%s_%s.%s", DateUtil.formatDate(new Date()),uuid, FileNameUtil.getSuffix(originFilename));
        String uploadPath=String.format("/%s/%s",uploadPathprefix,uploadFilename);
        File file=null;

        try{
            //创建临时文件
            file=File.createTempFile(uploadPath,null);
            multipartFile.transferTo(file);

            //上传图片
            PutObjectResult putObjectResult=cosManager.putPictureObject(uploadPath,file);
            ImageInfo imageInfo=putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();

            //封装返回结果
            UploadPictureResult uploadPictureResult=new UploadPictureResult();
            int picWidth=imageInfo.getWidth();
            int picHeight=imageInfo.getHeight();
            double picScale= NumberUtil.round(picWidth*1.0/picHeight,2).doubleValue();

            uploadPictureResult.setPicName(FileUtil.mainName(originFilename));
            uploadPictureResult.setPicHeight(picHeight);
            uploadPictureResult.setPicWidth(picWidth);
            uploadPictureResult.setPicScale(picScale);
            uploadPictureResult.setPicFormat(imageInfo.getFormat());
            uploadPictureResult.setPicSize(FileUtil.size(file));
            uploadPictureResult.setUrl(cosClientConfig.getHost()+"/"+uploadPath);
            return uploadPictureResult;



        } catch (Exception e) {
            log.error("图片上传对对象存储失败",e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"上传错误");
        }finally {
            this.deleteTempFile(file);
        }

    }

    public UploadPictureResult uploadPictureByUrl(String fileUrl, String uploadPathPrefix) {
        // 校验图片
        // validPicture(multipartFile);
        validPicture(fileUrl);
        // 图片上传地址
        String uuid = RandomUtil.randomString(16);
        // String originFilename = multipartFile.getOriginalFilename();
        String originFilename = FileUtil.mainName(fileUrl);
        String uploadFilename = String.format("%s_%s.%s", DateUtil.formatDate(new Date()), uuid,
                FileUtil.getSuffix(originFilename));
        String uploadPath = String.format("/%s/%s", uploadPathPrefix, uploadFilename);
        File file = null;
        try {
            // 创建临时文件
            file = File.createTempFile(uploadPath, null);
            // multipartFile.transferTo(file);
            HttpUtil.downloadFile(fileUrl, file);


            //上传图片
            PutObjectResult putObjectResult=cosManager.putPictureObject(uploadPath,file);
            ImageInfo imageInfo=putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();

            //封装返回结果
            UploadPictureResult uploadPictureResult=new UploadPictureResult();
            int picWidth=imageInfo.getWidth();
            int picHeight=imageInfo.getHeight();
            double picScale= NumberUtil.round(picWidth*1.0/picHeight,2).doubleValue();

            uploadPictureResult.setPicName(FileUtil.mainName(originFilename));
            uploadPictureResult.setPicHeight(picHeight);
            uploadPictureResult.setPicWidth(picWidth);
            uploadPictureResult.setPicScale(picScale);
            uploadPictureResult.setPicFormat(imageInfo.getFormat());
            uploadPictureResult.setPicSize(FileUtil.size(file));
            uploadPictureResult.setUrl(cosClientConfig.getHost()+"/"+uploadPath);
            return uploadPictureResult;


        } catch (Exception e) {
            log.error("图片上传到对象存储失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传失败");
        } finally {
            this.deleteTempFile(file);
        }
    }

    private void validPicture(String fileUrl) {
        ThrowUtils.throwIf(StrUtil.isBlank(fileUrl), ErrorCode.PARAMS_ERROR, "文件地址不能为空");

        try {
            // 1. 验证 URL 格式
            new URL(fileUrl); // 验证是否是合法的 URL
        } catch (MalformedURLException e) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件地址格式不正确");
        }

        // 2. 校验 URL 协议
        ThrowUtils.throwIf(!(fileUrl.startsWith("http://") || fileUrl.startsWith("https://")),
                ErrorCode.PARAMS_ERROR, "仅支持 HTTP 或 HTTPS 协议的文件地址");

        // 3. 发送 HEAD 请求以验证文件是否存在
        HttpResponse response = null;
        try {
            response = HttpUtil.createRequest(Method.HEAD, fileUrl).execute();
            // 未正常返回，无需执行其他判断
            if (response.getStatus() != HttpStatus.HTTP_OK) {
                return;
            }
            // 4. 校验文件类型
            String contentType = response.header("Content-Type");
            if (StrUtil.isNotBlank(contentType)) {
                // 允许的图片类型
                final List<String> ALLOW_CONTENT_TYPES = Arrays.asList("image/jpeg", "image/jpg", "image/png", "image/webp");
                ThrowUtils.throwIf(!ALLOW_CONTENT_TYPES.contains(contentType.toLowerCase()),
                        ErrorCode.PARAMS_ERROR, "文件类型错误");
            }
            // 5. 校验文件大小
            String contentLengthStr = response.header("Content-Length");
            if (StrUtil.isNotBlank(contentLengthStr)) {
                try {
                    long contentLength = Long.parseLong(contentLengthStr);
                    final long TWO_MB = 2 * 1024 * 1024L; // 限制文件大小为 2MB
                    ThrowUtils.throwIf(contentLength > TWO_MB, ErrorCode.PARAMS_ERROR, "文件大小不能超过 2M");
                } catch (NumberFormatException e) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件大小格式错误");
                }
            }
        } finally {
            if (response != null) {
                response.close();
            }
        }
    }


}
