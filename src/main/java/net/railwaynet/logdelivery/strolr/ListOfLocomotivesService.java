package net.railwaynet.logdelivery.strolr;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.rds.AmazonRDS;
import com.amazonaws.services.rds.AmazonRDSClientBuilder;
import com.amazonaws.services.rds.model.CreateDBInstanceRequest;
import com.amazonaws.services.rds.model.DBInstance;
import com.amazonaws.services.rds.model.DescribeDBInstancesResult;
import com.amazonaws.services.rds.model.Endpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class ListOfLocomotivesService {

    private static final Logger logger = LoggerFactory.getLogger(ListOfLocomotivesService.class);

    @SuppressWarnings("SpellCheckingInspection")
    private AWSCredentialsProvider credentials = new AWSStaticCredentialsProvider(
            new BasicAWSCredentials(
//                    "AKIAZZT54LR2YYDAEK5S",
//                    "jiFbruq3LSy/o9Sqi6Hkd5+t1FtdRNjJPLLtag+k")
            "AKIAITXGJL3A5KKC7L7A",
            "jC4SEtH9uMkFFbLijEif9l+gY6HP3N+4LbuZ2IUN")
    );

    @SuppressWarnings("SpellCheckingInspection")
//    private String url = "jdbc:postgresql://strolrdevreporterdb.cluster-cazrvbfnv6bw.us-east-2.rds.amazonaws.com/strolrdevreporterdb:5432/";
    private String endpoint = "database-1.chduvwzl9h3v.us-east-1.rds.amazonaws.com";
    private String jdbc_url = "jdbc:postgresql://" + endpoint + ":5432/postgres";
//    private String jdbc_url = "jdbc:postgresql://" + endpoint + ":5432/strolrdevreporterdb";


    private final String db_username = "postgres";
    //    private final String db_password = "eL3kcV3SUf5iEXwmCoGI";
    private final String db_password = "3016295Abrikos!!";

    private AmazonRDS amazonRDS = AmazonRDSClientBuilder.standard().withCredentials(credentials)
            .withRegion(Regions.AP_SOUTHEAST_2).build();

    public void createTable() throws SQLException {
        Connection conn = DriverManager.getConnection(jdbc_url, db_username, db_password);
        Statement statement = conn.createStatement();
        String sql = "CREATE TABLE IF NOT EXISTS locomotives (" +
                "id SERIAL PRIMARY KEY, " +
                "scac character varying(255) NOT NULL," +
                "mark character varying(255) NOT NULL," +
                "locoID character varying(255) NOT NULL," +
                "ATTModem_Address character varying(15)," +
                "ATTModem_Status boolean NOT NULL," +
                "VZWModem_Address character varying(15)," +
                "VZWModem_Status boolean NOT NULL," +
                "WiFi_Address character varying(255)," +
                "WiFi_Status boolean NOT NULL," +
                "Radio_Address character varying(255)," +
                "Radio_Status boolean NOT NULL" +
                ")";
        statement.executeUpdate(sql);
    }

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
        result.put("StartTestTime", "2020-08-01:00:01");

        List<Map<String, Object>> locomotives = new ArrayList<>();
        Connection conn = DriverManager.getConnection(jdbc_url, db_username, db_password);
        Statement statement = conn.createStatement();
        String sql = "SELECT " +
                "scac, mark, locoID, " +
                "ATTModem_Address, ATTModem_Status, " +
                "VZWModem_Address, VZWModem_Status, " +
                "WiFi_Address, WiFi_Status, " +
                "Radio_Address, Radio_Status " +
                " FROM locomotives";
        ResultSet resultSet = statement.executeQuery(sql);
        while (resultSet.next()) {
            locomotives.add(makeLocomotive(resultSet));
        }
        result.put("Locomotives", locomotives);

        return result;
    }

    public void insertData(String locoID, String ip1, String ip2) throws SQLException {
        Connection conn = DriverManager.getConnection(jdbc_url, db_username, db_password);
        PreparedStatement preparedStatement = conn.prepareStatement(
                "INSERT INTO locomotives (" +
                        "scac, mark, locoID, " +
                        "ATTModem_Address, ATTModem_Status, " +
                        "VZWModem_Address, VZWModem_Status, " +
                        "WiFi_Address, WiFi_Status, " +
                        "Radio_Address, Radio_Status " +
                ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");

        preparedStatement.setString(1, "AMTK");
        preparedStatement.setString(2, "AMTK");
        preparedStatement.setString(3, String.valueOf(Integer.parseInt(locoID) + 100));

        preparedStatement.setString(4, ip1);
        preparedStatement.setBoolean(5, false);

        preparedStatement.setString(6, ip2);
        preparedStatement.setBoolean(7, true);

        preparedStatement.setString(8, "AP Mac");
        preparedStatement.setBoolean(9, false);

        preparedStatement.setString(10, "Base ID");
        preparedStatement.setBoolean(11, false);

        preparedStatement.executeUpdate();
    }

    public void listDatabases() throws SQLException {
        DescribeDBInstancesResult result = amazonRDS.describeDBInstances();
        List<DBInstance> instances = result.getDBInstances();
        logger.debug("List of instances:");
        for (DBInstance instance : instances) {
            // Information about each RDS instance
            String identifier = instance.getDBInstanceIdentifier();
            String engine = instance.getEngine();
            String status = instance.getDBInstanceStatus();
            Endpoint endpoint = instance.getEndpoint();
            logger.debug("===============");
            logger.debug("Identifier: " + identifier);
            logger.debug("engine: " + engine);
            logger.debug("status: " + status);
            logger.debug("endpoint: " + endpoint.getAddress() + ", " + endpoint.getHostedZoneId() + ", " + endpoint.getPort());
        }
    }

    public void createDatabase() throws SQLException {
        CreateDBInstanceRequest request = new CreateDBInstanceRequest();
        // RDS instance name
        request.setDBInstanceIdentifier("database-1");
        request.setEngine("postgres");
        request.setMultiAZ(false);
        request.setMasterUsername(db_username);
        request.setMasterUserPassword(db_password);
        request.setDBName("strolrdevreporterdb");
        request.setDBInstanceClass("db.t2.micro");
        request.setStorageType("gp2");
        request.setAllocatedStorage(10);

        DBInstance instance = amazonRDS.createDBInstance(request);
    }

}
