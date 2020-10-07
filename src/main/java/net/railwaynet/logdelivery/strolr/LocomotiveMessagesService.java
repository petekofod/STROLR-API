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

    public List<Map<String, Object>> getMessages(Date startDate, Date endDate, String mark, String messageType) {
        List<Map<String, Object>> result = new ArrayList<>();

        MongoClientURI uri = new MongoClientURI("mongodb://reportReader:swRtP7yQav3^D@34.236.163.155:27017/?authSource=reports");
        MongoClient mongoClient = new MongoClient(uri);
        MongoDatabase reports = mongoClient.getDatabase("reports");
        MongoCollection<Document> messages = reports.getCollection("messages");

        logger.debug("Start millis: " + startDate.getTime());
        logger.debug("End millis: " + endDate.getTime());

        List<Bson> conditions = new ArrayList<>();
        conditions.add(gt("time", startDate.getTime() / 1000));
        conditions.add(lt("time", endDate.getTime() / 1000));
//        if (messageType == null) {
            conditions.add(or(
                    eq("idType", 2083),
                    eq("idType", 2080)
            ));
//        } else {
//            conditions.add(eq("idType", Integer.parseInt(messageType)));
//        }
//        conditions.add(regex("srcAddress", mark + "\\..*"));
        Bson filter = and(conditions);

        try (MongoCursor<Document> cursor = messages.find(filter)
                .iterator()) {
            while (cursor.hasNext()) {
                System.out.print(".");
                result.add(cursor.next());
            }
        }

        return result;
    }
}
