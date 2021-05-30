package be.lghs.accounting.web;

import be.lghs.accounting.configuration.Roles;
import be.lghs.accounting.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequiredArgsConstructor
@RequestMapping("/users")
public class UsersController {

    private final UserRepository userRepository;

    @GetMapping
    @Secured(Roles.ROLE_ADMIN)
    @Transactional(readOnly = true)
    public String userList(Model model) {
        var users = userRepository.findAll();
        model.addAttribute("users", users);

        return "app/users/list";
    }

    @GetMapping("/subscriptions")
    @Secured(Roles.ROLE_ADMIN)
    @Transactional(readOnly = true)
    public String userSubscriptions(Model model) {
        var users = userRepository.findAll();
        model.addAttribute("users", users);

        return "app/users/subscriptions";
    }
}
