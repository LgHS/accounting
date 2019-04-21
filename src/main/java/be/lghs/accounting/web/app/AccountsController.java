package be.lghs.accounting.web.app;

import be.lghs.accounting.model.tables.records.AccountsRecord;
import be.lghs.accounting.repositories.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.jooq.Result;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Controller
@RequestMapping("/app/accounts")
@RequiredArgsConstructor
public class AccountsController {

    private final AccountRepository accountRepository;

    @GetMapping
    public String accounts(Model model) {
        Result<AccountsRecord> accounts = accountRepository.findAll();
        model.addAttribute("accounts", accounts);
        return "app/accounts/list";
    }

    @GetMapping("/new")
    public String accountForm(Model model) {
        Result<AccountsRecord> accounts = accountRepository.findAll();
        model.addAttribute("accounts", accounts);
        return "app/accounts/form";
    }

    @PostMapping("/new")
    public String createAccount(@RequestParam("name") String name,
                                @RequestParam("description") String description) {
        accountRepository.createOne(name, description);
        return "redirect:/app/accounts";
    }

    @GetMapping("/{id}")
    public String accountForm(@PathVariable("id") UUID id, Model model) {
        Result<AccountsRecord> accounts = accountRepository.findAll();
        model.addAttribute("accounts", accounts);
        return "app/accounts/form";
    }
}
