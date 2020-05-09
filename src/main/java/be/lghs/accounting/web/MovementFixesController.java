package be.lghs.accounting.web;

import be.lghs.accounting.configuration.Roles;
import be.lghs.accounting.repositories.MovementRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/movements")
@RequiredArgsConstructor
public class MovementFixesController {

    private final MovementRepository movementRepository;

    @Transactional(readOnly = true)
    @GetMapping("/by-month/{month:" + PathRegexes.YEAR_MONTH + "}/fix-categories")
    @Secured(Roles.ROLE_TREASURER)
    public String fixCategories(@PathVariable("month") YearMonth month,
                                Model model) {
        var movements = movementRepository.missingCategories(month);
        var categories = movementRepository.categories();

        model.addAttribute("movements", movements);
        model.addAttribute("categories", categories);
        model.addAttribute("month", month);

        return "app/movements/fix-categories";
    }

    @Data
    public static class CategoryForm {
        private UUID movementId;
        private UUID categoryId;
    }

    @Data
    public static class CategoryFormList {
        private List<CategoryForm> movements;
    }

    @Transactional
    @PostMapping("/by-month/{month:" + PathRegexes.YEAR_MONTH + "}/fix-categories")
    @Secured(Roles.ROLE_TREASURER)
    public String fixCategories(@PathVariable("month") YearMonth month,
                                @ModelAttribute CategoryFormList form) {
        for (CategoryForm movement : form.movements) {
            // TODO Should we sanity check `month`?
            movementRepository.setCategory(movement.movementId, movement.categoryId);
        }

        return "redirect:/movements/by-month";
    }

    @Transactional(readOnly = true)
    @RequestMapping("/by-month/{month:" + PathRegexes.YEAR_MONTH + "}/fix-subscriptions")
    @Secured(Roles.ROLE_TREASURER)
    public String fixSubscriptions(@PathVariable("month") YearMonth month,
                                   Model model) {
        var movements = movementRepository.missingSubscription(month);
        var categories = movementRepository.categories();
        var categoryNamesById = movementRepository.categoryNamesById();

        model.addAttribute("movements", movements);
        model.addAttribute("categories", categories);
        model.addAttribute("categoryNamesById", categoryNamesById);

        return "app/movements/list";
    }
}
