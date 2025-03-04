package com.h33.picture.controller;


import com.h33.picture.annotation.AuthCheck;
import com.h33.picture.common.BaseResponse;
import com.h33.picture.common.ResultUtils;
import com.h33.picture.constant.UserConstant;
import com.h33.picture.exception.BusinessException;
import com.h33.picture.exception.ErrorCode;
import com.h33.picture.manager.CosManager;
import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.COSObjectInputStream;
import com.qcloud.cos.utils.IOUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;

@Slf4j
@RestController
@RequestMapping("/file")
public class FileController {

    @Resource
    private CosManager cosManager;

//    public FileController(CosManager cosManager) {
//        this.cosManager = cosManager;
//    }

    /*
    * 测试文件上传
    * @param multipartFile
    * @return
    * */
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @PostMapping("/test/upload")
    public BaseResponse<String> testUploadFile(@RequestPart("file") MultipartFile multipartFile){
        //文件目录
        String filename=multipartFile.getOriginalFilename();
        String filepath=String.format("test/%s",filename);
        File file=null;
        try {
            //上传
            file=File.createTempFile(filepath,null);
            multipartFile.transferTo(file);
            cosManager.putObject(filepath,file);

            //返回可访问地址
            return ResultUtils.sucess(filepath);

        }catch (Exception e){
            log.error("图片加载失败，地址为"+filepath,e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"上传失败!!!");

        }finally {
            if(file!=null){
                //删除临时文件
                boolean delete=file.delete();
                if(!delete){
                    log.error("图片删除失败，文件地址为{}",filepath);
                }
            }
        }
    }

    /*
    * 测试文件下载
    * @param filepath 文件路径
    * @param response 响应对象
    *
    * */
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @GetMapping("test/download/")
    public void testDownloadFile(String filepath, HttpServletResponse response) throws IOException{
        COSObjectInputStream cosObjectInput=null;
        try{
            COSObject cosObject=cosManager.getObject(filepath);
            cosObjectInput=cosObject.getObjectContent();

            //处理下载到的流
            byte[] bytes= IOUtils.toByteArray(cosObjectInput);

            //设置响应头
            response.setContentType("application/octet-stream;charset=UTF-8");
            response.setHeader("Content-Disposition","attachment;filename="+filepath);

            //写入响应
            response.getOutputStream().write(bytes);
            response.getOutputStream().flush();

            } catch (Exception e) {

                log.error("文件下载失败，地址为"+filepath,e);
                throw new BusinessException(ErrorCode.SYSTEM_ERROR,"下载失败");

            }finally {
            if(cosObjectInput!=null){
                cosObjectInput.close();
            }

        }
    }
}
