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

import java.sql.Timestamp;
import java.time.LocalDateTime;
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
            return new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date(millis));
        else
            return "Invalid";
    }

    private static void handleTime(List<Map<String, Object>> messages) {
        for (Map<String, Object> message : messages) {
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
        for (Map<String, Object> message : messages) {
            // 2083
            addCoordinates(message, "warningHeadEndPosition");
            addCoordinates(message, "enforcementHeadEndPosition");
            addCoordinates(message, "emergencyEnforcementHeadEndPosition");
            addCoordinates(message, "currentHeadEndPosition");
            // 2080
            addCoordinates(message, "headEndPosition");
        }
    }

    /**
     * Get the train ID from the last 2003 message for given train and time
     * @param messages2003  2003 messages collection
     * @param srcAddress    train
     * @param t             time
     * @param state         train state
     * @return              Train ID
     */
    private String getTrainIDFrom2003(List<Map<String, Object>> messages2003, String srcAddress, Integer t, String state) {
        if (state == null || !state.equals("CONTROLLING"))
            return "NA";

        int last2003Time = 0;
        String trainId = null;

        for (Map<String, Object> message : messages2003) {
            if (message.get("srcAddress").equals(srcAddress)) {
                int time2003 = (int) message.get("time");
                if (time2003 < t && time2003 > last2003Time) {
                    last2003Time = time2003;
                    trainId = (String) message.get("trainID");
                }
            }
        }

        if (trainId == null) {
            logger.warn("Can't find TrainID for " + srcAddress);
            return "";
        } else {
            return trainId;
        }
    }

    private List<Map<String, Object>> columns2080(List<Map<String, Object>> source, List<Map<String, Object>> messages2003) {
        List<Map<String, Object>> dest = new ArrayList<>();
        for (Map<String, Object> message : source) {
            Map<String, Object> destMessage = new LinkedHashMap<>();
            destMessage.put("idType", message.get("idType"));
            destMessage.put("timeUTC", message.get("timeUTC"));
            destMessage.put("srcAddress", message.get("srcAddress"));
            destMessage.put("destAddress", message.get("destAddress"));
            destMessage.put("scac", message.get("scac"));
            destMessage.put("trainID", getTrainIDFrom2003(messages2003, (String) message.get("srcAddress"), (Integer) message.get("time"), (String) message.get("locomotiveStateSummary")));
            destMessage.put("headEndMilepost", message.get("headEndMilepost"));
            destMessage.put("headEndTrackName", message.get("headEndTrackName"));
            destMessage.put("headEndScac", message.get("headEndScac"));
            destMessage.put("headEndSubdivDistrictId", message.get("headEndSubdivDistrictId"));
            destMessage.put("rearEndMilepost", message.get("rearEndMilepost"));
            destMessage.put("rearEndTrackName", message.get("rearEndTrackName"));
            destMessage.put("rearEndScac", message.get("rearEndScac"));
            destMessage.put("rearEndSubdivDistrictId", message.get("rearEndSubdivDistrictId"));
            destMessage.put("speed", message.get("speed"));
            destMessage.put("positionUncertainty", message.get("positionUncertainty"));
            destMessage.put("travelDirection", message.get("travelDirection"));
            destMessage.put("positionReportReason", message.get("positionReportReason"));
            destMessage.put("stateTimeUTC", message.get("stateTimeUTC"));
            destMessage.put("locomotiveStateSummary", message.get("locomotiveStateSummary"));
            destMessage.put("locomotiveState", message.get("locomotiveState"));
            destMessage.put("controlBrake", message.get("controlBrake"));
            dest.add(destMessage);
        }
        return dest;
    }

    private List<Map<String, Object>> columns2083(List<Map<String, Object>> source) {
        List<Map<String, Object>> dest = new ArrayList<>();
        for (Map<String, Object> message : source) {
            Map<String, Object> destMessage = new LinkedHashMap<>();
            destMessage.put("srcAddress", message.get("srcAddress"));
            destMessage.put("warningEnforcementType", message.get("warningEnforcementType"));
            destMessage.put("trainID", message.get("trainID"));
            destMessage.put("onboardSoftwareVersion", message.get("onboardSoftwareVersion"));
            destMessage.put("targetType", message.get("targetType"));
            destMessage.put("targetDescription", message.get("targetDescription"));
            destMessage.put("startTargetMilepost", message.get("startTargetMilepost"));
            destMessage.put("startTargetTrackName", message.get("startTargetTrackName"));
            destMessage.put("startTargetScac", message.get("startTargetScac"));
            destMessage.put("startTargetSubdivDistrictId", message.get("startTargetSubdivDistrictId"));
            destMessage.put("endTargetMilepost", message.get("endTargetMilepost"));
            destMessage.put("endTargetTrackName", message.get("endTargetTrackName"));
            destMessage.put("targetSpeed", message.get("targetSpeed"));
            destMessage.put("warningTimeUTC", message.get("warningTimeUTC"));
            destMessage.put("warningMilepost", message.get("warningMilepost"));
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
            destMessage.put("enforcementTimeUTC", message.get("enforcementTimeUTC"));
            destMessage.put("enforcementMilepost", message.get("enforcementMilepost"));
            destMessage.put("enforcementTrackName", message.get("enforcementTrackName"));
            destMessage.put("enforcementScac", message.get("enforcementScac"));
            destMessage.put("enforcementSubdivDistrictId", message.get("enforcementSubdivDistrictId"));
            destMessage.put("enforcementDistance", message.get("enforcementDistance"));
            destMessage.put("enforcementTravelDirection", message.get("enforcementTravelDirection"));
            destMessage.put("enforcementHeadEndPositionX", message.get("enforcementHeadEndPositionX"));
            destMessage.put("enforcementHeadEndPositionY", message.get("enforcementHeadEndPositionY"));
            destMessage.put("enforcementHeadEndPositionZ", message.get("enforcementHeadEndPositionZ"));
            destMessage.put("enforcementSpeed", message.get("enforcementSpeed"));
            destMessage.put("emergencyEnforcementTimeUTC", message.get("emergencyEnforcementTimeUTC"));
            destMessage.put("emergencyEnforcementMilepost", message.get("emergencyEnforcementMilepost"));
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
            destMessage.put("currentTimeUTC", message.get("currentTimeUTC"));
            destMessage.put("currentMilepost", message.get("currentMilepost"));
            destMessage.put("currentTrackName", message.get("currentTrackName"));
            destMessage.put("currentScac", message.get("currentScac"));
            destMessage.put("currentSubdivDistrictId", message.get("currentSubdivDistrictId"));
            destMessage.put("currentUncertainty", message.get("currentUncertainty"));
            destMessage.put("currentTravelDirection", message.get("currentTravelDirection"));
            destMessage.put("currentHeadEndPositionX", message.get("currentHeadEndPositionX"));
            destMessage.put("currentHeadEndPositionY", message.get("currentHeadEndPositionY"));
            destMessage.put("currentHeadEndPositionZ", message.get("currentHeadEndPositionZ"));
            dest.add(destMessage);
        }
        return dest;
    }

    public List<Map<String, Object>> getMessagesForCSV(Date startDate, Date endDate, String messageType) {

        List<Map<String, Object>> result = getMessages(startDate, endDate, messageType);
        handleTime(result);
        handleCoordinates(result);

        if (messageType.equals("2080")) {
            LocalDateTime dt = new Timestamp(startDate.getTime()).toLocalDateTime().minusHours(48);
            List<Map<String, Object>> messages2003 = getMessages2003(Timestamp.valueOf(dt), endDate);
            return columns2080(result, messages2003);
        }
        if (messageType.equals("2083"))
            return columns2083(result);

        return result;
    }

    public List<Map<String, Object>> getMessages(Date startDate, Date endDate, String messageType) {
        List<Map<String, Object>> result = new ArrayList<>();

        logger.debug("Start millis: " + startDate.getTime());
        logger.debug("End millis: " + endDate.getTime());
        logger.debug("messageType: " + messageType);

        int type = Integer.parseInt(messageType);

        List<Map<String, Object>> messages2003 = null;

        if (type == 0 || type == 2080) {
            logger.debug("Loading 2003 messages...");
            LocalDateTime dt = new Timestamp(startDate.getTime()).toLocalDateTime().minusHours(48);
            messages2003 = getMessages2003(Timestamp.valueOf(dt), endDate);
            logger.debug("Loaded " + messages2003.size() + " messages of type 2003");
        }

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

        logger.debug("Requesting data from MongoDB...");
        try (MongoCursor<Document> cursor = getMessagesCollection().find(filter)
                .iterator()) {
            while (cursor.hasNext()) {
                result.add(cursor.next());
            }
        }
        logger.debug("Loaded " + result.size() + " messages");

        if (messages2003 != null) {
            logger.debug("Updating 2080 messages with TrainID...");
            for (Map<String, Object> message : result)
                if (!message.containsKey("trainID"))
                    // 2080 doesn't contain train ID
                    message.put("trainID",
                            getTrainIDFrom2003(messages2003,
                                    (String) message.get("srcAddress"),
                                    (Integer) message.get("time"),
                                    (String) message.get("locomotiveStateSummary"))
                    );
        }

        return result;
    }

    public List<Map<String, Object>> getMessages2003(Date startDate, Date endDate) {
        List<Map<String, Object>> result = new ArrayList<>();

        logger.debug("Getting 2003 messages");
        logger.debug("Start millis: " + startDate.getTime());
        logger.debug("End millis: " + endDate.getTime());

        List<Bson> conditions = new ArrayList<>();
        conditions.add(gt("time", startDate.getTime() / 1000));
        conditions.add(lt("time", endDate.getTime() / 1000));

        conditions.add(eq("idType", 2003));
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
