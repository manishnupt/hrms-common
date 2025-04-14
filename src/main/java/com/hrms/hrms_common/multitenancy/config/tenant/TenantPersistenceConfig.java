package com.hrms.hrms_common.multitenancy.config.tenant;

import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.orm.jpa.JpaProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.orm.jpa.hibernate.SpringImplicitNamingStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.hibernate5.SpringBeanContainer;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;

import java.util.HashMap;
import java.util.Map;

@RequiredArgsConstructor
@Configuration
@EnableJpaRepositories(basePackages = {
        "${multitenancy.tenant.repository.packages}" }, entityManagerFactoryRef = "tenantEntityManagerFactory", transactionManagerRef = "tenantTransactionManager")
@EnableConfigurationProperties(JpaProperties.class)
public class TenantPersistenceConfig {

    private final ConfigurableListableBeanFactory beanFactory;
    private final JpaProperties jpaProperties;

    @Value("${multitenancy.tenant.entityManager.packages}")
    private String entityPackages;

    @Primary
    @Bean
    public LocalContainerEntityManagerFactoryBean tenantEntityManagerFactory(
            @Qualifier("dynamicDataSourceBasedMultiTenantConnectionProvider") MultiTenantConnectionProvider<String> connectionProvider,
            @Qualifier("currentTenantIdentifierResolver") CurrentTenantIdentifierResolver<String> tenantResolver) {
        LocalContainerEntityManagerFactoryBean emfBean = new LocalContainerEntityManagerFactoryBean();
        emfBean.setPersistenceUnitName("tenant-persistence-unit");
        emfBean.setPackagesToScan(entityPackages);

        JpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        emfBean.setJpaVendorAdapter(vendorAdapter);

        Map<String, Object> properties = new HashMap<>(this.jpaProperties.getProperties());
        properties.put("hibernate.physical_naming_strategy", CamelCaseToUnderscoresNamingStrategy.class.getName());
        properties.put("hibernate.implicit_naming_strategy", SpringImplicitNamingStrategy.class.getName());

        properties.put(AvailableSettings.BEAN_CONTAINER, new SpringBeanContainer(this.beanFactory));
        properties.put(AvailableSettings.MULTI_TENANT_CONNECTION_PROVIDER, connectionProvider);
        properties.put(AvailableSettings.MULTI_TENANT_IDENTIFIER_RESOLVER, tenantResolver);
        emfBean.setJpaPropertyMap(properties);

        return emfBean;
    }

    @Primary
    @Bean
    public JpaTransactionManager tenantTransactionManager(
            @Qualifier("tenantEntityManagerFactory") EntityManagerFactory emf) {
        JpaTransactionManager tenantTransactionManager = new JpaTransactionManager();
        tenantTransactionManager.setEntityManagerFactory(emf);
        return tenantTransactionManager;
    }
}
