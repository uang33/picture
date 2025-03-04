package com.h33.picture.controller;

import com.h33.picture.common.BaseResponse;
import com.h33.picture.common.ResultUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/")
public class MainController {
    /*
    * 健康检查
    * */

    @GetMapping("/health")
    public BaseResponse<String> health(){
        return ResultUtils.sucess("ok");
    }


}
