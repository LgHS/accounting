package be.lghs.accounting.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.Duration;

@Configuration
public class WebConfiguration implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        CacheControl staticCacheControl = CacheControl
            .maxAge(Duration.ofHours(12))
            .cachePublic()
            // .mustRevalidate()
            .staleWhileRevalidate(Duration.ofMinutes(1));

        registry
            .addResourceHandler("/favicon.ico")
            .addResourceLocations("classpath:/public/")
            .setCacheControl(staticCacheControl);

        registry
            .addResourceHandler("/public/**")
            .addResourceLocations("classpath:/public/")
            .setCacheControl(staticCacheControl);
    }


    @Bean
    public JavaMailSender javaMailSender() {
        return new JavaMailSenderImpl();
    }
}
