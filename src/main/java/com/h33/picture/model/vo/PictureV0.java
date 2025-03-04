package com.h33.picture.model.vo;

import cn.hutool.core.lang.copier.SrcToDestCopier;
import cn.hutool.json.JSONUtil;
import com.h33.picture.model.entity.Picture;
import io.swagger.models.auth.In;
import lombok.Data;
import org.springframework.beans.BeanUtils;
import springfox.documentation.spring.web.scanners.ApiDescriptionReader;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
public class PictureV0 implements Serializable {
    /*
    * id
    * */
    private Long id;
    private String url;
    private String name;
    private String introduction;
    private List<String> tags;
    private String category;
    private Long picSize;
    private Integer picWidth;
    private Integer picHeight;
    private Integer picScale;
    private String picFormat;
    private Long userId;
    private Date editTime;
    private Date updateTime;
    private UserV0 user;
    private static final long serialVersionUID=1L;

    /**
     * 缩略图 url
     */
    private String thumbnailUrl;

    /**
     * 空间 id
     */
    private Long spaceId;


    /**
     * 图片主色调
     */
    private String picColor;

    /**
     * 权限列表
     */
    private List<String> permissionList = new ArrayList<>();





    //封装类对象
    public static Picture voToObj(PictureV0 pictureV0){
        if(pictureV0==null) return null;
        Picture picture=new Picture();
        BeanUtils.copyProperties(pictureV0,picture);

        //List<String> -> String
        picture.setTags(JSONUtil.toJsonStr(pictureV0.getTags()));
        return picture;
    }

    //对象转封装类
    public static PictureV0 objToVo(Picture picture){
        if(picture==null) return null;
        PictureV0 pictureV0=new PictureV0();
        BeanUtils.copyProperties(picture,pictureV0);

        pictureV0.setTags(JSONUtil.toList(picture.getTags(),String.class));
        return pictureV0;
    }





}
