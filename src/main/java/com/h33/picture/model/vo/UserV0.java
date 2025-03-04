package com.h33.picture.model.vo;

import lombok.Data;

import java.io.Serializable;

@Data
public class UserV0 implements Serializable {
    private Long id;

    private String userName;
    private String userAccount;
    private String userAvatar;
    private String userProfile;
    private String userRole;
    private static final long serialVersionUID=1L;
}
