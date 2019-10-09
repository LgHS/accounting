package be.lghs.accounting.web.app;

import be.lghs.accounting.configuration.Roles;
import be.lghs.accounting.model.tables.records.AccountsRecord;
import be.lghs.accounting.model.tables.records.CodasRecord;
import be.lghs.accounting.repositories.AccountRepository;
import be.lghs.accounting.repositories.CodaRepository;
import be.lghs.accounting.services.CodasService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.Result;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Slf4j
@Controller
@RequestMapping("/app/codas")
@RequiredArgsConstructor
public class CodasController {

    private final CodaRepository codaRepository;
    private final AccountRepository accountRepository;
    private final CodasService codasService;

    @GetMapping
    @Transactional(readOnly = true)
    @Secured(Roles.ROLE_ADMIN)
    public String codas(Model model) {
        Result<CodasRecord> codas = codaRepository.findAll();
        model.addAttribute("codas", codas);
        return "app/codas/list";
    }

    @GetMapping("/new")
    @Transactional(readOnly = true)
    @Secured(Roles.ROLE_ADMIN)
    public String codaForm(Model model) {
        Result<CodasRecord> codas = codaRepository.findAll();
        Result<AccountsRecord> accounts = accountRepository.findAll();
        model.addAttribute("codas", codas);
        model.addAttribute("accounts", accounts);
        return "app/codas/form";
    }

    @PostMapping("/new")
    @Secured(Roles.ROLE_ADMIN)
    public String createCoda(@RequestParam("account_id") UUID accountId,
                             @RequestParam("codas") List<MultipartFile> files) {
        files.sort(Comparator.comparing(MultipartFile::getOriginalFilename));

        for (MultipartFile file : files) {
            try (InputStream inputStream = file.getInputStream()) {
                codasService.handleCodaUpload(accountId, file.getOriginalFilename(), inputStream);
            } catch (IOException e) {
                // FIXME Report what went wrong without failing the whole batch
                log.error("error importing {}", file.getOriginalFilename(), e);
            }
        }

        return "redirect:/app/codas";
    }
}
