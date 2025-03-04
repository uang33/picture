package com.h33.picture.model.dto.picture;

import lombok.Data;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.util.List;

@Data
//管理员使用
public class PictureUpdateRequest implements Serializable {
    private Long id;
    private String name;
    private String introduction;
    private String category;
    private List<String> tags;

    private static final long serialVersionUID=1L;


}
