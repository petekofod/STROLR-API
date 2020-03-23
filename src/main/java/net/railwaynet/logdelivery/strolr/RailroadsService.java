package net.railwaynet.logdelivery.strolr;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    /*
    Need to first get AccessSCAC, then from that get the list of SCACs (labels), which in turn give list of options
     */
    public static Map<?, ?> getRailroadsBySCAC(String accessScac) {
    	//This Map Stores the SCAC list result that gets returned
    	Map<?, ?> result = null;

        for (Map<?, ?> accessScacItem: scacs) {
            if (accessScacItem.get("AccessSCAC").toString().equals(accessScac)) {
                result = accessScacItem;
                break;
            }
        }

        if (result == null) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR, "User role is not found!");
        }

        logger.debug("JSON:" + result.get("SCAC").toString());

        return result;
    }

}
