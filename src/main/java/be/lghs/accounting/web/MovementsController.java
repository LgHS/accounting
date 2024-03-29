package be.lghs.accounting.web;

import be.lghs.accounting.configuration.OAuth2UserImpl;
import be.lghs.accounting.configuration.Roles;
import be.lghs.accounting.model.enums.SubscriptionType;
import be.lghs.accounting.model.tables.records.MovementCategoriesRecord;
import be.lghs.accounting.model.tables.records.MovementsRecord;
import be.lghs.accounting.repositories.*;
import be.lghs.accounting.services.MovementService;
import be.lghs.accounting.services.UserService;
import lombok.RequiredArgsConstructor;
import org.jooq.Result;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.UUID;

@Controller
@RequestMapping("/movements")
@RequiredArgsConstructor
public class MovementsController {

    private final MovementRepository movementRepository;
    private final MovementCategoryRepository movementCategoryRepository;
    private final MovementService movementService;
    private final SubscriptionRepository subscriptionRepository;
    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final UserService userService;

    @GetMapping
    @Transactional(readOnly = true)
    @Secured(Roles.ROLE_TREASURER)
    public String movements(Model model) {
        var movements = movementRepository.findAll();
        var categories = movementRepository.categories();
        var categoryNamesById = movementRepository.categoryNamesById();

        model.addAttribute("movements", movements);
        model.addAttribute("categories", categories);
        model.addAttribute("categoryNamesById", categoryNamesById);

        return "app/movements/list";
    }

    @GetMapping("/{movement_id:" + PathRegexes.UUID + "}")
    @Transactional(readOnly = true)
    @Secured(Roles.ROLE_TREASURER)
    public String movements(@PathVariable("movement_id") UUID movementId,
                            Model model) {
        var movement = movementRepository.getOne(movementId);
        var categories = movementRepository.categories();

        model.addAttribute("movement", movement);
        model.addAttribute("categories", categories);

        return "app/movements/view";
    }

    @Transactional
    @PostMapping("/{movement_id:" + PathRegexes.UUID + "}/category")
    @Secured(Roles.ROLE_TREASURER)
    public String setCategory(@PathVariable("movement_id") UUID movementId,
                              @RequestParam("category_id") UUID categoryId) {
        if (categoryId == null) {
            return "redirect:/movements";
        }
        movementRepository.setCategory(movementId, categoryId);
        return "redirect:/movements#"+movementId;
    }

    @Transactional(readOnly = true)
    @GetMapping("/by-iban/{iban}")
    @Secured(Roles.ROLE_TREASURER)
    public String movementsFromIban(@PathVariable("iban") String iban,
                                    Model model) {
        var movements = movementRepository.findFromCounterParty(iban);
        var categories = movementRepository.categories();
        var categoryNamesById = movementRepository.categoryNamesById();

        model.addAttribute("movements", movements);
        model.addAttribute("categories", categories);
        model.addAttribute("categoryNamesById", categoryNamesById);

        return "app/movements/list";
    }

    @Transactional(readOnly = true)
    @GetMapping("/by-month")
    @Secured(Roles.ROLE_ADMIN)
    public String movementsByMonth(Model model) {
        var movements = movementRepository.monthsSummaries();

        var monthFormatter = new DateTimeFormatterBuilder()
            .appendValue(ChronoField.YEAR, 4)
            .appendLiteral('-')
            .appendValue(ChronoField.MONTH_OF_YEAR, 2)
            .toFormatter();

        model.addAttribute("monthFormatter", monthFormatter);
        model.addAttribute("monthlySummaries", movements);

        return "app/movements/monthly";
    }

    @Transactional(readOnly = true)
    @GetMapping("/by-month/{month:" + PathRegexes.YEAR_MONTH + "}")
    @Secured(Roles.ROLE_TREASURER)
    public String movementsByMonth(@PathVariable("month") YearMonth month,
                                   Model model) {
        var movements = movementRepository.findForMonth(month);
        var categories = movementRepository.categories();
        var categoryNamesById = movementRepository.categoryNamesById();

        model.addAttribute("movements", movements);
        model.addAttribute("categories", categories);
        model.addAttribute("categoryNamesById", categoryNamesById);

        return "app/movements/list";
    }

