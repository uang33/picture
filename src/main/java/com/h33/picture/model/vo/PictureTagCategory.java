package com.h33.picture.model.vo;

import lombok.Data;

import java.util.List;

@Data
public class PictureTagCategory {
    private List<String> tagList;
    private List<String> categoryList;

}
