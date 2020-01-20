package be.lghs.accounting.services;

import be.lghs.accounting.configuration.OAuth2UserImpl;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class UserService {

    public Optional<OAuth2UserImpl> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication.getPrincipal().equals("anonymousUser")) {
            return Optional.empty();
        }
        return Optional.ofNullable((OAuth2UserImpl) authentication.getPrincipal());
    }
}
