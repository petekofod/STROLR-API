package net.railwaynet.logdelivery.strolr;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;

import java.security.Principal;

@RestController
public class WelcomeController {

    private static final Logger logger = LoggerFactory.getLogger(WelcomeController.class);

    @PreAuthorize("hasAuthority('ROLE_USER')")
    @RequestMapping("/user_test")
    public String userTest(Principal principal) {
        UserDetails currentUser
                = (UserDetails) ((Authentication) principal).getPrincipal();
        return "Hello " + currentUser.getUsername()+"!";
    }

}
