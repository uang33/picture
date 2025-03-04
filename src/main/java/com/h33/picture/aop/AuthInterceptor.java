package com.h33.picture.aop;

import com.h33.picture.annotation.AuthCheck;
import com.h33.picture.exception.BusinessException;
import com.h33.picture.exception.ErrorCode;
import com.h33.picture.model.entity.User;
import com.h33.picture.model.enums.UserRoleEnum;
import com.h33.picture.service.UserService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

@Aspect
@Component
public class AuthInterceptor {
    @Resource
    private UserService userService;
    /*
    * 执行拦截
    * @param joinPoint 切入点
    *
    * @param authCheck 权限校验注解
    *
    * */

    @Around("@annotation(authCheck)")
    public Object doInterceptor(ProceedingJoinPoint joinPoint, AuthCheck authCheck) throws Throwable{
        String mustRole= authCheck.mustRole();
        RequestAttributes requestAttributes= RequestContextHolder.currentRequestAttributes();
        HttpServletRequest request=((ServletRequestAttributes) requestAttributes).getRequest();

        //当前登录用户
        User loginUser=userService.getLoginUser(request);
        UserRoleEnum mustRoleEnum=UserRoleEnum.getEnumByValue(mustRole);

        //不需要权限放行
        if(mustRoleEnum==null) return joinPoint.proceed();

        //必须有该权限才通过
        //获取当前用户具有的权限
        UserRoleEnum userRoleEnum=UserRoleEnum.getEnumByValue(loginUser.getUserRole());

        if(userRoleEnum==null) return new BusinessException(ErrorCode.NOT_AUTH_ERROE);

        if(UserRoleEnum.ADMIN.equals(mustRoleEnum)&&!UserRoleEnum.ADMIN.equals(userRoleEnum)){
            throw new BusinessException(ErrorCode.NOT_AUTH_ERROE);
        }
        return joinPoint.proceed();
    }


}
