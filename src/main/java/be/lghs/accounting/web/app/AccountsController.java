package be.lghs.accounting.web.app;

import be.lghs.accounting.model.tables.records.AccountsRecord;
import be.lghs.accounting.repositories.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.jooq.Result;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

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
        return "app/accounts";
    }

    @GetMapping("/new")
    public String accountForm(Model model) {
        Result<AccountsRecord> accounts = accountRepository.findAll();
        model.addAttribute("accounts", accounts);
        return "app/accounts/form";
    }

    @GetMapping("/{id}")
    public String accountForm(@PathVariable("id") UUID id, Model model) {
        Result<AccountsRecord> accounts = accountRepository.findAll();
        model.addAttribute("accounts", accounts);
        return "app/accounts/form";
    }
}
