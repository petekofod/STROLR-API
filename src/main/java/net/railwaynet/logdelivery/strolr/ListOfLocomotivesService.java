package net.railwaynet.logdelivery.strolr;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class ListOfLocomotivesService {

    private static final Logger logger = LoggerFactory.getLogger(ListOfLocomotivesService.class);

    @Autowired
    private Environment env;

    private final String TIME_FORMAT = "yyyy-MM-dd HH:mm:ssX";
    private final SimpleDateFormat sdf = new SimpleDateFormat(TIME_FORMAT);

    private Map<String, Object> makeLocomotive(ResultSet resultSet) throws SQLException {
        Map<String, Object> result = new HashMap<>();

        result.put("SCAC", resultSet.getString("scac"));
        result.put("Mark", resultSet.getString("mark"));
        result.put("LocoID", resultSet.getString("locoID"));

        Map<String, Object> attModem = new HashMap<>();
        attModem.put("Address", resultSet.getString("ATTModem_Address"));
        attModem.put("IsOnline", resultSet.getBoolean("ATTModem_Status"));
        result.put("ATTModem", attModem);

        Map<String, Object> vzwModem = new HashMap<>();
        vzwModem.put("Address", resultSet.getString("VZWModem_Address"));
        vzwModem.put("IsOnline", resultSet.getBoolean("VZWModem_Status"));
        result.put("VZWModem", vzwModem);

        Map<String, Object> wifiModem = new HashMap<>();
        wifiModem.put("Address", resultSet.getString("WiFi_Address"));
        wifiModem.put("IsOnline", resultSet.getBoolean("WiFi_Status"));
        result.put("WiFi", wifiModem);

        Map<String, Object> radioModem = new HashMap<>();
        radioModem.put("Address", resultSet.getString("Radio_Address"));
        radioModem.put("IsOnline", resultSet.getBoolean("Radio_Status"));
        result.put("Radio", radioModem);

        return result;
    }

    public Map<String, Object> getLocomotives() throws SQLException {
        Map<String, Object> result = new HashMap<>();

        String endpoint = Objects.requireNonNull(env.getProperty("aws.rds.endpoint"));
        String jdbc_url = "jdbc:postgresql://" + endpoint + ":5432/postgres";

        List<Map<String, Object>> locomotives = new ArrayList<>();
        String db_username = Objects.requireNonNull(env.getProperty("aws.rds.username"));
        String db_password = Objects.requireNonNull(env.getProperty("aws.rds.password"));
        Connection conn = DriverManager.getConnection(jdbc_url, db_username, db_password);
        Statement statement = conn.createStatement();
        String sql = "SELECT " +
                "scac, mark, locoID, " +
                "ATTModem_Address, ATTModem_Status, " +
                "VZWModem_Address, VZWModem_Status, " +
                "WiFi_Address, WiFi_Status, " +
                "Radio_Address, Radio_Status, created_at " +
                " FROM locomotives join history on history.id = locomotives.history_id " +
                " where history_id = (select id from history order by created_at desc limit 1);";
        ResultSet resultSet = statement.executeQuery(sql);

        boolean firstLine = true;
        while (resultSet.next()) {
            if (firstLine) {
                logger.debug("Adding date/time to the list of locomotives");
                result.put("StartTestTime", sdf.format(resultSet.getTimestamp("created_at")));
                firstLine = false;
            }

            locomotives.add(makeLocomotive(resultSet));
        }
        result.put("Locomotives", locomotives);

        return result;
    }
}
