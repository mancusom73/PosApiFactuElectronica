package com.pos.factura.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
 * Configuración del DataSource SECUNDARIO: base de datos DBTPVIV.
 *
 * Maneja las tablas temporales externas ubicadas en el paquete
 * com.pos.factura.entity.dbtpviv.
 *
 * Sus repositorios viven en com.pos.factura.repository.dbtpviv.
 *
 * NO ejecuta Flyway — las tablas de DBTPVIV son externas y se gestionan
 * independientemente. Hibernate solo las lee (ddl-auto=none).
* Construye EntityManagerFactory directamente con HibernateJpaVendorAdapter
 * para no depender del bean EntityManagerFactoryBuilder.
 * ddl-auto=none: Hibernate no toca el esquema, las tablas son externas.
 */
@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(
        basePackages = "com.pos.factura.repository.dbtpviv",
        entityManagerFactoryRef = "dbtpvivEntityManagerFactory",
        transactionManagerRef  = "dbtpvivTransactionManager"
)
public class DbtpvivDataSourceConfig {

    private final Environment env;

    public DbtpvivDataSourceConfig(Environment env) {
        this.env = env;
    }

    @Bean(name = "dbtpvivDataSource")
    public DataSource dbtpvivDataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(env.getRequiredProperty("datasource.dbtpviv.url"));
        config.setUsername(env.getRequiredProperty("datasource.dbtpviv.username"));
        config.setPassword(env.getRequiredProperty("datasource.dbtpviv.password"));
        config.setDriverClassName(env.getRequiredProperty("datasource.dbtpviv.driver-class-name"));
        config.setMaximumPoolSize(
                env.getProperty("datasource.dbtpviv.hikari.maximum-pool-size", Integer.class, 5));
        config.setMinimumIdle(
                env.getProperty("datasource.dbtpviv.hikari.minimum-idle", Integer.class, 1));
        config.setConnectionTimeout(
                env.getProperty("datasource.dbtpviv.hikari.connection-timeout", Long.class, 30000L));
        config.setPoolName(
                env.getProperty("datasource.dbtpviv.hikari.pool-name", "HikariPool-DBTPVIV"));
        return new HikariDataSource(config);
    }

    @Bean(name = "dbtpvivEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean dbtpvivEntityManagerFactory(
            @Qualifier("dbtpvivDataSource") DataSource dataSource) {

        HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        vendorAdapter.setGenerateDdl(false);
        vendorAdapter.setShowSql(true);

        Properties jpaProps = new Properties();
        jpaProps.setProperty("hibernate.hbm2ddl.auto",  "none");
        jpaProps.setProperty("hibernate.dialect",        "org.hibernate.dialect.MySQL5InnoDBDialect");
        jpaProps.setProperty("hibernate.show_sql",       "true");
        jpaProps.setProperty("hibernate.format_sql",     "true");

        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(dataSource);
        em.setPackagesToScan("com.pos.factura.entity.dbtpviv");
        em.setPersistenceUnitName("dbtpviv");
        em.setJpaVendorAdapter(vendorAdapter);
        em.setJpaProperties(jpaProps);
        return em;
    }

    @Bean(name = "dbtpvivTransactionManager")
    public PlatformTransactionManager dbtpvivTransactionManager(
            @Qualifier("dbtpvivEntityManagerFactory") EntityManagerFactory emf) {
        return new JpaTransactionManager(emf);
    }
}
