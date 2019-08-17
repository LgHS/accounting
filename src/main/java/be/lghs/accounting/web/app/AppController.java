package be.lghs.accounting.web.app;

import be.lghs.accounting.configuration.Roles;
import be.lghs.accounting.repositories.MovementRepository;
import be.lghs.accounting.services.GraphService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Controller
@RequestMapping("/app")
@RequiredArgsConstructor
public class AppController {

    private final MovementRepository movementRepository;
    private final GraphService graphService;

    @GetMapping
    @Secured(Roles.ROLE_MEMBER)
    public String dashboard(Model model) {
        model.addAttribute("legalSummary", movementRepository.legalSummary());
        return "app/dashboard";
    }

    @GetMapping(value = "rolling-sum", produces = "image/svg+xml")
    @Secured(Roles.ROLE_MEMBER)
    public void rollingSum(HttpServletResponse response) throws IOException {
        response.setContentType("image/svg+xml");
        try (ServletOutputStream output = response.getOutputStream()) {
            graphService.generateRollingSumGraph(output);
        }
    }
}