    @Transactional(readOnly = true)
    @GetMapping("/by-category/{category_id:" + PathRegexes.UUID + "}")
    @Secured(Roles.ROLE_TREASURER)
    public String movementsFromIban(@PathVariable("category_id") UUID categoryId,
                                    Model model) {
        var movements = movementRepository.findByCategory(categoryId);
        var categories = movementRepository.categories();
        var categoryNamesById = movementRepository.categoryNamesById();

        model.addAttribute("movements", movements);
        model.addAttribute("categories", categories);
        model.addAttribute("categoryNamesById", categoryNamesById);

        return "app/movements/list";
    }

    @Transactional(readOnly = true)
    @GetMapping("/{movement_id:" + PathRegexes.UUID + "}/split")
    @Secured(Roles.ROLE_TREASURER)
    public String splitMovementForm(@PathVariable("movement_id") UUID movementId,
                                    Model model) {
        MovementsRecord movement = movementRepository.getOne(movementId);
        Result<MovementCategoriesRecord> categories;
        if (movement.getAmount().signum() > 0) {
            categories = movementCategoryRepository.credits();
        } else {
            categories = movementCategoryRepository.debits();
        }

        model.addAttribute("movement", movement);
        model.addAttribute("categories", categories);

        return "app/movements/split";
    }

    @Transactional
    @PostMapping("/{movement_id:" + PathRegexes.UUID + "}/split")
    @Secured(Roles.ROLE_TREASURER)
    public String splitMovement(@PathVariable("movement_id") UUID movementId,
                                @RequestParam("communication") String communication,
                                @RequestParam("amount") BigDecimal amount,
                                @RequestParam("category") UUID categoryId,
                                @RequestParam("communication_split") String communicationSplit,
                                @RequestParam("amount_split") BigDecimal amountSplit,
                                @RequestParam("category_split") UUID categorySplitId) {
        MovementsRecord partOne = movementService.splitMovement(
            movementId,
            communication, amount, categoryId,
            communicationSplit, amountSplit, categorySplitId);

        return "redirect:/movements#" + partOne.getId();
    }

    @Transactional(readOnly = true)
    @GetMapping("/{movement_id:" + PathRegexes.UUID + "}/subscription")
    @Secured(Roles.ROLE_TREASURER)
    public String subscriptionForm(@PathVariable("movement_id") UUID movementId,
                                   Model model) {
        var movement = movementRepository.getOne(movementId);
        var subscription = subscriptionRepository.getForMovement(movementId);
        var users = userRepository.findUsersWithLastSubscriptions();

        model.addAttribute("users", users);
        model.addAttribute("movement", movement);
        model.addAttribute("subscription", subscription);
        model.addAttribute("monthFormatter", DateTimeFormatter.ofPattern("yyyy-MM"));

        return "app/subscriptions/form";
    }

    @Transactional
    @PostMapping("/{movement_id:" + PathRegexes.UUID + "}/subscription")
    @Secured(Roles.ROLE_TREASURER)
    public String subscriptionForm(@PathVariable("movement_id") UUID movementId,
                                   @RequestParam("username") String username,
                                   @RequestParam("type") SubscriptionType type,
                                   @RequestParam("start") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                   @RequestParam("end") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                                   @RequestParam("comment") String comment,
                                   Model model) {
        subscriptionRepository.linkMovement(
            userService.getCurrentUser().map(OAuth2UserImpl::getId).orElseThrow(),
            movementId, username, type, startDate, endDate, comment.isBlank() ? null : comment);

        return subscriptionForm(movementId, model);
    }

    @Transactional(readOnly = true)
    @GetMapping("/add")
    @Secured(Roles.ROLE_ADMIN)
    public String movementForm(Model model) {
        var accounts = accountRepository.findAll();
        model.addAttribute("accounts", accounts);
        return "app/movements/add";
    }

    @Transactional
    @PostMapping("/add")
    @Secured(Roles.ROLE_ADMIN)
    public String addMovement(@RequestParam("account_id") UUID accountId,
                              @RequestParam("amount") BigDecimal amount,
                              @RequestParam("communication") String communication,
                              @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        UUID movementId = movementService.addMovement(accountId, amount, communication, date);
        return "redirect:/movements#" + movementId;
    }
}
