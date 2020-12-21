package net.railwaynet.logdelivery.strolr;

import java.io.File;
import java.util.*;

import com.mongodb.Cursor;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import static com.mongodb.client.model.Filters.*;

@Component
public class ReportsService {

    private static final Logger logger = LoggerFactory.getLogger(MongoDatabase.class);

    @Autowired
    private Environment env;

    private static final String FIELD_TYPE = "type";
    private static final String FIELD_PERIOD = "period";
    private static final String PERIOD_MONTH = "month";
    private static final String PERIOD_QUARTER = "quarter";
    private static final String FIELD_TERM = "term";
    private static final String FIELD_YEAR = "year";
    private static final String FIELD_URL = "url";

    public static final String LOCO_POSITION_REPORT = "Loco_Position_Report";
    public static final String ENFORCEMENT_REPORT = "Enforcement_Report";
    public static final String INIT_FAILED_REPORT = "Init_Failed_Report";

    private MongoCollection<Document> reports;

    private void init() {
        if (!env.containsProperty("reports.mongo.url")) {
            logger.error("reports.mongo.url is not defined in application properties!");
        }
        MongoClientURI uri = new MongoClientURI(Objects.requireNonNull(env.getProperty("reports.mongo.url")));

        MongoClient mongoClient = new MongoClient(uri);

        if (!env.containsProperty("reports.mongo.database")) {
            logger.error("reports.mongo.database is not defined in application properties!");
        }
        MongoDatabase reportsDB = mongoClient.getDatabase(Objects.requireNonNull(env.getProperty("reports.mongo.database")));

        if (!env.containsProperty("reports.mongo.collection")) {
            logger.error("reports.mongo.collection is not defined in application properties!");
        }
        reports = reportsDB.getCollection(Objects.requireNonNull(env.getProperty("reports.mongo.collection")));
    }

    private MongoCollection<Document> getReportsCollection() {
        if (reports == null)
            init();
        return reports;
    }

    public List<Document> readReports() {
        List<Document> result = new ArrayList<>();

        for (Document document : getReportsCollection().find()) result.add(document);

        logger.info("Reports found: " + result.size());

        return result;
    }

}
