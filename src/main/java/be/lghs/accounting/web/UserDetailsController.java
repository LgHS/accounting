package be.lghs.accounting.web;

import be.lghs.accounting.configuration.Roles;
import be.lghs.accounting.model.enums.SubscriptionType;
import be.lghs.accounting.repositories.MovementRepository;
import be.lghs.accounting.repositories.SubscriptionRepository;
import be.lghs.accounting.repositories.UserAccountNumberRepository;
import be.lghs.accounting.repositories.UserRepository;
import be.lghs.accounting.services.SubscriptionService;
import be.lghs.accounting.services.UserService;
import lombok.RequiredArgsConstructor;
import nl.garvelink.iban.IBAN;
import nl.garvelink.iban.Modulo97;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.WebRequest;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
@RequestMapping("/users")
public class UserDetailsController {

    //
    // If I understand this correctly,
    //   age < maxAge :               cache, no request
    //   age < staleWhileRevalidate : serve from cache, validate with server for next request (non
    //                                blocking, would be neat if the new image was swapped if the cache
    //                                is not actually fresh)
    //   otherwise :                  no caching
    //
    // mustRevalidate should block the request while checking with the server for freshness, but I
    // couldn't get that behavior working in firefox (note that firefox is hiding freshness requests
    // right now - empty cache tab in the network panel, so maybe I'm just wrong).
    //
    private static final String GRAPH_CACHE_CONTROL_HEADER = CacheControl
        .maxAge(Duration.ofMinutes(1))
        .cachePrivate()
        // .mustRevalidate()
        .staleWhileRevalidate(Duration.ofHours(24))
        .getHeaderValue();

    private static long toEpochMillis(LocalDate date) {
        if (date == null) {
            return -1;
        }
        return date.toEpochSecond(LocalTime.MIDNIGHT, ZoneOffset.UTC /* meh */) * 1000;
    }

    private final UserRepository userRepository;
    private final UserService userService;
    private final SubscriptionRepository subscriptionRepository;
    private final MovementRepository movementRepository;
    private final SubscriptionService subscriptionService;
    private final UserAccountNumberRepository userAccountNumberRepository;

    @GetMapping("/me")
    @Secured({ Roles.ROLE_MEMBER, Roles.ROLE_OLD_MEMBER })
    @Transactional(readOnly = true)
    public String userDetails(Model model) {
        var oAuth2User = userService.getCurrentUser().orElseThrow();

        return userDetails(oAuth2User.getId(), model);
    }

    @GetMapping("/{user_id:" + PathRegexes.UUID + "}")
    @Secured(Roles.ROLE_ADMIN)
    @Transactional(readOnly = true)
    public String userDetails(@PathVariable("user_id") UUID userId,
                              Model model) {
        var user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        var statistics = userRepository.statistics(userId);

        model.addAttribute("user", user);
        model.addAttribute("firstSeen", statistics.get("firstSeen"));
        model.addAttribute("lastSeen", statistics.get("lastSeen"));
        model.addAttribute("movementCount", statistics.get("movementCount"));
        model.addAttribute("totalAmount", statistics.get("totalAmount"));
        model.addAttribute("endMonthly", statistics.get("endMonthly"));
        model.addAttribute("endYearly", statistics.get("endYearly"));

        return "app/users/details/summary";
    }

    @GetMapping("/me/movements")
    @Secured({ Roles.ROLE_MEMBER, Roles.ROLE_OLD_MEMBER })
    @Transactional(readOnly = true)
    public String userMovements(@RequestParam(value = "allSubscriptions", defaultValue = "false") boolean loadAllSubscriptions,
                                Model model) {
        var oAuth2User = userService.getCurrentUser().orElseThrow();

        return userMovements(loadAllSubscriptions, oAuth2User.getId(), model);
    }

