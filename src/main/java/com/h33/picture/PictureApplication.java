package com.h33.picture;

import com.h33.picture.annotation.AuthCheck;
import com.h33.picture.constant.UserConstant;
import com.h33.picture.model.enums.UserRoleEnum;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
@MapperScan("com.h33.picture.mapper")
@EnableAspectJAutoProxy(exposeProxy = true)

public class PictureApplication {

    public static void main(String[] args) {
        SpringApplication.run(PictureApplication.class, args);
    }

}
