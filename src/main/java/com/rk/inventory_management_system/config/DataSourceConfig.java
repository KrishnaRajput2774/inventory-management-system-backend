//package com.rk.inventory_management_system.config;
//
//import jakarta.persistence.EntityManagerFactory;
//import org.springframework.beans.factory.annotation.Qualifier;
//import org.springframework.boot.context.properties.ConfigurationProperties;
//import org.springframework.boot.jdbc.DataSourceBuilder;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.context.annotation.Primary;
//import org.springframework.core.env.Environment;
//import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
//import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;
//import org.springframework.orm.jpa.JpaTransactionManager;
//import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
//import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
//import org.springframework.transaction.PlatformTransactionManager;
//import org.springframework.transaction.annotation.EnableTransactionManagement;
//import org.springframework.transaction.support.TransactionSynchronizationManager;
//
//import javax.sql.DataSource;
//import java.util.HashMap;
//import java.util.Map;
//import java.util.Properties;
//
//@Configuration
//@EnableTransactionManagement
//@EnableJpaRepositories(
//        basePackages = "com.inventory.management.repository",
//        entityManagerFactoryRef = "entityManagerFactory",
//        transactionManagerRef = "transactionManager"
//)
//public class DataSourceConfig {
//
////
////    @Bean(name = "writeDataSource")
////    @ConfigurationProperties(prefix = "spring.datasource.write")
////    public DataSource writeDataSource() {
////        return DataSourceBuilder.create().build();
////    }
////
////    @Bean(name = "readDataSource")
////    @ConfigurationProperties(prefix = "spring.datasource.read")
////    public DataSource readDataSource() {
////        return DataSourceBuilder.create().build();
////    }
////
////    @Bean
////    @Primary
////    public DataSource routingDatasource(@Qualifier("writeDataSource") DataSource writeDataSource,
////                                        @Qualifier("readDataSource") DataSource readDataSource) {
////
////        Map<Object, Object> targetDatasources = new HashMap<>();
////        targetDatasources.put("WRITE",writeDataSource);
////        targetDatasources.put("READ",readDataSource);
////
////        RoutingDataSource routingDataSource = new RoutingDataSource();
////        routingDataSource.setDefaultTargetDataSource(writeDataSource);
////        routingDataSource.setTargetDataSources(targetDatasources);
////
////        return routingDataSource;
////    }
////
////    @Bean
////    @Primary
////    public LocalContainerEntityManagerFactoryBean entityManagerFactory(
////            @Qualifier("routingDatasource") DataSource dataSource, Environment environment) {
////        LocalContainerEntityManagerFactoryBean emf = new LocalContainerEntityManagerFactoryBean();
////        emf.setDataSource(dataSource);
////        emf.setPackagesToScan("com.rk.inventory_management_system.entities");
////        emf.setPersistenceUnitName("inventoryPU");
////
////        HibernateJpaVendorAdapter hibernateJpaVendorAdapter = new HibernateJpaVendorAdapter();
////        emf.setJpaVendorAdapter(hibernateJpaVendorAdapter);
////
////        Properties jpaProperties = new Properties();
////        jpaProperties.put("hibernate.hbm2ddl.auto", environment.getProperty("spring.jpa.hibernate.ddl-auto", "update"));
////        jpaProperties.put("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
////
////
////        emf.setJpaProperties(jpaProperties);
////
////        return emf;
////    }
////
////    @Bean(name = "transactionManager")
////    public PlatformTransactionManager transactionManager(
////            @Qualifier("entityManagerFactory") EntityManagerFactory emf) {
////        return new JpaTransactionManager(emf);
////    }
////
////    public static class RoutingDataSource extends AbstractRoutingDataSource {
////        @Override
////        protected Object determineCurrentLookupKey() {
////            return TransactionSynchronizationManager.isCurrentTransactionReadOnly()?"READ":"WRITE";
////        }
////    }
//
//
//
//}

package com.rk.inventory_management_system.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

@Configuration
public class DataSourceConfig {

    @Bean
    @Primary
    @ConfigurationProperties("spring.datasource.write")
    public DataSource writeDataSource() {
        return new HikariDataSource();
    }

    @Bean(name = "readOnlyDataSource")
    @ConfigurationProperties("spring.datasource.read")
    public DataSource readOnlyDataSource() {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setReadOnly(true);
        return dataSource;
    }
}
