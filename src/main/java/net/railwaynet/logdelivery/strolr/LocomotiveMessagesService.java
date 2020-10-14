package net.railwaynet.logdelivery.strolr;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.util.*;

import static com.mongodb.client.model.Filters.*;

@Service
public class LocomotiveMessagesService {
    private static final Logger logger = LoggerFactory.getLogger(LocomotiveMessagesService.class);

    @Autowired
    private Environment env;

    private MongoCollection<Document> messages;

    private void init() {
        if (!env.containsProperty("messages.mongo.url")) {
            logger.error("messages.mongo.url is not defined in application properties!");
        }
        MongoClientURI uri = new MongoClientURI(Objects.requireNonNull(env.getProperty("messages.mongo.url")));

        MongoClient mongoClient = new MongoClient(uri);

        if (!env.containsProperty("messages.mongo.database")) {
            logger.error("messages.mongo.database is not defined in application properties!");
        }
        MongoDatabase reports = mongoClient.getDatabase(Objects.requireNonNull(env.getProperty("messages.mongo.database")));

        if (!env.containsProperty("messages.mongo.collection")) {
            logger.error("messages.mongo.collection is not defined in application properties!");
        }
        messages = reports.getCollection(Objects.requireNonNull(env.getProperty("messages.mongo.collection")));
    }

    private MongoCollection<Document> getMessagesCollection() {
        if (messages == null)
            init();
        return messages;
    }

