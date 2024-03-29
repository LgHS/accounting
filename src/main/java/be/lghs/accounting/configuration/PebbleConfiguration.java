package be.lghs.accounting.configuration;

import be.lghs.accounting.services.UserService;
import com.mitchellbosecke.pebble.error.PebbleException;
import com.mitchellbosecke.pebble.extension.AbstractExtension;
import com.mitchellbosecke.pebble.extension.Extension;
import com.mitchellbosecke.pebble.extension.Filter;
import com.mitchellbosecke.pebble.extension.Function;
import com.mitchellbosecke.pebble.template.EvaluationContext;
import com.mitchellbosecke.pebble.template.PebbleTemplate;
import nl.garvelink.iban.IBAN;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.AuthenticatedPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDate;
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
                    "current_date", function((args, self) -> LocalDate.now()),

                    "day_of_month", function((args, self) -> {
                        var base = (LocalDate) args.getOrDefault("date", LocalDate.now());
                        if (args.getOrDefault("first", Boolean.FALSE) == Boolean.TRUE) {
                            return base.withDayOfMonth(1);
                        }
                        if (args.getOrDefault("last", Boolean.FALSE) == Boolean.TRUE) {
                            return base.withDayOfMonth(1).plusMonths(1).minusDays(1);
                        }
                        return base;
                    }, "first", "last", "date"),

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

                    "is_old_member", function((args, self) -> userService.getCurrentUser()
                        .map(OAuth2User::getAuthorities)
                        .map(authorities -> authorities.contains(Roles.OLD_MEMBER_AUTHORITY))
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
                    "ellipsis", filter(input -> {
                        String value = input.toString();
                        if (value.length() <= 7) {
                            return value;
                        }

                        return value.substring(0, 3) + "…" + value.substring(value.length() - 3);
                    }),

                    "first_day_of_month", filter(input -> {
                        var date = (LocalDate) input;
                        return date.withDayOfMonth(1);
                    }),

                    "format_iban", filter(input -> IBAN.parse(input.toString()).toString()),

                    "last_day_of_month", filter(input -> {
                        var date = (LocalDate) input;
                        return date
                            .withDayOfMonth(1)
                            .plusMonths(1)
                            .minusDays(1);
                    })
                );
            }
        };
    }

    public static <T> Filter filter(java.util.function.Function<Object, T> function) {
        return new Filter() {
            @Override
            public Object apply(Object input, Map<String, Object> args, PebbleTemplate self, EvaluationContext context, int lineNumber) throws PebbleException {
                return function.apply(input);
            }

            @Override
            public List<String> getArgumentNames() {
                return List.of();
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
