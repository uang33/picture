package com.h33.picture.model.dto.picture;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
//用户
public class PictureEditRequest implements Serializable {
    private Long id;
    private String name;
    private String introduction;
    private String category;
    private List<String> tags;

    private static final long serialVersionUID=1L;
}
