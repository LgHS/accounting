package be.lghs.accounting.web.app;

import be.lghs.accounting.repositories.MovementRepository;
import lombok.RequiredArgsConstructor;
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
    public String dashboard(Model model) {
        model.addAttribute("legalSummary", movementRepository.legalSummary());
        return "app/dashboard";
    }
}
