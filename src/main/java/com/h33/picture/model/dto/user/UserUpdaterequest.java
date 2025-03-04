package com.h33.picture.model.dto.user;

import lombok.Data;

import java.io.Serializable;
@Data
public class UserUpdaterequest implements Serializable {
    private  long id;
    private String userName;
    private String userAccount;
    private String userAvatar;
    private String userProfile;
    private String userRole;
    private static final long serialVersionUID=1L;
}