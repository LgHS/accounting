package be.lghs.accounting.configuration;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

@Configuration
@EnableGlobalMethodSecurity(
    securedEnabled = true,
    prePostEnabled = true
)
@RequiredArgsConstructor
public class OAuth2SecurityConfiguration extends WebSecurityConfigurerAdapter {

    private final SecurityUserService securityUserService;

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.authorizeRequests()
            .mvcMatchers("/").permitAll()
            .antMatchers("/main.css").permitAll()
            .antMatchers("/public/**").permitAll()
            .anyRequest().authenticated()

            .and()
                .oauth2Login().userInfoEndpoint().userService(securityUserService)
        ;
    }
}
