package net.railwaynet.logdelivery.strolr;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
public class ListOfRailroadsController {

    private static final Logger logger = LoggerFactory.getLogger(ListOfRailroadsController.class);

    private static final String RAILROADS_FILE = "railroads.json";

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @RequestMapping("/railroads.json")
    public String data(Principal principal) {
        UserDetails currentUser = (UserDetails) ((Authentication) principal).getPrincipal();
        logger.debug(currentUser.getUsername() + " requesting the list of railroads");

        Map<String, ArrayList<Map<?, ?>>> result = new HashMap<>();
        result.put("SCAC", new ArrayList<>());

        try {
            Map<String, ArrayList<Map<?, ?>>> map = objectMapper.readValue(new ClassPathResource(RAILROADS_FILE).getInputStream(),
                    new TypeReference<Map<String, ArrayList<Map<?, ?>>>>() {});
            List<Map<?, ?>> scacs = map.get("SCAC");
            for (Map<?, ?> scac: scacs) {
                if (scac.get("label").toString().equals(currentUser.getUsername())) {
                    result.get("SCAC").add(scac);
                    break;
                }
            }

            logger.debug("JSON:" + result.get("SCAC").toString());
        } catch (IOException e) {
            logger.error("Can't read the list of railroads!", e);
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR, "Can't read the list of railroads!", e);
        }
        try {
            return objectMapper.writeValueAsString(result);
        } catch (JsonProcessingException e) {
            logger.error("Can't generate the list of railroads!");
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR, "Can't generate the list of railroads!", e);
        }
    }

}
