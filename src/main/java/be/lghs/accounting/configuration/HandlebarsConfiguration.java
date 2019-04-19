package be.lghs.accounting.configuration;

import com.github.jknack.handlebars.Options;
import org.springframework.security.core.AuthenticatedPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import pl.allegro.tech.boot.autoconfigure.handlebars.HandlebarsHelper;

import java.io.IOException;
import java.util.Optional;

@HandlebarsHelper
public class HandlebarsConfiguration {

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
}
