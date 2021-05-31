package be.lghs.accounting.web;

import be.lghs.accounting.repositories.UserAccountNumberRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
@RequestMapping("/users/account-numbers")
public class UserAccountNumberController {

    private final UserAccountNumberRepository userAccountNumberRepository;

    @GetMapping
    @Transactional(readOnly = true)
    public String listWaitingValidation(Model model) {
        model.addAttribute("accountNumbers", userAccountNumberRepository.listWaitingValidation());
        return "app/users/account-numbers/validation";
    }

    @Data
    public static class AccountNumberForm {
        private String accountNumber;
        private String selected;
    }

    @Data
    public static class AccountNumberFormList {
        private List<AccountNumberForm> numbers;
    }

    @PostMapping
    @Transactional
    public String updateList(@ModelAttribute AccountNumberFormList form,
                             Model model) {
        var numbers = form.numbers.stream()
                .filter(f -> "on".equals(f.selected))
                .map(AccountNumberForm::getAccountNumber)
                .collect(Collectors.toList());
        userAccountNumberRepository.validate(numbers);

        return listWaitingValidation(model);
    }

}
