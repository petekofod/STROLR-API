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

    public List<Map<String, Object>> getMessagesForCSV(Date startDate, Date endDate, String mark, String messageType) {

        List<Map<String, Object>> result = getMessages(startDate, endDate, mark, messageType);
        handleTime(result);
        handleCoordinates(result);

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
//        conditions.add(regex("srcAddress", "/" + mark.toLowerCase() + "\\..*/"));
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
