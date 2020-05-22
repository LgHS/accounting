package be.lghs.accounting.web;

import be.lghs.accounting.repositories.MovementByTagsRepository;
import be.lghs.accounting.repositories.utils.DateTrunc;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/tags")
@RequiredArgsConstructor
public class TagsController {

    private final MovementByTagsRepository repository;

    @GetMapping
    public String index(Model model) {
        var summaries = repository.summary(DateTrunc.DateTruncUnit.YEARS);

        model.addAttribute("yearlySummaries", summaries);

        return "app/tags/index";
    }
}