    @GetMapping("/{user_id:" + PathRegexes.UUID + "}/movements")
    @Secured(Roles.ROLE_ADMIN)
    @Transactional(readOnly = true)
    public String userMovements(@RequestParam(value = "allSubscriptions", defaultValue = "false") boolean loadAllSubscriptions,
                                @PathVariable("user_id") UUID userId,
                                Model model) {
        var user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));


        var subscriptions = subscriptionRepository.findLastSubscriptionsForUser(userId, loadAllSubscriptions);
        var unlinkedSubscriptions = movementRepository.unlinkedSubscriptions(userId);

        model.addAttribute("loadAllSubscriptions", loadAllSubscriptions);
        model.addAttribute("subscriptions", subscriptions);
        model.addAttribute("unlinkedSubscriptions", unlinkedSubscriptions);
        model.addAttribute("user", user);

        return "app/users/details/movements";
    }




    @GetMapping("/me/account-numbers")
    @Secured({ Roles.ROLE_MEMBER, Roles.ROLE_OLD_MEMBER })
    @Transactional(readOnly = true)
    public String userAccountNumbers(Model model) {
        var oAuth2User = userService.getCurrentUser().orElseThrow();

        return userAccountNumbers(oAuth2User.getId(), model);
    }

    @GetMapping("/{user_id:" + PathRegexes.UUID + "}/account-numbers")
    @Secured(Roles.ROLE_ADMIN)
    @Transactional(readOnly = true)
    public String userAccountNumbers(@PathVariable("user_id") UUID userId,
                                     Model model) {
        var user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));

        model.addAttribute("user", user);
        model.addAttribute("accountNumbers", userAccountNumberRepository.forUser(userId));

        return "app/users/details/account-numbers";
    }

    @PostMapping("/me/account-numbers")
    @Secured({ Roles.ROLE_MEMBER, Roles.ROLE_OLD_MEMBER })
    @Transactional
    public String addAccountNumber(@RequestParam("account_number") String accountNumber,
                                   Model model) {
        var oAuth2User = userService.getCurrentUser().orElseThrow();

        var pendingAccountNumbers = userAccountNumberRepository.countPendingAccountNumbersForUser(oAuth2User.getId());
        if (pendingAccountNumbers >= 3) {
            throw new IllegalArgumentException("You are limited to 3 pending numbers before you can propose a new one");
        }

        return addAccountNumber(oAuth2User.getId(), accountNumber, model);
    }

    @PostMapping("/{user_id:" + PathRegexes.UUID + "}/account-numbers")
    @Secured(Roles.ROLE_ADMIN)
    @Transactional
    public String addAccountNumber(@PathVariable("user_id") UUID userId,
                                   @RequestParam("account_number") String accountNumber,
                                   Model model) {
        if (!Modulo97.verifyCheckDigits(accountNumber)) {
            throw new IllegalArgumentException("invalid iban");
        }

        IBAN iban = IBAN.parse(accountNumber);
        userAccountNumberRepository.addAccountNumber(userId, iban.toPlainString());

        return userAccountNumbers(userId, model);
    }


    @GetMapping(value = "/me/subscriptions/graph/monthly", produces = "image/svg+xml")
    @Secured({ Roles.ROLE_MEMBER, Roles.ROLE_OLD_MEMBER })
    @Transactional(readOnly = true)
    public void monthlyGraph(@RequestParam(name = "width", required = false, defaultValue = "1200") int width,
                             @RequestParam(name = "height", required = false, defaultValue = "100") int height,
                             @RequestParam(name = "title", required = false, defaultValue = "true") boolean drawTitle,
                             WebRequest request,
                             HttpServletResponse response) throws IOException {
        monthlyGraph(userService.getCurrentUser().orElseThrow().getId(),
            width, height, drawTitle, request, response);
    }

    @GetMapping(value = "/me/subscriptions/graph/yearly", produces = "image/svg+xml")
    @Secured({ Roles.ROLE_MEMBER, Roles.ROLE_OLD_MEMBER })
    @Transactional(readOnly = true)
    public void yearlyGraph(@RequestParam(name = "width", required = false, defaultValue = "1200") int width,
                            @RequestParam(name = "height", required = false, defaultValue = "100") int height,
                            @RequestParam(name = "title", required = false, defaultValue = "true") boolean drawTitle,
                            WebRequest request,
                            HttpServletResponse response) throws IOException {
        yearlyGraph(userService.getCurrentUser().orElseThrow().getId(),
            width, height, drawTitle, request, response);
    }

    @GetMapping(value = "/{user_id:" + PathRegexes.UUID + "}/subscriptions/graph/monthly", produces = "image/svg+xml")
    @Secured(Roles.ROLE_ADMIN)
    @Transactional(readOnly = true)
    public void monthlyGraph(@PathVariable("user_id") UUID userId,
                             @RequestParam(name = "width", required = false, defaultValue = "1200") int width,
                             @RequestParam(name = "height", required = false, defaultValue = "100") int height,
                             @RequestParam(name = "title", required = false, defaultValue = "true") boolean drawTitle,
                             WebRequest request,
                             HttpServletResponse response) throws IOException {
        LocalDate last = subscriptionRepository.getLastSubscription(userId, SubscriptionType.MONTHLY);
        response.setHeader(HttpHeaders.CACHE_CONTROL, GRAPH_CACHE_CONTROL_HEADER);
        response.setDateHeader(HttpHeaders.LAST_MODIFIED, toEpochMillis(last));
        if (request.checkNotModified(toEpochMillis(last))) {
            return;
        }

        response.setContentType("image/svg+xml");
        try (ServletOutputStream output = response.getOutputStream()) {
            subscriptionService.generateMonthlyGraphForUser(
                userId, output, width, height, drawTitle);
        }
    }

    @GetMapping(value = "/{user_id:" + PathRegexes.UUID + "}/subscriptions/graph/yearly", produces = "image/svg+xml")
    @Secured(Roles.ROLE_ADMIN)
    @Transactional(readOnly = true)
    public void yearlyGraph(@PathVariable("user_id") UUID userId,
                            @RequestParam(name = "width", required = false, defaultValue = "1200") int width,
                            @RequestParam(name = "height", required = false, defaultValue = "100") int height,
                            @RequestParam(name = "title", required = false, defaultValue = "true") boolean drawTitle,
                            WebRequest request,
                            HttpServletResponse response) throws IOException {
        LocalDate last = subscriptionRepository.getLastSubscription(userId, SubscriptionType.YEARLY);
        response.setHeader(HttpHeaders.CACHE_CONTROL, GRAPH_CACHE_CONTROL_HEADER);
        response.setDateHeader(HttpHeaders.LAST_MODIFIED, toEpochMillis(last));
        if (request.checkNotModified(toEpochMillis(last))) {
            return;
        }

        response.setContentType("image/svg+xml");
        try (ServletOutputStream output = response.getOutputStream()) {
            subscriptionService.generateYearlyGraphForUser(
                userId, output, width, height, drawTitle);
        }
    }
}
