package be.lghs.accounting.configuration;

import be.lghs.accounting.model.tables.records.UsersRecord;
import be.lghs.accounting.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SecurityUserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;


    private static String getMandatory(Map<String, Object> values, String attribute) {
        if (values.containsKey(attribute)) {
            return (String) values.get(attribute);
        } else {
            throw new RuntimeException("Missing attribute " + attribute + " for user ");
        }
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User user = super.loadUser(userRequest);
        Map<String, Object> attributes = user.getAttributes();

        UsersRecord usersRecord = userRepository.ensureUserExists(
                UUID.fromString(getMandatory(attributes, "sub")),
                getMandatory(attributes, "name"),
                getMandatory(attributes, "preferred_username"),
                getMandatory(attributes, "email"));

        return new OAuth2UserImpl(
                usersRecord.getRoles(),
                attributes,
                usersRecord
        );
    }
}
