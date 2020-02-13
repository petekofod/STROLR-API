package net.railwaynet.logdelivery.strolr;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
public class ListOfRailroadsController {

    private static final Logger logger = LoggerFactory.getLogger(ListOfRailroadsController.class);

    @RequestMapping("/data.json")
    public String data(Principal principal) {
        UserDetails currentUser = (UserDetails) ((Authentication) principal).getPrincipal();
        return "Hello " + currentUser.getUsername()+"!";
    }

}
