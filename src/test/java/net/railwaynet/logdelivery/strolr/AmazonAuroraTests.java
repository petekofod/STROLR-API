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
            service.insertData();
        } catch (SQLException e) {
            logger.debug("Can't insert data!", e);
            assert(false);
        }
    }
}
