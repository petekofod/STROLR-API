package net.railwaynet.logdelivery.strolr;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.keycloak.adapters.springsecurity.token.KeycloakAuthenticationToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.sql.SQLException;
import java.util.*;

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
    @CrossOrigin
    public String railroads(Principal principal) {

        @SuppressWarnings("unchecked") Map<String, Object> userConfiguration =
                (Map<String, Object>) railroadsService.getRailroadsBySCAC("RCAX");
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
    @CrossOrigin
    public String locomotives(Principal principal) {

        List<String> scacs = new ArrayList<>();

        for (GrantedAuthority role: ((KeycloakAuthenticationToken) principal).getAuthorities()) {
            if (role.getAuthority().endsWith(".locomotive.status.reader"))
                scacs.add("'" + role.getAuthority().substring(5, 9).toUpperCase() + "'");
        }

        try {
            Map<String, Object> result = locomotivesService.getLocomotives(scacs);
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

    @RequestMapping("/locomotive-update.json/{mark}/{locoID}")
    @CrossOrigin
    public String locomotivesUpdates(Principal principal,
                                     @PathVariable("mark") String mark,
                                     @PathVariable("locoID") String locoID) {

        if (mark.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_ACCEPTABLE, "Please specify the mark");
        }

        if (locoID.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_ACCEPTABLE, "Please specify the locoID");
        }

        List<String> scacs = new ArrayList<>();

        for (GrantedAuthority role: ((KeycloakAuthenticationToken) principal).getAuthorities()) {
            if (role.getAuthority().endsWith(".locomotive.status.reader"))
                scacs.add("'" + role.getAuthority().substring(5, 9).toUpperCase() + "'");
        }

        try {
            Map<String, Object> result = locomotivesService.getLast10Updates(mark, locoID, scacs);
            return objectMapper.writeValueAsString(result);
        } catch (SQLException e) {
            logger.error("Can't get updates from RDS!", e);
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR, "Can't get updates from RDS!", e);
        } catch (JsonProcessingException e) {
            logger.error("Can't generate JSON with a list of updates!");
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR, "Can't generate JSON with a list of updates!", e);
        }
    }

}
