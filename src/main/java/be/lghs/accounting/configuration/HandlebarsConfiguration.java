package be.lghs.accounting.configuration;

import com.github.jknack.handlebars.Options;
import com.github.jknack.handlebars.helper.ConditionalHelpers;
import com.github.jknack.handlebars.springmvc.HandlebarsViewResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.AuthenticatedPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import pl.allegro.tech.boot.autoconfigure.handlebars.HandlebarsHelper;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.Optional;

@HandlebarsHelper
@RequiredArgsConstructor
public class HandlebarsConfiguration {

    private final HandlebarsViewResolver handlebarsViewResolver;

    @PostConstruct
    public void postConstruct() {
        handlebarsViewResolver.getHandlebars().registerHelpers(ConditionalHelpers.class);
    }

    private static Optional<OAuth2User> getUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication.getPrincipal().equals("anonymousUser")) {
            return Optional.empty();
        }
        return Optional.ofNullable((OAuth2User) authentication.getPrincipal());
    }

    public String user() {
        return getUser()
            .map(AuthenticatedPrincipal::getName)
            .orElse("anonymous");
    }

    public CharSequence logged(Options options) throws IOException {
        Optional<OAuth2User> user = getUser();
        if (user.isPresent()) {
            return options.fn();
        } else {
            return options.inverse();
        }
    }



    public String csrf_token() {
        return getCsrf().getToken();
    }

    public String csrf_param() {
        return getCsrf().getParameterName();
    }

    private CsrfToken getCsrf() {
        return (CsrfToken) ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
            .getRequest()
            .getAttribute(CsrfToken.class.getName());
    }
}
