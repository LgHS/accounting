package be.lghs.accounting;

import be.lghs.accounting.configuration.AccountingConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ImportResource;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@SpringBootApplication
@EnableTransactionManagement
@EnableWebMvc
@EnableConfigurationProperties(AccountingConfiguration.class)
@EnableWebSecurity
@ImportResource({
    "classpath:spring-database.xml",
})
public class AccountingApplication {

    public static void main(String... args) {
        SpringApplication.run(AccountingApplication.class, args);
    }
}
