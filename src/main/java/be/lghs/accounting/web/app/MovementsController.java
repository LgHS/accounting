package be.lghs.accounting.web.app;

import be.lghs.accounting.repositories.MovementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Controller
@RequestMapping("/app/movements")
@RequiredArgsConstructor
public class MovementsController {

    private final MovementRepository movementRepository;

    @GetMapping
    @Transactional(readOnly = true)
    public String movements(Model model) {
        var movements = movementRepository.findAll();
        var categories = movementRepository.categories();
        model.addAttribute("movements", movements);
        model.addAttribute("categories", categories);
        return "app/movements/list";
    }

    @Transactional
    @PostMapping("/{movement_id}/category")
    public String setCategory(@PathVariable("movement_id") UUID movementId,
                              @RequestParam("category_id") UUID categoryId) {
        if (categoryId == null) {
            return "redirect:/app/movements";
        }
        movementRepository.setCategory(movementId, categoryId);
        return "redirect:/app/movements#"+movementId;
    }
}
