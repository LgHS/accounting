package be.lghs.accounting.web.app;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/app")
public class AppController {

    @GetMapping(value = "/", produces = "text/html")
    public String dashboard() {
        return "app/dashboard";
    }
}
