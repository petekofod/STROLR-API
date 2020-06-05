package net.railwaynet.logdelivery.strolr;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.DeleteMessageRequest;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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

    @Autowired
    private Environment env;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private AmazonSQS SQS;
    private String QUEUE_URL;

    private void init() {
        BasicAWSCredentials bAWSc = new BasicAWSCredentials(
                Objects.requireNonNull(env.getProperty("aws.api.key")),
                Objects.requireNonNull(env.getProperty("aws.api.secret")));
        SQS = AmazonSQSClientBuilder.standard()
                .withRegion(Objects.requireNonNull(env.getProperty("aws.region")))
                .withCredentials(new AWSStaticCredentialsProvider(bAWSc))
                .build();
        String queueName = Objects.requireNonNull(env.getProperty("response.queue.name"));
        logger.info("Initialing response queue: " + queueName);
        QUEUE_URL = SQS.getQueueUrl(queueName).getQueueUrl();
    }

    private AmazonSQS getSQS() {
        if (SQS == null)
            init();
        return SQS;
    }

    private String getQueueUrl() {
        if (QUEUE_URL == null)
            init();
        return QUEUE_URL;
    }

    private final Map<String, Map<String, String>> statusUpdates = new HashMap<>();

    @Async
    public void monitorResponseQueue() throws InterruptedException {
        //noinspection InfiniteLoopStatement
        while (true) {
            Thread.sleep(1000); // sleep for 1 second
            logger.info("Checking for new messages");

            ReceiveMessageRequest receiveMessageRequest = new ReceiveMessageRequest(getQueueUrl())
                    .withWaitTimeSeconds(10)
                    .withMaxNumberOfMessages(10);

            List<Message> sqsMessages = getSQS().receiveMessage(receiveMessageRequest).getMessages();
            logger.info("Getting new messages: " + sqsMessages.size());

            if (!sqsMessages.isEmpty()) {
                for (Message m: sqsMessages) {
                    handleMessage(m);
                }
            }
        }
    }

    private void handleMessage(Message m) {
        logger.debug("Handling message " + m.getBody());

        try {
            final Map<String, String> status;
            status = objectMapper.readValue(m.getBody(),
                    new TypeReference<Map<String, String>>() {});
            String messageId = status.get("MessageID");
            putStatus(messageId, status);
        } catch (JsonProcessingException e) {
            logger.warn("Can't parse status update: " + m.getBody(), e);
        } finally {
            logger.debug("Deleting the message");
            getSQS().deleteMessage(new DeleteMessageRequest()
                    .withQueueUrl(getQueueUrl())
                    .withReceiptHandle(m.getReceiptHandle()));
        }
    }

    public void putStatus(String messageId, Map<String, String> status) {
        String statusText;
        switch (status.get("Status")) {
            case "1":
                statusText = "Request is being processed";
                break;
            case "2":
                statusText = "Logs have been found";
                status.put("filesCount", status.get("Info").isEmpty()?"0":status.get("Info"));
                break;
            case "3":
                statusText = "Logs have been archived";
                status.put("totalBytes", status.get("Info"));
                break;
            case "4":
                statusText = "The archive has been uploaded";
                status.put("end", "1");
                break;
            default:
                statusText = "";
                logger.warn("Unknown status of the message! Status = " + status);
        }
        status.put("statusText", statusText);
        logger.debug("Adding status for " + messageId);
        logger.debug("Status text is " + statusText);
        statusUpdates.put(messageId, status);
    }

    public String getStatus(String messageId) throws JsonProcessingException {
        if (!statusUpdates.containsKey(messageId))
            return null;
        Map<String, String> status = statusUpdates.get(messageId);
        logger.debug("Removing status for MessageID = " + messageId);
        statusUpdates.remove(messageId);
        return objectMapper.writeValueAsString(status);
    }
}
