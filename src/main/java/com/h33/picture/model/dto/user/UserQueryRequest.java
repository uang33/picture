package com.h33.picture.model.dto.user;

import com.h33.picture.common.PageRequest;
import com.h33.picture.model.vo.LoginUserV0;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

@EqualsAndHashCode(callSuper = true)
@Data
public class UserQueryRequest extends PageRequest implements Serializable {

    private Long id;
    private String userRole;

    private String userName;
    private String userAccount;
//    private String userAvatar;
    private String userProfile;
    private static final long serialVersionUID=1L;
}
