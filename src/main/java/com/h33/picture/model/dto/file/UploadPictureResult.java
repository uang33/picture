package com.h33.picture.model.dto.file;


import cn.hutool.poi.excel.sax.SheetRidReader;
import lombok.Data;

@Data
public class UploadPictureResult {
    private String url;
    private String picName;
    private Long picSize;
    private int picWidth;
    private int picHeight;
    private Double picScale;
    private String picFormat;

    /**
     * 缩略图 url
     */
    private String thumbnailUrl;

    /**
     * 图片主色调
     */
    private String picColor;





}
