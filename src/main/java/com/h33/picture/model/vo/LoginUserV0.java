package com.h33.picture.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class LoginUserV0 implements Serializable {
    /*
    * 用户id
    * */
    private Long id;


    /*
     * 用户账户
     * */
    private  String userAccount;

    /*
     * 用户昵称
     * */

    private String userName;



    /*
     * 用户头像
     * */
    private String userAvatar;


    /*
     * 用户简介
     * */
    private String userPofile;


    /*
     * 用户角色： user/admin
     * */
    private String userRole;


    /*
     * 创建时间
     * */
    private Date createTime;

    /*
     * 更新时间
     * */
    private Date updateTime;

    private static final long serialVersionUID=1L;



}
