package be.lghs.movementing.web.app;

import be.lghs.accounting.model.tables.records.MovementsRecord;
import be.lghs.accounting.repositories.MovementRepository;
import lombok.RequiredArgsConstructor;
import org.jooq.Result;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/app/movements")
@RequiredArgsConstructor
public class MovementsController {

    private final MovementRepository movementRepository;

    @GetMapping
    @Transactional(readOnly = true)
    public String movements(Model model) {
        Result<MovementsRecord> movements = movementRepository.findAll();
        model.addAttribute("movements", movements);
        return "app/movements/list";
    }
}
