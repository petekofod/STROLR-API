package net.railwaynet.logdelivery.strolr;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.ArrayList;
import java.util.Map;

@RestController
public class ListOfRailroadsController {

    private static final Logger logger = LoggerFactory.getLogger(ListOfRailroadsController.class);

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @RequestMapping("/railroads.json")
    public String data(Principal principal) {
        UserDetails currentUser = (UserDetails) ((Authentication) principal).getPrincipal();
        logger.debug(currentUser.getUsername() + " requesting the list of railroads");

        Map<String, ArrayList<Map<?, ?>>> railroadsForUser = RailroadsService.getRailroadsBySCAC(currentUser.getUsername());

        try {
            return objectMapper.writeValueAsString(railroadsForUser);
        } catch (JsonProcessingException e) {
            logger.error("Can't generate the list of railroads!");
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR, "Can't generate the list of railroads!", e);
        }
    }

}
