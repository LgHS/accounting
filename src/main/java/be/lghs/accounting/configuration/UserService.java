package be.lghs.accounting.configuration;

import be.lghs.accounting.model.enums.UserRole;
import be.lghs.accounting.model.tables.records.UsersRecord;
import be.lghs.accounting.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User user = super.loadUser(userRequest);
        Map<String, Object> attributes = user.getAttributes();

        UsersRecord usersRecord = userRepository.ensureUserExists(
            (int) attributes.get("id"),
            UUID.fromString((String) attributes.get("uuid")),
            (String) attributes.get("name"),
            (String) attributes.get("username"),
            (String) attributes.get("email"));

        return new DefaultOAuth2User(
            getAuthorities(usersRecord),
            attributes,
            "username"
        );
    }

    private Collection<? extends GrantedAuthority> getAuthorities(UsersRecord usersRecord) {
        return Arrays.stream(usersRecord.getRoles())
            .map(UserRole::getLiteral)
            .map(SimpleGrantedAuthority::new)
            .collect(Collectors.toSet());
    }
}
