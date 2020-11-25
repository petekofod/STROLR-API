package net.railwaynet.logdelivery.strolr;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

@RestController
public class UsersController {

    private static final Logger logger = LoggerFactory.getLogger(UsersController.class);

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @RequestMapping("/user.json")
    @CrossOrigin
    public String data(Principal principal) {
        UserDetails currentUser = (UserDetails) ((Authentication) principal).getPrincipal();

        String userName;

        if (currentUser == null) {
            logger.error("Can't find the user name!");
            // TODO: KeyCloak
            userName = "AMTK";
            /*
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR, "User name is NULL!");

             */
        } else {
            userName = currentUser.getUsername();
        }

        logger.debug("User name is " + userName);

        Map<String, String> result = new HashMap<>();
        result.put("userName", userName);

        try {
            return objectMapper.writeValueAsString(result);
        } catch (JsonProcessingException e) {
            logger.error("Can't generate user name JSON file!");
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR, "Can't generate user name JSON file!", e);
        }
    }

}
