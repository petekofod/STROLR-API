package net.railwaynet.logdelivery.strolr;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.parameters.P;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class RailroadsService {

    private static final Logger logger = LoggerFactory.getLogger(RailroadsService.class);

    private static final String RAILROADS_FILE = "railroads.json";

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private static Map<String, ArrayList<Map<?, ?>>> allRailroads;

    static {
        try {
            allRailroads = objectMapper.readValue(new ClassPathResource(RAILROADS_FILE).getInputStream(),
                        new TypeReference<Map<String, ArrayList<Map<?, ?>>>>() {});
        } catch (IOException e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR, "Can't read the list of railroads!", e);
        }
    }

    private final static List<Map<?, ?>> scacs = allRailroads.get("SCACS");

    private Map<?, ?> getScacsByAccessScac(String accessScac) {
        Map<?, ?> result = null;

        for (Map<?, ?> accessScacItem: scacs) {
            if (accessScacItem.get("AccessSCAC").toString().equals(accessScac)) {
                result = accessScacItem;
                break;
            }
        }

        return result;
    }

    /*
    Need to first get AccessSCAC, then from that get the list of SCACs (labels), which in turn give list of options
     */
    public Map<?, ?> getRailroadsBySCAC(String accessScac) {

    	//This Map Stores the SCAC list result that gets returned
    	Map<?, ?> result = getScacsByAccessScac(accessScac);

        if (result == null) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR, "User role is not found!");
        }

        logger.debug("JSON:" + result.get("SCAC").toString());

        return result;
    }

    public String getSCACbyMARK(String accessScac, String mark) {
        logger.debug("Looking for SCAC of mark " + mark);
        Map<String, List<Map<String, ?>>> scacs = (Map<String, List<Map<String, ?>>>) getScacsByAccessScac(accessScac);
        if (scacs == null)
            return null;

        for (Map<String, ?> scac: scacs.get("SCAC")) {
            logger.debug("SCAC: " + scac.get("label"));
            List<String> scacOptions = (List<String>) scac.get("options");
            if (scacOptions.contains(mark)) {
                logger.debug("SCAC found!");
                return (String) scac.get("label");
            }
        }

        return null;
    }

}
