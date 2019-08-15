package be.lghs.accounting.web.app;

import be.lghs.accounting.configuration.Roles;
import be.lghs.accounting.repositories.MovementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/app")
@RequiredArgsConstructor
public class AppController {

    private final MovementRepository movementRepository;

    @GetMapping
    @Secured(Roles.ROLE_MEMBER)
    public String dashboard(Model model) {
        model.addAttribute("legalSummary", movementRepository.legalSummary());
        return "app/dashboard";
    }
}
