package be.lghs.accounting.configuration;

import com.mitchellbosecke.pebble.extension.AbstractExtension;
import com.mitchellbosecke.pebble.extension.Extension;
import com.mitchellbosecke.pebble.extension.Function;
import com.mitchellbosecke.pebble.template.EvaluationContext;
import com.mitchellbosecke.pebble.template.PebbleTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.AuthenticatedPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;

@Configuration
public class PebbleConfiguration {

    @Bean
    public Extension pebbleExtension() {
        return new AbstractExtension() {
            @Override
            public Map<String, Function> getFunctions() {
                return Map.of(
                    "username", function((args, self) -> getUser()
                        .map(AuthenticatedPrincipal::getName)
                        .orElse("anonymous")),

                    "user", function((args, self) -> getUser().orElseThrow()),

                    "authenticated", function((args, self) -> getUser().isPresent()),

                    "csrf_token", function((args, self) -> getCsrf().getToken()),

                    "csrf_param", function((args, self) -> getCsrf().getParameterName())
                );
            }
        };
    }

    public static Function function(BiFunction<Map<String, Object>, PebbleTemplate, Object> function,
                                    String... argumentNames) {
        return new Function() {
            @Override
            public Object execute(Map<String, Object> args,
                                  PebbleTemplate self,
                                  EvaluationContext context,
                                  int lineNumber) {
                return function.apply(args, self);
            }

            @Override
            public List<String> getArgumentNames() {
                return List.of(argumentNames);
            }
        };
    }


    private static Optional<OAuth2User> getUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication.getPrincipal().equals("anonymousUser")) {
            return Optional.empty();
        }
        return Optional.ofNullable((OAuth2User) authentication.getPrincipal());
    }

    private static CsrfToken getCsrf() {
        return (CsrfToken) ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
            .getRequest()
            .getAttribute(CsrfToken.class.getName());
    }
}
