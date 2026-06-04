package com.pos.factura;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;

/**
 * Excluimos los autoconfigurators de DataSource y JPA porque
 * los configuramos manualmente para dos bases de datos:
 *   - posFE    → PosFeDataSourceConfig
 *   - DBTPVIV  → DbtpvivDataSourceConfig
 */
@SpringBootApplication(exclude = {
        DataSourceAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class
})
public class FacturaElectronicaApplication {
    public static void main(String[] args) {
        SpringApplication.run(FacturaElectronicaApplication.class, args);
    }
}
