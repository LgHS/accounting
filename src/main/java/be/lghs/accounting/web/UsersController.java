package be.lghs.accounting.web;

import be.lghs.accounting.configuration.Roles;
import be.lghs.accounting.model.enums.SubscriptionType;
import be.lghs.accounting.repositories.SubscriptionRepository;
import be.lghs.accounting.repositories.UserRepository;
import be.lghs.accounting.services.SubscriptionService;
import be.lghs.accounting.services.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
public class UsersController {

    //
    // If I understand this correctly,
    //   age < maxAge :               cache, no request
    //   age < staleWhileRevalidate : serve from cache, validate with server for next request (non
    //                                blocking, would be neat if the new image was swap if the cache
    //                                is not actually fresh)
    //   otherwise :                  no caching
    //
    // mustRevalidate should block the request while checking with the server for freshness, but I
    // couldn't get that behavior working in firefox (note that firefox is hiding freshness requests
    // right now - empty cache tab in the network panel, so maybe I'm just wrong).
    //
    private static final String GRAPH_CACHE_CONTROL_HEADER = CacheControl
        .maxAge(Duration.ofSeconds(15))
        .cachePrivate()
        // .mustRevalidate()
        .staleWhileRevalidate(Duration.ofHours(12))
        .getHeaderValue();

    private final UserRepository userRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final UserService userService;
    private final SubscriptionService subscriptionService;

    @GetMapping
    @Secured(Roles.ROLE_ADMIN)
    @Transactional(readOnly = true)
    public String userList(Model model) {
        var users = userRepository.findAll();
        model.addAttribute("users", users);

        return "app/users/list";
    }

    @GetMapping("/me")
    @Secured({ Roles.ROLE_MEMBER, Roles.ROLE_OLD_MEMBER })
    @Transactional(readOnly = true)
    public String userDetails(@RequestParam(value = "allPayments", defaultValue = "false") boolean loadAllPayments,
                              Model model) {
        var oAuth2User = userService.getCurrentUser().orElseThrow();

        return userDetails(loadAllPayments, oAuth2User.getId(), model);
    }

    @GetMapping("/{user_id:" + PathRegexes.UUID + "}")
    @Secured(Roles.ROLE_ADMIN)
    @Transactional(readOnly = true)
    public String userDetails(@RequestParam(value = "allPayments", defaultValue = "false") boolean loadAllPayments,
                              @PathVariable("user_id") UUID userId,
                              Model model) {
        var user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));

        var payments = subscriptionRepository.findLastSubscriptionsForUser(userId, loadAllPayments);

        model.addAttribute("loadAllPayments", loadAllPayments);
        model.addAttribute("payments", payments);
        model.addAttribute("user", user);

        return "app/users/details";
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

    private static long toEpochMillis(LocalDate date) {
        if (date == null) {
            return -1;
        }
        return date.toEpochSecond(LocalTime.MIDNIGHT, ZoneOffset.UTC /* meh */) * 1000;
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
