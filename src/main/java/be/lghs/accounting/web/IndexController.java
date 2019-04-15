package be.lghs.accounting.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class IndexController {

    @GetMapping(value = "/", produces = "text/html")
    public String dashboard() {
        return "index";
    }
}
