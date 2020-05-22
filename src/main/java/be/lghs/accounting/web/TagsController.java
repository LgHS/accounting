package be.lghs.accounting.web;

import be.lghs.accounting.repositories.MovementByTagsRepository;
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

        model.addAttribute("summary", repository.summary());

        return "app/tags/index";
    }
}
