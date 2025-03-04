package com.h33.picture.model.dto.picture;

import lombok.Data;

import java.io.Serializable;


@Data
public class PictureUploadRequest implements Serializable {
    /*
    * 图片id 用于修改
    *
    * */
    private Long id;
    private static final long serialVersionUID=1L;

    private String fileUrl;

    /**
     * 图片名称
     */
    private String picName;
    /**
     * 空间 id
     */
    private Long spaceId;


}
