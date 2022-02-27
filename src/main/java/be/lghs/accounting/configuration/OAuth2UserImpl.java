package be.lghs.accounting.configuration;

import be.lghs.accounting.model.enums.UserRole;
import be.lghs.accounting.model.tables.records.UsersRecord;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class OAuth2UserImpl extends DefaultOAuth2User {

    private static Collection<? extends GrantedAuthority> getAuthorities(UserRole[] roles) {
        return Arrays.stream(roles)
                .map(UserRole::getLiteral)
                .map(Roles::fromString)
                .collect(Collectors.toSet());
    }

    private final UUID id;

    public OAuth2UserImpl(UserRole[] roles, Map<String, Object> attributes, UsersRecord record) {
        super(getAuthorities(roles), attributes, "preferred_username");
        this.id = record.getUuid();
    }

    public UUID getId() {
        return id;
    }
}
