package com.hrms.hrms_common.multitenancy.config.tenant.hibernate;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.github.benmanes.caffeine.cache.RemovalListener;

import com.hrms.hrms_common.multitenancy.domain.entity.Tenant;
import com.hrms.hrms_common.request.TenantDbDto;
import com.hrms.hrms_common.request.TenantDbResponseDto;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.engine.jdbc.connections.spi.AbstractDataSourceBasedMultiTenantConnectionProviderImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


@RequiredArgsConstructor
@Slf4j
@Component
public class DynamicDataSourceBasedMultiTenantConnectionProvider
        extends AbstractDataSourceBasedMultiTenantConnectionProviderImpl<String> {

    private static final long serialVersionUID = -460277105706399638L;

    private static final String TENANT_POOL_NAME_SUFFIX = "DataSource";


    @Qualifier("masterDataSource")
    private final DataSource masterDataSource;

    @Qualifier("masterDataSourceProperties")
    private final DataSourceProperties masterDataSourceProperties;

    //private final TenantRepository masterTenantRepository;
    @Value("${multitenancy.tenant.datasource.url-prefix}")
    private String urlPrefix;

    @Value("${multitenancy.datasource-cache.maximumSize:100}")
    private Long maximumSize;

    @Value("${multitenancy.datasource-cache.expireAfterAccess:10}")
    private Integer expireAfterAccess;

    @Value("${multitenancy.master.configuration.endpoint}")
    private String masterDbConfigEndpoint;

    private LoadingCache<String, DataSource> tenantDataSources;

    @Autowired
    private RestTemplate restTemplate;

    @PostConstruct
    private void createCache() {
            ResponseEntity<TenantDbResponseDto> response = restTemplate.exchange(
                    this.masterDbConfigEndpoint,
                    org.springframework.http.HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<>() {}
            );


        HashMap<String, Tenant> tenantDatabaseHashMap = response.getBody().getData().stream()
                .collect(Collectors.toMap(
                        TenantDbDto::getTenantId,
                        this::mapToTenant,
                        (existing, replacement) -> existing,
                        HashMap::new
                ));


        tenantDataSources = Caffeine.newBuilder()
                .maximumSize(maximumSize)
                .expireAfterAccess(expireAfterAccess, TimeUnit.MINUTES)
                .removalListener((RemovalListener<String, DataSource>) (tenantId, dataSource, removalCause) -> {
                    HikariDataSource ds = (HikariDataSource) dataSource;
                    ds.close(); // tear down properly
                    log.info("Closed datasource: {}", ds.getPoolName());
                })
                .build(tenantId -> {
                    Tenant tenant = tenantDatabaseHashMap.get(tenantId);
                    if (tenant == null){
                        throw  new RuntimeException("No such tenant: " + tenantId);
                    }
                    return createAndConfigureDataSource(tenant);
                        }
                );
    }

    @Override
    protected DataSource selectAnyDataSource() {
        return masterDataSource;
    }

    @Override
    protected DataSource selectDataSource(String tenantIdentifier) {
        return tenantDataSources.get(tenantIdentifier);
    }

    private DataSource createAndConfigureDataSource(Tenant tenant) {
        //String decryptedPassword = encryptionService.decrypt(tenant.getPassword(), secret, salt);
        String decryptedPassword = tenant.getPassword();

        HikariDataSource ds = masterDataSourceProperties.initializeDataSourceBuilder().type(HikariDataSource.class).build();

        ds.setUsername(tenant.getUsername());
        ds.setPassword(decryptedPassword);
        ds.setJdbcUrl(urlPrefix + tenant.getDb());

        ds.setPoolName(tenant.getTenantId() + TENANT_POOL_NAME_SUFFIX);

        log.info("Configured datasource: {}", ds.getPoolName());
        return ds;
    }

    private Tenant mapToTenant(TenantDbDto tenantDbDto) {
        return Tenant.builder()
                .tenantId(tenantDbDto.getTenantId())
                .db(extractDbNameFromUrl(tenantDbDto.getDbUrl()))
                .username(tenantDbDto.getUsername())
                .dbUrl(tenantDbDto.getDbUrl())
                .password(tenantDbDto.getPassword())
                .build();
    }
    private String extractDbNameFromUrl(String dbUrl) {
        int lastIndex = dbUrl.lastIndexOf('/');
        return lastIndex != -1 ? dbUrl.substring(lastIndex + 1) : dbUrl;
    }

}