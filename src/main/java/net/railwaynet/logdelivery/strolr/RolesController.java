package net.railwaynet.logdelivery.strolr;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.keycloak.KeycloakPrincipal;
import org.keycloak.KeycloakSecurityContext;
import org.keycloak.adapters.springsecurity.token.KeycloakAuthenticationToken;
import org.keycloak.representations.IDToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Array;
import java.security.Principal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
public class RolesController {

    private static final Logger logger = LoggerFactory.getLogger(RolesController.class);

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @RequestMapping("/roles.json")
    @CrossOrigin(origins = "http://localhost:3000")
    public String roles(Principal principal) {

        logger.debug("Username: " + principal.getName());

        List<Map<String, String>> userRoles = new ArrayList<>();

        for (GrantedAuthority role: ((KeycloakAuthenticationToken) principal).getAuthorities()) {
            Map<String, String> newRole = new HashMap<>();
            newRole.put("role", role.getAuthority());
            userRoles.add(newRole);
        }

        try {
            String result = objectMapper.writeValueAsString(userRoles);
            logger.debug("User roles is: " + result);
            return result;
        } catch (JsonProcessingException e) {
            logger.error("Can't get roles for the user!");
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR, "Can't get roles for the user!", e);
        }
    }

    @RequestMapping(value= "/**", method= RequestMethod.OPTIONS)
    public void corsHeaders(HttpServletResponse response) {
        logger.debug("Handling CORS preflight request");
        response.addHeader("Access-Control-Allow-Origin", "*");
        response.addHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        response.addHeader("Access-Control-Allow-Headers", "origin, content-type, accept, x-requested-with");
        response.addHeader("Access-Control-Max-Age", "3600");
    }

}

