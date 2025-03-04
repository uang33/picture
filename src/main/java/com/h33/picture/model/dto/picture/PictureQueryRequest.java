package com.h33.picture.model.dto.picture;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.h33.picture.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data

public class PictureQueryRequest extends PageRequest implements Serializable {
    private Long id;
    private String name;
    private String introduction;
    private String category;
    private List<String> tags;

    private Long picSize;
    private Integer picWidth;
    private Integer picHeight;
    private Double picScale;
    private String picFormat;

    //搜索词
    private String searchText;

    private Long userId;


    private static final long serialVersionUID=1L;

    /**
     * 状态：0-待审核; 1-通过; 2-拒绝
     */
    private Integer reviewStatus;

    /**
     * 审核信息
     */
    private String reviewMessage;

    /**
     * 审核人 id
     */
    private Long reviewerId;

    /**
     * 空间 id
     */
    private Long spaceId;

    /**
     * 是否只查询 spaceId 为 null 的数据
     */
    private boolean nullSpaceId;

    /**
     * 开始编辑时间
     */
    private Date startEditTime;

    /**
     * 结束编辑时间
     */
    private Date endEditTime;



}
