package be.lghs.accounting.web;

import be.lghs.accounting.configuration.Roles;
import be.lghs.accounting.model.enums.SubscriptionType;
import be.lghs.accounting.repositories.SubscriptionRepository;
import be.lghs.accounting.services.GraphService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/subscriptions")
@RequiredArgsConstructor
public class SubscriptionsController {

    private final SubscriptionRepository subscriptionRepository;
    private final GraphService graphService;

    @GetMapping
    @Transactional(readOnly = true)
    @Secured(Roles.ROLE_ADMIN)
    public String subscriptions(@RequestParam(value = "type", required = false) SubscriptionType type,
                                Model model) {
        var subscriptions = subscriptionRepository.findAll(type);

        model.addAttribute("selectedType", type);
        model.addAttribute("subscriptions", subscriptions);

        return "app/subscriptions/list";
    }

    // @PostMapping({"/new", "/{id}"})
    // @Transactional
    // @Secured(Roles.ROLE_TREASURER)
    // public String createSubscription(@PathVariable(value = "id", required = false) UUID subscriptionId,
    //                                  @RequestParam("name") String name,
    //                                  @RequestParam("description") String description) {
    //     if (subscriptionId == null) {
    //         subscriptionRepository.createOne(name, description);
    //     } else {
    //         subscriptionRepository.update(subscriptionId, name, description);
    //     }
    //     return "redirect:/subscriptions";
    // }
    //
    // @GetMapping("/{id}")
    // @Transactional
    // @Secured(Roles.ROLE_TREASURER)
    // public String subscriptionsForm(@PathVariable("id") UUID id, Model model) {
    //     SubscriptionsRecord subscriptions = subscriptionRepository.findOne(id)
    //             .orElseThrow(() -> new EmptyResultDataAccessException(1));
    //     model.addAttribute("subscriptions", subscriptions);
    //     return "app/subscriptions/form";
    // }
}
