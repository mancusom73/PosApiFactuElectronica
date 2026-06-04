package com.pos.factura.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import java.util.Properties;

/**
 * DataSource PRIMARIO: base de datos posFE.
 * Construye EntityManagerFactory directamente con HibernateJpaVendorAdapter
 * para no depender del bean EntityManagerFactoryBuilder
 * (que solo existe cuando HibernateJpaAutoConfiguration está activo).
 */
@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(
        basePackages = {
            "com.pos.factura.repository",
            "com.pos.factura.repository.factura"
        },
        excludeFilters = @ComponentScan.Filter(
            type = FilterType.REGEX,
            pattern = "com\\.pos\\.factura\\.repository\\.dbtpviv\\..*"
        ),
        entityManagerFactoryRef = "posfeEntityManagerFactory",
        transactionManagerRef  = "posfeTransactionManager"
)
public class PosFeDataSourceConfig {

    private final Environment env;

    public PosFeDataSourceConfig(Environment env) {
        this.env = env;
    }

    @Primary
    @Bean(name = "posfeDataSource")
    public DataSource posfeDataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(env.getRequiredProperty("datasource.posfe.url"));
        config.setUsername(env.getRequiredProperty("datasource.posfe.username"));
        config.setPassword(env.getRequiredProperty("datasource.posfe.password"));
        config.setDriverClassName(env.getRequiredProperty("datasource.posfe.driver-class-name"));
        config.setMaximumPoolSize(
                env.getProperty("datasource.posfe.hikari.maximum-pool-size", Integer.class, 10));
        config.setMinimumIdle(
                env.getProperty("datasource.posfe.hikari.minimum-idle", Integer.class, 2));
        config.setConnectionTimeout(
                env.getProperty("datasource.posfe.hikari.connection-timeout", Long.class, 30000L));
        config.setPoolName(
                env.getProperty("datasource.posfe.hikari.pool-name", "HikariPool-posFE"));
        return new HikariDataSource(config);
    }

    @Primary
    @Bean(name = "posfeEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean posfeEntityManagerFactory(
            @Qualifier("posfeDataSource") DataSource dataSource) {

        HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        vendorAdapter.setGenerateDdl(false);
        vendorAdapter.setShowSql(true);

        Properties jpaProps = new Properties();
        jpaProps.setProperty("hibernate.hbm2ddl.auto",  "validate");
        jpaProps.setProperty("hibernate.dialect",        "org.hibernate.dialect.MySQL5InnoDBDialect");
        jpaProps.setProperty("hibernate.show_sql",       "true");
        jpaProps.setProperty("hibernate.format_sql",     "true");

        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(dataSource);
        em.setPackagesToScan(
                "com.pos.factura.entity.posfe",   // Ticket, Producto, etc.
                "com.pos.factura.entity.factura"   // FacturaCabecera, Detalle, MediosPago
        );
        em.setPersistenceUnitName("posfe");
        em.setJpaVendorAdapter(vendorAdapter);
        em.setJpaProperties(jpaProps);
        return em;
    }

    @Primary
    @Bean(name = "posfeTransactionManager")
    public PlatformTransactionManager posfeTransactionManager(
            @Qualifier("posfeEntityManagerFactory") EntityManagerFactory emf) {
        return new JpaTransactionManager(emf);
    }
}
