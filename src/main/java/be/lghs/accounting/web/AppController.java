package be.lghs.accounting.web;

import be.lghs.accounting.configuration.AccountingConfiguration;
import be.lghs.accounting.repositories.AccountRepository;
import be.lghs.accounting.repositories.MovementRepository;
import be.lghs.accounting.services.GraphService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

@Controller
@RequestMapping("/")
@RequiredArgsConstructor
public class AppController {

    private final MovementRepository movementRepository;
    private final AccountRepository accountRepository;
    private final GraphService graphService;
    private final AccountingConfiguration config;

    @GetMapping
    public String dashboard(Model model) {
        var globalBalance = accountRepository.globalBalance();
        var monthsOfRentLeft = globalBalance
            .divideToIntegralValue(config.getAverageMonthlyRent())
            .setScale(0, RoundingMode.UNNECESSARY)
            .intValueExact();

        var monthFormatter = DateTimeFormatter.ofPattern("MMMM yy");

        model.addAttribute("monthFormatter", monthFormatter);
        model.addAttribute("legalSummary", movementRepository.legalSummary());
        model.addAttribute("globalBalance", globalBalance);
        model.addAttribute("amountsPerMonth", movementRepository.amountsPerMonth());

        model.addAttribute("deadLine", LocalDate.now()
            .withDayOfMonth(1)
            .plusMonths(monthsOfRentLeft + 1));

        model.addAttribute("monthsOfRentLeft", monthsOfRentLeft);

        return "app/dashboard";
    }

    @GetMapping(value = "/graphs/rolling-sum", produces = "image/svg+xml")
    public void rollingSum(HttpServletResponse response) throws IOException {
        response.setContentType("image/svg+xml");
        response.setHeader(HttpHeaders.PRAGMA, "");
        response.setHeader(HttpHeaders.CACHE_CONTROL, CacheControl
            .maxAge(4, TimeUnit.HOURS)
            .cachePublic()
            .getHeaderValue());
        try (ServletOutputStream output = response.getOutputStream()) {
            graphService.generateRollingSumGraph(output);
        }
    }

    @GetMapping(value = "/graphs/credits-per-day", produces = "image/svg+xml")
    public void creditsPerDay(HttpServletResponse response) throws IOException {
        response.setContentType("image/svg+xml");
        response.setHeader(HttpHeaders.PRAGMA, "");
        response.setHeader(HttpHeaders.CACHE_CONTROL, CacheControl
            .maxAge(4, TimeUnit.HOURS)
            .cachePublic()
            .getHeaderValue());
        try (ServletOutputStream output = response.getOutputStream()) {
            graphService.generateCreditsPerDayGraph(output);
        }
    }
}
