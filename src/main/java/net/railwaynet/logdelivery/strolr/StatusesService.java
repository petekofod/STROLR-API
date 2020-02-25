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
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class StatusesService {

    private static final Logger logger = LoggerFactory.getLogger(StatusesService.class);

    private static final String QUEUE_NAME = "dev_strolrlogresponse.fifo";

    private static final BasicAWSCredentials bAWSc = new BasicAWSCredentials("AKIAZZT54LR2UM7XFBET", "mW8/hHj/NafsEZqJNaAnzoRSjruT/FUbQZ2YxF56");
    private static final AmazonSQS sqs = AmazonSQSClientBuilder.standard()
            .withRegion("us-east-1")
            .withCredentials(new AWSStaticCredentialsProvider(bAWSc))
            .build();
    private static String QUEUE_URL = sqs.getQueueUrl(QUEUE_NAME).getQueueUrl();

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private static final Map<String, Map<String, String>> statusUpdates = new HashMap<>();

    @Async
    public void monitorResponseQueue() throws InterruptedException {
        while (true) {
            Thread.sleep(1000); // sleep for 1 second
            logger.info("Checking for new messages");

            ReceiveMessageRequest receiveMessageRequest = new ReceiveMessageRequest(QUEUE_URL)
                    .withWaitTimeSeconds(10)
                    .withMaxNumberOfMessages(10);

            List<Message> sqsMessages = sqs.receiveMessage(receiveMessageRequest).getMessages();
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
            sqs.deleteMessage(new DeleteMessageRequest()
                    .withQueueUrl(QUEUE_URL)
                    .withReceiptHandle(m.getReceiptHandle()));
        }
    }

    private void putStatus(String messageId, Map<String, String> status) {
        String statusText;
        switch (status.get("Status")) {
            case "1":
                statusText = "Request is being processed";
                break;
            case "2":
                statusText = "Logs have been found";
                status.put("filesCount", status.get("Info"));
                break;
            case "3":
                statusText = "Logs have been archived";
                status.put("totalBytes", status.get("Info"));
                break;
            case "4":
                statusText = "The archive has been uploaded";
                break;
            default:
                statusText = "";
                logger.warn("Unknown status of the message! Status = " + status);
        }
        status.put("statusText", statusText);
        statusUpdates.put(messageId, status);
    }

    public String getStatus(String messageId) throws JsonProcessingException {
        if (!statusUpdates.containsKey(messageId))
            return null;
        Map<String, String> status = statusUpdates.get(messageId);
        statusUpdates.remove(messageId);
        return objectMapper.writeValueAsString(status);
    }
}
