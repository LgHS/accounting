package be.lghs.accounting.web.app;

import be.lghs.accounting.configuration.Roles;
import be.lghs.accounting.repositories.UserRepository;
import be.lghs.accounting.services.SubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
@RequestMapping("/app/users")
public class UsersController {

    private final UserRepository userRepository;
    private final SubscriptionService subscriptionService;

    @GetMapping
    @Secured(Roles.ROLE_ADMIN)
    @Transactional(readOnly = true)
    public String userList(Model model) {
        var users = userRepository.findAll();
        model.addAttribute("users", users);

        return "app/users/list";
    }

    @GetMapping("/{user_id}")
    @Secured(Roles.ROLE_ADMIN)
    @Transactional(readOnly = true)
    public String userDetails(@PathVariable("user_id") UUID userId,
                              Model model) {
        var user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        model.addAttribute("user", user);

        return "app/users/details";
    }

    @GetMapping(value = "/{user_id}/subscriptions/graph", produces = "image/svg+xml")
    @Secured(Roles.ROLE_MEMBER)
    @Transactional(readOnly = true)
    public void graph(@PathVariable("user_id") UUID userId,
                      @RequestParam(name = "width", required = false, defaultValue = "1200") int width,
                      HttpServletResponse response) throws IOException {
        response.setContentType("image/svg+xml");
        try (ServletOutputStream output = response.getOutputStream()) {
            subscriptionService.generateMonthlyGraphForUser(userId, output, width);
        }
    }
}
