package be.lghs.accounting;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ImportResource;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@SpringBootApplication
@EnableWebMvc
@EnableWebSecurity
@ImportResource("classpath:spring-config.xml")
public class AccountingApplication {

    public static void main(String... args) {
        SpringApplication.run(AccountingApplication.class, args);
    }
}
