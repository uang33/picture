package com.h33.picture.common;

import lombok.Data;

import java.io.Serializable;

@Data
public class DeleteRequest implements Serializable {
    private Long id;
    private static final long serialVerdsionID=1L;
}
