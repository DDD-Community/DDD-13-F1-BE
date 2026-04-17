package com.f1.quiket.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.util.Properties;

/**
 * JPA 설정 클래스
 * JPA 엔티티 매니저, 트랜잭션 매니저, 감사 기능 등 설정
 */
@Configuration
@EnableJpaRepositories(basePackages = "com.f1.quiket.domain")
@EnableJpaAuditing  // JPA 감사 기능 활성화 (@CreatedDate, @LastModifiedDate 자동 관리)
@EnableTransactionManagement  // 선언적 트랜잭션 관리 활성화
public class JpaConfig {

    /**
     * JPA 엔티티 매니저 팩토리 빈 설정
     * 엔티티 스캔, JPA 벤더 설정, Hibernate 속성들을 구성합니다.
     */
    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource, Environment environment) {
        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(dataSource);
        em.setPackagesToScan("com.f1.quiket.domain");  // 엔티티 클래스 스캔 패키지
        em.setJpaVendorAdapter(new HibernateJpaVendorAdapter());  // Hibernate를 JPA 구현체로 사용

        // 프로파일별 YAML 값을 그대로 따라가게 해서 test/prod 설정을 분리합니다.
        Properties properties = new Properties();
        properties.setProperty("hibernate.hbm2ddl.auto",
                environment.getProperty("spring.jpa.hibernate.ddl-auto", "none"));
        properties.setProperty("hibernate.show_sql",
                environment.getProperty("spring.jpa.show-sql", "false"));
        properties.setProperty("hibernate.format_sql",
                environment.getProperty("spring.jpa.properties.hibernate.format_sql", "false"));
        properties.setProperty("hibernate.use_sql_comments",
                environment.getProperty("spring.jpa.properties.hibernate.use_sql_comments", "false"));

        em.setJpaProperties(properties);
        return em;
    }

    /**
     * JPA 트랜잭션 매니저 빈 설정
     * @Transactional 어노테이션이 이 매니저를 사용합니다.
     */
    @Bean
    public PlatformTransactionManager transactionManager(LocalContainerEntityManagerFactoryBean entityManagerFactory) {
        JpaTransactionManager transactionManager = new JpaTransactionManager();
        transactionManager.setEntityManagerFactory(entityManagerFactory.getObject());
        return transactionManager;
    }
}
