package com.hrms.hrms_common.multitenancy.service;

import com.hrms.hrms_common.multitenancy.domain.entity.Tenant;
import org.springframework.data.repository.query.Param;

public interface TenantService {

    Tenant findByTenantId(@Param("tenantId") String tenantId);
}
