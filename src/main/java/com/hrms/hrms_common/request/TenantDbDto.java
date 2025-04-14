package com.hrms.hrms_common.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenantDbDto {
    private String tenantId;
    private String dbUrl;
    private String username;
    private String password;
}
