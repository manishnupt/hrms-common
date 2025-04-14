package com.hrms.hrms_common.multitenancy.util;


import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class TenantContext {

    private TenantContext() {}

    private static final InheritableThreadLocal<String> currentTenant =
            new InheritableThreadLocal<>();

    public static void setTenantId(String tenantId) {
        currentTenant.set(tenantId);
    }

    public static String getTenantId() {
        return currentTenant.get();
    }

    public static void clear(){
        currentTenant.remove();
    }
}