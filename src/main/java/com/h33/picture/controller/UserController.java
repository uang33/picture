package com.h33.picture.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.h33.picture.annotation.AuthCheck;
import com.h33.picture.common.BaseResponse;
import com.h33.picture.common.DeleteRequest;
import com.h33.picture.common.ResultUtils;
import com.h33.picture.constant.UserConstant;
import com.h33.picture.exception.BusinessException;
import com.h33.picture.exception.ErrorCode;
import com.h33.picture.exception.ThrowUtils;
import com.h33.picture.model.dto.user.*;
import com.h33.picture.model.entity.User;
import com.h33.picture.model.vo.LoginUserV0;
import com.h33.picture.model.vo.UserV0;
import com.h33.picture.service.UserService;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    private UserService userService;
    /*
    * 用户注册
    * */

    @PostMapping("/resgister")
    public BaseResponse<Long> userRegister(@RequestBody UserRegisterRequest userRegisterRequest){
        ThrowUtils.throwIf(userRegisterRequest==null, ErrorCode.PARAMS_ERROR);
        String userAccount=userRegisterRequest.getUserAccount();
        String userPassword= userRegisterRequest.getUserPassword();
        String checkPassord= userRegisterRequest.getCheckPassword();
        long result=userService.userRegister(userAccount,userPassword,checkPassord);
        return ResultUtils.sucess(result);

    }

    /*
     * 用户登录. 获取当前登录用户
     * */

    @PostMapping("/login")
    public BaseResponse<LoginUserV0> userLogin(@RequestBody UserLoginRequest userLoginRequest, HttpServletRequest request){
        ThrowUtils.throwIf(userLoginRequest==null, ErrorCode.PARAMS_ERROR);
        String userAccount= userLoginRequest.getUserAccount();
        String userPassword=userLoginRequest.getUserPassword();
        LoginUserV0 loginUserV0=userService.userLogin(userAccount,userPassword,request);
        return ResultUtils.sucess(loginUserV0);
    }


    //登录
    @GetMapping("/get/login")
    public BaseResponse<LoginUserV0> getLoginUser(HttpServletRequest request){
        User loginUser=userService.getLoginUser(request);
        return ResultUtils.sucess(userService.getLoginUserV0(loginUser));
    }

    //退出
    @PostMapping("/logout")
    public BaseResponse<Boolean> userLogout(HttpServletRequest request){
        ThrowUtils.throwIf(request==null,ErrorCode.PARAMS_ERROR);
        boolean result=userService.userLogout(request);
        return ResultUtils.sucess(result);
    }

    //添加
    @PostMapping("/add")
    @AuthCheck(mustRole =UserConstant.ADMIN_ROLE)
    public BaseResponse<Long> addUser(@RequestBody UserAddRequest userAddRequest){
        ThrowUtils.throwIf(userAddRequest==null,ErrorCode.PARAMS_ERROR);
        User user=new User();
        BeanUtils.copyProperties(userAddRequest,user);

        //默认密码
        final String DEFAULT_PASSWORD="00000000";
        String encryptPassword=userService.getEncryptPassword(DEFAULT_PASSWORD);
        user.setUserPassword(encryptPassword);
        boolean result=userService.save(user);
        ThrowUtils.throwIf(!result,ErrorCode.OPERRATION_ERROR);
        return ResultUtils.sucess(user.getId());
    }

    //根据id获取用户 仅管理员
    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<User> getUserById(long id){
        ThrowUtils.throwIf(id<=0,ErrorCode.PARAMS_ERROR);
        User user=userService.getById(id);
        ThrowUtils.throwIf(user==null,ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.sucess(user);

    }

    //根据id获取包装类
    @GetMapping("/get/vo")
    public BaseResponse<UserV0> getUserV0ById(long id){
        BaseResponse<User> response=getUserById(id);
        User user=response.getData();
        return ResultUtils.sucess(userService.getUserV0(user));
    }

    //删除
    @PostMapping("/delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteUser(@RequestBody DeleteRequest deleteRequest){
        if(deleteRequest==null||deleteRequest.getId()<0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean b=userService.removeById(deleteRequest.getId());
        return ResultUtils.sucess(b);
    }
    //更新
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateUser(@RequestBody UserUpdaterequest userUpdaterequest){
        if(userUpdaterequest==null||userUpdaterequest.getId()<0) throw new BusinessException(ErrorCode.PARAMS_ERROR);

        User user=new User();
        BeanUtils.copyProperties(userUpdaterequest,user);
        boolean result=userService.updateById(user);
        ThrowUtils.throwIf(!result,ErrorCode.OPERRATION_ERROR);
        return ResultUtils.sucess(true);
    }

    //分页获取用户封装列表 仅管理员
    //@param userQueryRequest 查询请求参数
    @PostMapping("/list/page/vo")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<UserV0>> listUserV0ByPage(@RequestBody UserQueryRequest userQueryRequest){
        ThrowUtils.throwIf(userQueryRequest==null,ErrorCode.PARAMS_ERROR);

        long current=userQueryRequest.getCurrent();
        long pageSize=userQueryRequest.getPageSize();
        Page<User> userPage=userService.page(new Page<>(current,pageSize),
                userService.getQueryWrapper(userQueryRequest));

        Page<UserV0> userV0Page=new Page<>(current,pageSize,userPage.getTotal());
        List<UserV0> userV0List=userService.getUserV0List(userPage.getRecords());

        userV0Page.setRecords(userV0List);
        return ResultUtils.sucess(userV0Page);
    }


}
