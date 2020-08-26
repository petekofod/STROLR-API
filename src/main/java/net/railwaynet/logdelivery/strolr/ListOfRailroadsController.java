package net.railwaynet.logdelivery.strolr;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.sql.SQLException;
import java.util.Map;
import java.util.Objects;

@RestController
public class ListOfRailroadsController {

    private static final Logger logger = LoggerFactory.getLogger(ListOfRailroadsController.class);

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private RailroadsService railroadsService;

    @Autowired
    private ListOfLocomotivesService locomotivesService;

    @Autowired
    private Environment env;

    @RequestMapping("/railroads.json")
    public String railroads(Principal principal) {

        if (env == null) {
            logger.error("Can't access application.properties, skipping the request");
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR, "Can't access application.properties, skipping the request");
        }

        UserDetails currentUser = (UserDetails) ((Authentication) principal).getPrincipal();
        logger.debug(currentUser.getUsername() + " requesting the list of railroads");

        @SuppressWarnings("unchecked") Map<String, Object> userConfiguration =
                (Map<String, Object>) railroadsService.getRailroadsBySCAC(currentUser.getUsername());
        userConfiguration.put("S3BaseURL", Objects.requireNonNull(env.getProperty("S3.base.URL")));

        try {
            String result = objectMapper.writeValueAsString(userConfiguration);
            logger.debug("User configuration is: " + result);
            return result;
        } catch (JsonProcessingException e) {
            logger.error("Can't generate the configuration for the user!");
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR, "Can't generate the configuration for the user!", e);
        }
    }

    @RequestMapping("/locomotives.json")
    public String locomotives(Principal principal) {
        UserDetails currentUser = (UserDetails) ((Authentication) principal).getPrincipal();
        logger.debug(currentUser.getUsername() + " requesting the list of locomotives");

        try {
            Map<String, Object> result = locomotivesService.getLocomotives();
            return objectMapper.writeValueAsString(result);
        } catch (SQLException e) {
            logger.error("Can't get locomotives from RDS!", e);
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR, "Can't get locomotives from RDS!", e);
        } catch (JsonProcessingException e) {
            logger.error("Can't generate JSON with a list of locomotives!");
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR, "Can't generate JSON with a list of locomotives!", e);
        }


    }

}