    private static String EpochToString(Long millis) {
        if (millis > 0)
            return new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date (millis));
        else
            return "Invalid";
    }

    private static void handleTime(List<Map<String, Object>> messages) {
        for (Map<String, Object> message: messages) {
            // common
            if (message.containsKey("time")) {
                message.put("timeUTC", EpochToString((Integer.toUnsignedLong((Integer) message.get("time")) * 1000)));
            }
            // 2083
            if (message.containsKey("warningTime")) {
                message.put("warningTimeUTC", EpochToString((Long) message.get("warningTime")));
            }
            if (message.containsKey("enforcementTime")) {
                message.put("enforcementTimeUTC", EpochToString((Long) message.get("enforcementTime")));
            }
            if (message.containsKey("emergencyEnforcementTime")) {
                message.put("emergencyEnforcementTimeUTC", EpochToString((Long) message.get("emergencyEnforcementTime")));
            }
            if (message.containsKey("currentTime")) {
                message.put("currentTimeUTC", EpochToString((Long) message.get("currentTime")));
            }
            // 2080
            if (message.containsKey("stateTime")) {
                message.put("stateTimeUTC", EpochToString((Long) message.get("stateTime")));
            }
        }
    }

    private static void addCoordinates(Map<String, Object> message, String key) {
        if (message.containsKey(key)) {
            @SuppressWarnings("unchecked") Map<String, Long> coordinates = (Map<String, Long>) message.remove(key);
            message.put(key + "X", coordinates.get("x"));
            message.put(key + "Y", coordinates.get("y"));
            message.put(key + "Z", coordinates.get("z"));
        }
    }

    private static void handleCoordinates(List<Map<String, Object>> messages) {
        for (Map<String, Object> message: messages) {
            // 2083
            addCoordinates(message, "warningHeadEndPosition");
            addCoordinates(message, "enforcementHeadEndPosition");
            addCoordinates(message, "emergencyEnforcementHeadEndPosition");
            addCoordinates(message, "currentHeadEndPosition");
            // 2080
            addCoordinates(message, "headEndPosition");
        }
    }

    private void copyHeader(Map<String, Object> message, Map<String, Object> destMessage) {
        destMessage.put("_id", message.get("_id"));
        destMessage.put("idType", message.get("idType"));
        destMessage.put("version", message.get("version"));
        destMessage.put("dataLength", message.get("dataLength"));
        destMessage.put("number", message.get("number"));
        destMessage.put("time", message.get("time"));
        destMessage.put("timeUTC", message.get("timeUTC"));
        destMessage.put("timeToLive", message.get("timeToLive"));
        destMessage.put("msgClass", message.get("msgClass"));
        destMessage.put("priority", message.get("priority"));
        destMessage.put("networkPreference", message.get("networkPreference"));
        destMessage.put("specialHandling", message.get("specialHandling"));
        destMessage.put("serviceRequest", message.get("serviceRequest"));
        destMessage.put("srcAddress", message.get("srcAddress"));
        destMessage.put("destAddress", message.get("destAddress"));
        destMessage.put("crc", message.get("crc"));
        destMessage.put("scac", message.get("scac"));
    }

    private List<Map<String, Object>> columns2080(List<Map<String, Object>> source) {
        List<Map<String, Object>> dest = new ArrayList<>();
        for (Map<String, Object> message: source) {
            Map<String, Object> destMessage = new LinkedHashMap<>();
            copyHeader(message, destMessage);
            destMessage.put("ptcAuthorityReferenceNumber", message.get("ptcAuthorityReferenceNumber"));
            destMessage.put("headEndMilepost", message.get("headEndMilepost"));
            destMessage.put("headEndMilepostPrefix", message.get("headEndMilepostPrefix"));
            destMessage.put("headEndMilepostSuffix", message.get("headEndMilepostSuffix"));
            destMessage.put("headEndTrackName", message.get("headEndTrackName"));
            destMessage.put("headEndScac", message.get("headEndScac"));
            destMessage.put("headEndSubdivDistrictId", message.get("headEndSubdivDistrictId"));
            destMessage.put("rearEndMilepost", message.get("rearEndMilepost"));
            destMessage.put("rearEndMilepostPrefix", message.get("rearEndMilepostPrefix"));
            destMessage.put("rearEndMilepostSuffix", message.get("rearEndMilepostSuffix"));
            destMessage.put("rearEndTrackName", message.get("rearEndTrackName"));
            destMessage.put("rearEndScac", message.get("rearEndScac"));
            destMessage.put("rearEndSubdivDistrictId", message.get("rearEndSubdivDistrictId"));
            destMessage.put("speed", message.get("speed"));
            destMessage.put("positionUncertainty", message.get("positionUncertainty"));
            destMessage.put("travelDirection", message.get("travelDirection"));
            destMessage.put("headEndPositionX", message.get("headEndPositionX"));
            destMessage.put("headEndPositionY", message.get("headEndPositionY"));
            destMessage.put("headEndPositionZ", message.get("headEndPositionZ"));
            destMessage.put("positionValidity", message.get("positionValidity"));
            destMessage.put("positionReportReason", message.get("positionReportReason"));
            destMessage.put("stateTime", message.get("stateTime"));
            destMessage.put("stateTimeUTC", message.get("stateTimeUTC"));
            destMessage.put("locomotiveStateSummary", message.get("locomotiveStateSummary"));
            destMessage.put("locomotiveState", message.get("locomotiveState"));
            destMessage.put("controlBrake", message.get("controlBrake"));
            destMessage.put("timeElapsed", message.get("timeElapsed"));
            destMessage.put("distanceElapsed", message.get("distanceElapsed"));
            destMessage.put("dataIntegrity", message.get("dataIntegrity"));
            dest.add(destMessage);
        }
        return dest;
    }

    private List<Map<String, Object>> columns2083(List<Map<String, Object>> source) {
        List<Map<String, Object>> dest = new ArrayList<>();
        for (Map<String, Object> message: source) {
            Map<String, Object> destMessage = new LinkedHashMap<>();
            copyHeader(message, destMessage);
            destMessage.put("warningEnforcementType", message.get("warningEnforcementType"));
            destMessage.put("trainID", message.get("trainID"));
            destMessage.put("onboardSoftwareVersion", message.get("onboardSoftwareVersion"));
            destMessage.put("targetType", message.get("targetType"));
            destMessage.put("targetDescription", message.get("targetDescription"));
            destMessage.put("startTargetMilepost", message.get("startTargetMilepost"));
            destMessage.put("startTargetMilepostPrefix", message.get("startTargetMilepostPrefix"));
            destMessage.put("startTargetMilepostSuffix", message.get("startTargetMilepostSuffix"));
            destMessage.put("startTargetTrackName", message.get("startTargetTrackName"));
            destMessage.put("startTargetScac", message.get("startTargetScac"));
            destMessage.put("startTargetSubdivDistrictId", message.get("startTargetSubdivDistrictId"));
            destMessage.put("endTargetMilepost", message.get("endTargetMilepost"));
            destMessage.put("endTargetMilepostPrefix", message.get("endTargetMilepostPrefix"));
            destMessage.put("endTargetMilepostSuffix", message.get("endTargetMilepostSuffix"));
            destMessage.put("endTargetTrackName", message.get("endTargetTrackName"));
            destMessage.put("endTargetScac", message.get("endTargetScac"));
            destMessage.put("endTargetSubdivDistrictId", message.get("endTargetSubdivDistrictId"));
            destMessage.put("targetSpeed", message.get("targetSpeed"));
            destMessage.put("warningTime", message.get("warningTime"));
            destMessage.put("warningTimeUTC", message.get("warningTimeUTC"));
            destMessage.put("warningMilepost", message.get("warningMilepost"));
            destMessage.put("warningMilepostPrefix", message.get("warningMilepostPrefix"));
            destMessage.put("warningMilepostSuffix", message.get("warningMilepostSuffix"));
            destMessage.put("warningTrackName", message.get("warningTrackName"));
            destMessage.put("warningScac", message.get("warningScac"));
            destMessage.put("warningSubdivDistrictId", message.get("warningSubdivDistrictId"));
            destMessage.put("warningUncertainty", message.get("warningUncertainty"));
            destMessage.put("warningDistance", message.get("warningDistance"));
            destMessage.put("warningTravelDirection", message.get("warningTravelDirection"));
            destMessage.put("warningHeadEndPositionX", message.get("warningHeadEndPositionX"));
            destMessage.put("warningHeadEndPositionY", message.get("warningHeadEndPositionY"));
            destMessage.put("warningHeadEndPositionZ", message.get("warningHeadEndPositionZ"));
            destMessage.put("warningSpeed", message.get("warningSpeed"));
            destMessage.put("enforcementTime", message.get("enforcementTime"));
            destMessage.put("enforcementTimeUTC", message.get("enforcementTimeUTC"));
            destMessage.put("enforcementMilepost", message.get("enforcementMilepost"));
            destMessage.put("enforcementMilepostPrefix", message.get("enforcementMilepostPrefix"));
            destMessage.put("enforcementMilepostSuffix", message.get("enforcementMilepostSuffix"));
            destMessage.put("enforcementTrackName", message.get("enforcementTrackName"));
            destMessage.put("enforcementScac", message.get("enforcementScac"));
            destMessage.put("enforcementSubdivDistrictId", message.get("enforcementSubdivDistrictId"));
            destMessage.put("enforcementDistance", message.get("enforcementDistance"));
            destMessage.put("enforcementTravelDirection", message.get("enforcementTravelDirection"));
            destMessage.put("enforcementHeadEndPositionX", message.get("enforcementHeadEndPositionX"));
            destMessage.put("enforcementHeadEndPositionY", message.get("enforcementHeadEndPositionY"));
            destMessage.put("enforcementHeadEndPositionZ", message.get("enforcementHeadEndPositionZ"));
            destMessage.put("enforcementSpeed", message.get("enforcementSpeed"));
            destMessage.put("emergencyEnforcementTime", message.get("emergencyEnforcementTime"));
            destMessage.put("emergencyEnforcementTimeUTC", message.get("emergencyEnforcementTimeUTC"));
            destMessage.put("emergencyEnforcementMilepost", message.get("emergencyEnforcementMilepost"));
            destMessage.put("emergencyEnforcementMilepostPrefix", message.get("emergencyEnforcementMilepostPrefix"));
            destMessage.put("emergencyEnforcementMilepostSuffix", message.get("emergencyEnforcementMilepostSuffix"));
            destMessage.put("emergencyEnforcementTrackName", message.get("emergencyEnforcementTrackName"));
            destMessage.put("emergencyEnforcementScac", message.get("emergencyEnforcementScac"));
            destMessage.put("emergencyEnforcementSubdivDistrictId", message.get("emergencyEnforcementSubdivDistrictId"));
            destMessage.put("emergencyEnforcementUncertainty", message.get("emergencyEnforcementUncertainty"));
            destMessage.put("emergencyEnforcementDistance", message.get("emergencyEnforcementDistance"));
            destMessage.put("emergencyEnforcementTravelDirection", message.get("emergencyEnforcementTravelDirection"));
            destMessage.put("emergencyEnforcementHeadEndPositionX", message.get("emergencyEnforcementHeadEndPositionX"));
            destMessage.put("emergencyEnforcementHeadEndPositionY", message.get("emergencyEnforcementHeadEndPositionY"));
            destMessage.put("emergencyEnforcementHeadEndPositionZ", message.get("emergencyEnforcementHeadEndPositionZ"));
            destMessage.put("emergencyEnforcementSpeed", message.get("emergencyEnforcementSpeed"));
            destMessage.put("currentTime", message.get("currentTime"));
            destMessage.put("currentTimeUTC", message.get("currentTimeUTC"));
            destMessage.put("currentMilepost", message.get("currentMilepost"));
            destMessage.put("currentMilepostPrefix", message.get("currentMilepostPrefix"));
            destMessage.put("currentMilepostSuffix", message.get("currentMilepostSuffix"));
            destMessage.put("currentTrackName", message.get("currentTrackName"));
            destMessage.put("currentScac", message.get("currentScac"));
            destMessage.put("currentSubdivDistrictId", message.get("currentSubdivDistrictId"));
            destMessage.put("currentUncertainty", message.get("currentUncertainty"));
            destMessage.put("currentTravelDirection", message.get("currentTravelDirection"));
            destMessage.put("currentHeadEndPositionX", message.get("currentHeadEndPositionX"));
            destMessage.put("currentHeadEndPositionY", message.get("currentHeadEndPositionY"));
            destMessage.put("currentHeadEndPositionZ", message.get("currentHeadEndPositionZ"));
            destMessage.put("currentSpeed", message.get("currentSpeed"));
            destMessage.put("dataIntegrity", message.get("dataIntegrity"));
            dest.add(destMessage);
        }
        return dest;
    }

    public List<Map<String, Object>> getMessagesForCSV(Date startDate, Date endDate, String mark, String messageType) {

        List<Map<String, Object>> result = getMessages(startDate, endDate, mark, messageType);
        handleTime(result);
        handleCoordinates(result);

        if (messageType.equals("2080"))
            return columns2080(result);
        if (messageType.equals("2083"))
            return columns2083(result);

        return result;
    }

    public List<Map<String, Object>> getMessages(Date startDate, Date endDate, String mark, String messageType) {
        List<Map<String, Object>> result = new ArrayList<>();

        logger.debug("Start millis: " + startDate.getTime());
        logger.debug("End millis: " + endDate.getTime());
        logger.debug("messageType: " + messageType);
        logger.debug("mark = " + mark);

        List<Bson> conditions = new ArrayList<>();
        conditions.add(gt("time", startDate.getTime() / 1000));
        conditions.add(lt("time", endDate.getTime() / 1000));

        if (Integer.parseInt(messageType) == 0) {
            conditions.add(or(
                    eq("idType", 2083),
                    eq("idType", 2080)
            ));
        } else {
            conditions.add(eq("idType", Integer.parseInt(messageType)));
        }
        conditions.add(in("destAddress", "amtk.b:gb.nec", "amtk.b:gb.me"));
        Bson filter = and(conditions);

        try (MongoCursor<Document> cursor = getMessagesCollection().find(filter)
                .iterator()) {
            while (cursor.hasNext()) {
                result.add(cursor.next());
            }
        }

        return result;
    }
}
