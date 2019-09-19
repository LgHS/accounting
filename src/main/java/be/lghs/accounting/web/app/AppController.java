package be.lghs.accounting.web.app;

import be.lghs.accounting.configuration.Roles;
import be.lghs.accounting.repositories.AccountRepository;
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
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

@Controller
@RequestMapping("/app")
@RequiredArgsConstructor
public class AppController {

    private final MovementRepository movementRepository;
    private final AccountRepository accountRepository;
    private final GraphService graphService;

    @GetMapping
    @Secured(Roles.ROLE_MEMBER)
    public String dashboard(Model model) {
        var globalBalance = accountRepository.globalBalance();
        var monthsOfRentLeft = globalBalance
            .divideToIntegralValue(BigDecimal.valueOf(800))
            .setScale(0, RoundingMode.UNNECESSARY)
            .intValueExact();

        model.addAttribute("legalSummary", movementRepository.legalSummary());
        model.addAttribute("globalBalance", globalBalance);
        model.addAttribute("amountsPerMonth", movementRepository.amountsPerMonth());

        model.addAttribute("deadLine", LocalDate.now()
            .withDayOfMonth(1)
            .plusMonths(monthsOfRentLeft + 1));

        model.addAttribute("monthsOfRentLeft", monthsOfRentLeft);

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

    @GetMapping(value = "credits-per-day", produces = "image/svg+xml")
    @Secured(Roles.ROLE_MEMBER)
    public void creditsPerDay(HttpServletResponse response) throws IOException {
        response.setContentType("image/svg+xml");
        try (ServletOutputStream output = response.getOutputStream()) {
            graphService.generateCreditsPerDayGraph(output);
        }
    }
}
