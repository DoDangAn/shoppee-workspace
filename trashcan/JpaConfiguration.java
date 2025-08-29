package com.andd.DoDangAn.DoDangAn.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.util.Properties;

@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(
        basePackages = "com.andd.DoDangAn.DoDangAn.repository.jpa",
        entityManagerFactoryRef = "jpaEntityManagerFactory",
        transactionManagerRef = "jpaTransactionManager"
)
public class JpaConfiguration {

    @Primary
    @Bean(name = "jpaDataSource")
    @ConfigurationProperties(prefix = "spring.datasource.jpa")
    public DataSource jpaDataSource() {
        return DataSourceBuilder.create().build();
    }

    @Primary
    @Bean(name = "jpaEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean jpaEntityManagerFactory(
            @Qualifier("jpaDataSource") DataSource jpaDataSource) {
        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(jpaDataSource);
        em.setPackagesToScan("com.andd.DoDangAn.DoDangAn.models"); // Đảm bảo rằng gói chứa các entity JPA là đúng
        em.setJpaVendorAdapter(new HibernateJpaVendorAdapter());

        Properties properties = new Properties();
        properties.setProperty("hibernate.hbm2ddl.auto", "update"); // Tùy chọn, bạn có thể thay đổi
        properties.setProperty("hibernate.dialect", "org.hibernate.dialect.MySQLDialect"); // Thay đổi nếu dùng DB khác
        properties.setProperty("hibernate.show_sql", "true"); // Tùy chọn, hiển thị SQL
        em.setJpaProperties(properties);

        return em;
    }

    @Primary
    @Bean(name = "jpaTransactionManager")
    public PlatformTransactionManager jpaTransactionManager(
            @Qualifier("jpaEntityManagerFactory") LocalContainerEntityManagerFactoryBean jpaEntityManagerFactory) {
        return new JpaTransactionManager(jpaEntityManagerFactory.getObject());
    }
}