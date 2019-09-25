package be.lghs.accounting.web.app;

import be.lghs.accounting.configuration.Roles;
import be.lghs.accounting.services.GraphService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;

@RequiredArgsConstructor
@RequestMapping("/users")
public class UsersController {

    private final GraphService graphService;

    @GetMapping("/{user_id}/subscriptions")
    public String userSubscriptions(@PathVariable("user_id") UUID userId) {
        throw new UnsupportedOperationException("NYI");
    }

    @GetMapping(value = "/{user_id}/subscriptions/graph", produces = "image/svg+xml")
    @Secured(Roles.ROLE_MEMBER)
    public void graph(@PathVariable("user_id") UUID userId,
                      HttpServletResponse response) throws IOException {
        response.setContentType("image/svg+xml");
        try (ServletOutputStream output = response.getOutputStream()) {
            graphService.generateSubscriptionGraph(userId, output);
        }
    }
}
