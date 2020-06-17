package net.railwaynet.logdelivery.strolr;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.DeleteMessageRequest;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class StatusesService {

    private static final Logger logger = LoggerFactory.getLogger(StatusesService.class);
    public static final String INFO_ATTR = "Info";
    public static final String STATUS_TEXT = "statusText";

    @Autowired
    private Environment env;

    private ObjectMapper objectMapper = null;

    private AmazonSQS SQS = null;
    private String LOG_QUEUE_URL = null;
    private String STATUS_QUEUE_URL = null;

    private void init() {
        objectMapper = JsonMapper.builder().enable(JsonReadFeature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER).build();
        BasicAWSCredentials bAWSc = new BasicAWSCredentials(
                Objects.requireNonNull(env.getProperty("aws.api.key")),
                Objects.requireNonNull(env.getProperty("aws.api.secret")));
        SQS = AmazonSQSClientBuilder.standard()
                .withRegion(Objects.requireNonNull(env.getProperty("aws.region")))
                .withCredentials(new AWSStaticCredentialsProvider(bAWSc))
                .build();
        String logQueueName = Objects.requireNonNull(env.getProperty("response.queue.name"));
        logger.info("Initialing logs response queue: " + logQueueName);
        LOG_QUEUE_URL = SQS.getQueueUrl(logQueueName).getQueueUrl();
        String statusQueueName = Objects.requireNonNull(env.getProperty("status.response.queue.name"));
        logger.info("Initialing status response queue: " + statusQueueName);
        STATUS_QUEUE_URL = SQS.getQueueUrl(statusQueueName).getQueueUrl();
    }

    private ObjectMapper getObjectMapper() {
        if (objectMapper == null)
            init();
        return objectMapper;
    }

    private AmazonSQS getSQS() {
        if (SQS == null)
            init();
        return SQS;
    }

    private String getLogQueueUrl() {
        if (LOG_QUEUE_URL == null)
            init();
        return LOG_QUEUE_URL;
    }

    private String getStatusQueueUrl() {
        if (STATUS_QUEUE_URL == null)
            init();
        return STATUS_QUEUE_URL;
    }

    private final Map<String, Map<String, String>> statusUpdates = new HashMap<>();

    private void checkQueue(String queueUrl) {
        ReceiveMessageRequest receiveMessageRequest = new ReceiveMessageRequest(queueUrl)
                .withWaitTimeSeconds(10)
                .withMaxNumberOfMessages(10);

        List<Message> sqsMessages = getSQS().receiveMessage(receiveMessageRequest).getMessages();
        logger.info("Getting new messages: " + sqsMessages.size());

        if (!sqsMessages.isEmpty()) {
            for (Message m: sqsMessages) {
                handleMessage(m, queueUrl);
            }
        }
    }

    @Async
    public void monitorResponseQueue() throws InterruptedException {
        //noinspection InfiniteLoopStatement
        while (true) {
            //noinspection BusyWait
            Thread.sleep(1000); // sleep for 1 second

            logger.info("Checking for new messages in logs queue");
            checkQueue(getLogQueueUrl());

            logger.info("Checking for new messages in status queue");
            checkQueue(getStatusQueueUrl());
        }
    }

    private void handleMessage(Message m, String queueUrl) {
        logger.debug("Handling message " + m.getBody());

        try {
            final Map<String, String> status;
            status = getObjectMapper().readValue(m.getBody(),
                    new TypeReference<Map<String, String>>() {});
            String messageId = status.get("MessageID");
            putStatus(messageId, status);
        } catch (JsonProcessingException e) {
            logger.warn("Can't parse status update: " + m.getBody(), e);
        } finally {
            logger.debug("Deleting the message");
            getSQS().deleteMessage(new DeleteMessageRequest()
                    .withQueueUrl(queueUrl)
                    .withReceiptHandle(m.getReceiptHandle()));
        }
    }

    synchronized public void putStatus(String messageId, Map<String, String> status) {
        String statusText;
        switch (status.get("Status")) {
            case "1":
                statusText = "Request is being processed";
                break;
            case "2":
                statusText = "Logs have been found";
                status.put("filesCount", status.get(INFO_ATTR).isEmpty()?"0":status.get(INFO_ATTR));
                break;
            case "3":
                statusText = "Logs have been archived";
                status.put("totalBytes", status.get(INFO_ATTR));
                break;
            case "4":
                statusText = "The archive has been uploaded";
                status.put("end", "1");
                break;
            case "1001":
                statusText = "Verifying SCAC and MARK";
                break;
            case "1002":
                statusText = "Looking for the locomotive";
                status.put("TestTime", status.get(INFO_ATTR));
                break;
            case "1003":
                statusText = "Found verizon modem address";
                status.put("VerizonModem", status.get(INFO_ATTR));
                break;
            case "1004":
                statusText = "Found ATT modem address";
                status.put("ATTModem", status.get(INFO_ATTR));
                break;
            case "1005":
                statusText = "Collecting IP Reachability Information";
                break;
            case "1006":
                statusText = "Received ATT IP Status";
                status.put("ATTModemStatus", status.get(INFO_ATTR));
                break;
            case "1007":
                statusText = "Received VZW IP Status";
                status.put("VerizonModemStatus", status.get(INFO_ATTR));
                break;
            case "1008":
                statusText = "Collecting WiFi Configuration";
                break;
            case "1009":
                statusText = "Received Wi-Fi Client status";
                status.put("WiFiClientStatus", status.get(INFO_ATTR));
                break;
            case "1010":
                statusText = "Found Wifi Client SSID";
                status.put("ClientId", status.get(INFO_ATTR));
                break;
            case "1011":
                statusText = "Found Access Point";
                status.put("AccessPoint", status.get(INFO_ATTR));
                break;
            case "1012":
                statusText = "Collecting IETMS Gateway Information";
                break;
            case "1013":
                statusText = "Received IETMS Client status";
                status.put("ETMSClient", status.get(INFO_ATTR));
                break;
            case "1014":
                statusText = "Received MDM Client status";
                status.put("MDMClient", status.get(INFO_ATTR));
                break;
            case "1015":
                statusText = "Collecting ITCM route information";
                break;
            case "1016":
                statusText = "Received ATT ITCM route status";
                status.put("ATTRoute", status.get(INFO_ATTR));
                break;
            case "1017":
                statusText = "Received Verizon ITCM route status";
                status.put("VerizonRoute", status.get(INFO_ATTR));
                break;
            case "1018":
                statusText = "Received Sprint ITCM route status";
                status.put("SprintRoute", status.get(INFO_ATTR));
                break;
            case "1019":
                statusText = "Received Wifi ITCM route status";
                status.put("WifiRoute", status.get(INFO_ATTR));
                break;
            case "1020":
                statusText = "Received 220 MHz Radio ITCM route status";
                status.put("MHzRadio", status.get(INFO_ATTR));
                break;
            case "1021":
                statusText = "Retrieving 220 Base Station Information";
                break;
            case "1022":
                statusText = "Found Radio ID";
                status.put("RadioId", status.get(INFO_ATTR));
                break;
            case "1023":
                statusText = "Found Base Station EMP Address";
                status.put("EMPAddress", status.get(INFO_ATTR));
                break;
            case "1024":
                statusText = "Received Cell Status";
                status.put("CellStatus", status.get(INFO_ATTR));
                break;
            case "1998":
                statusText = "Completed successfully";
                status.put("end", "1");
                break;
            case "1999":
                statusText = "An error detected";
                status.put("error", status.get(INFO_ATTR));
                status.put("end", "1");
                break;
            default:
                statusText = "";
                logger.warn("Unknown status of the message! Status = " + status);
        }
        status.put(STATUS_TEXT, statusText);
        status.remove(INFO_ATTR);
        logger.debug("Adding status for " + messageId);
        logger.debug("Status text is " + statusText);
        if (statusUpdates.containsKey(messageId)) {
            logger.debug("Adding new fields to the existing record");
            Map<String, String> existingStatus = statusUpdates.get(messageId);
            String combinedStatus = existingStatus.get(STATUS_TEXT) + ";" + status.get(STATUS_TEXT);
            logger.debug("Updating old status with combined status: " + combinedStatus);
            status.put(STATUS_TEXT, combinedStatus);
            status.forEach((key,value) -> existingStatus.merge(key, value, (v1, v2) -> v2));
        } else {
            statusUpdates.put(messageId, status);
        }
    }

    synchronized public String getStatus(String messageId) throws JsonProcessingException {
        if (!statusUpdates.containsKey(messageId))
            return null;
        Map<String, String> status = statusUpdates.get(messageId);
        logger.debug("Removing status for MessageID = " + messageId);
        statusUpdates.remove(messageId);
        return getObjectMapper().writeValueAsString(status);
    }
}
