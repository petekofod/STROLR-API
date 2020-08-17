package net.railwaynet.logdelivery.strolr;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.List;

public class AmazonAuroraTests {

    private static final Logger logger = LoggerFactory.getLogger(AmazonAuroraTests.class);

    private ListOfLocomotivesService service = new ListOfLocomotivesService();

    @Test
    void createTable() {
        try {
            service.createTable();
        } catch (SQLException e) {
            logger.debug("Can't create a table!", e);
            assert(false);
        }
    }

    @Test
    void listDatabases() {
        try {
            service.listDatabases();
        } catch (SQLException e) {
            logger.debug("Can't create a table!", e);
            assert(false);
        }
    }

    @Test
    void createDatabase() {
        try {
            service.createDatabase();
        } catch (SQLException e) {
            logger.debug("Can't create a database!", e);
            assert(false);
        }
    }

    @Test
    void insertData() {
        try {
            service.insertData("101", "3.3.3.3", "4.4.4.4");
            service.insertData("102", "5.5.5.5", "6.6.4.4");
            service.insertData("103", "7.7.7.7", "7.7.4.4");
            service.insertData("104", "9.9.9.9", "8.8.4.4");
        } catch (SQLException e) {
            logger.debug("Can't insert data!", e);
            assert(false);
        }
    }
}
