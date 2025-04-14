package com.hrms.hrms_common.request;

import lombok.Data;

import java.util.List;

@Data
public class TenantDbResponseDto {
    private int code;
    private String message;
    private List<TenantDbDto> data;
}
