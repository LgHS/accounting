package be.lghs.accounting.configuration;

import be.lghs.accounting.services.UserService;
import com.mitchellbosecke.pebble.error.PebbleException;
import com.mitchellbosecke.pebble.extension.AbstractExtension;
import com.mitchellbosecke.pebble.extension.Extension;
import com.mitchellbosecke.pebble.extension.Filter;
import com.mitchellbosecke.pebble.extension.Function;
import com.mitchellbosecke.pebble.template.EvaluationContext;
import com.mitchellbosecke.pebble.template.PebbleTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.AuthenticatedPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

@Configuration
public class PebbleConfiguration {

    @Bean
    public Extension pebbleExtension(UserService userService) {
        return new AbstractExtension() {
            @Override
            public Map<String, Function> getFunctions() {
                return Map.of(
                    "username", function((args, self) -> userService.getCurrentUser()
                        .map(AuthenticatedPrincipal::getName)
                        .orElse("anonymous")),

                    "user", function((args, self) -> userService.getCurrentUser().orElseThrow()),

                    "authenticated", function((args, self) -> userService.getCurrentUser().isPresent()),

                    "csrf_token", function((args, self) -> getCsrf().getToken()),

                    "csrf_param", function((args, self) -> getCsrf().getParameterName()),

                    "has_admin_role", function((args, self) -> userService.getCurrentUser()
                        .map(OAuth2User::getAuthorities)
                        .map(authorities -> authorities.contains(Roles.ADMIN_AUTHORITY))
                        .orElse(false)),

                    "has_treasurer_role", function((args, self) -> userService.getCurrentUser()
                        .map(OAuth2User::getAuthorities)
                        .map(authorities -> authorities.contains(Roles.TREASURER_AUTHORITY))
                        .orElse(false))
                );
            }

            @Override
            public Map<String, Filter> getFilters() {
                return Map.of(
                    "ellipsis", new Filter() {
                        @Override
                        public Object apply(Object input, Map<String, Object> args, PebbleTemplate self, EvaluationContext context, int lineNumber) throws PebbleException {
                            String value = input.toString();
                            if (value.length() <= 7) {
                                return value;
                            }

                            return value.substring(0, 3) + "…" + value.substring(value.length() - 3);
                        }

                        @Override
                        public List<String> getArgumentNames() {
                            return List.of();
                        }
                    }
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

    private static CsrfToken getCsrf() {
        return (CsrfToken) ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
            .getRequest()
            .getAttribute(CsrfToken.class.getName());
    }
}
