package com.h33.picture.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;


@Configuration
public class CorsConfig implements WebMvcConfigurer {

    //覆盖所有请求

    @Override
    public void addCorsMappings(CorsRegistry registry){
        registry.addMapping("/**")
//                允许发送Cookie
                .allowCredentials(true)
//                放行哪些域名，必须用patterns,否则*会和allowCredentials冲突
                .allowedOriginPatterns("*")
                .allowedMethods("Get","POST","PUT","DELETE","OPTIONS")
                .allowedHeaders("*")
                .allowedMethods("*");


    }

}
