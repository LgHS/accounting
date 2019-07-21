package be.lghs.accounting.web.app;

import be.lghs.accounting.model.tables.records.AccountsRecord;
import be.lghs.accounting.repositories.AccountRepository;
import be.lghs.accounting.repositories.MovementRepository;
import lombok.RequiredArgsConstructor;
import org.jooq.Result;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Controller
@RequestMapping("/app/accounts")
@RequiredArgsConstructor
public class AccountsController {

    private final AccountRepository accountRepository;
    private final MovementRepository movementRepository;

    @GetMapping
    @Transactional(readOnly = true)
    public String accounts(Model model) {
        Result<AccountsRecord> accounts = accountRepository.findAll();
        model.addAttribute("accounts", accounts);
        return "app/accounts/list";
    }

    @GetMapping("/new")
    public String accountForm() {
        return "app/accounts/form";
    }

    @PostMapping({"/new", "/{id}"})
    @Transactional
    public String createAccount(@PathVariable(value = "id", required = false) UUID accountId,
                                @RequestParam("name") String name,
                                @RequestParam("description") String description) {
        if (accountId == null) {
            accountRepository.createOne(name, description);
        } else {
            accountRepository.update(accountId, name, description);
        }
        return "redirect:/app/accounts";
    }

    @GetMapping("/{id}")
    @Transactional
    public String accountForm(@PathVariable("id") UUID id, Model model) {
        AccountsRecord account = accountRepository.findOne(id)
                .orElseThrow(() -> new EmptyResultDataAccessException(1));
        model.addAttribute("account", account);
        return "app/accounts/form";
    }

    @GetMapping("/{account_id}/movements")
    @Transactional(readOnly = true)
    public String movementsByAccount(@PathVariable("account_id") UUID accountId,
                                     Model model) {
        var movements = movementRepository.findByAccount(accountId);
        var categories = movementRepository.categories();
        model.addAttribute("movements", movements);
        model.addAttribute("categories", categories);
        return "app/movements/list";
    }
}
