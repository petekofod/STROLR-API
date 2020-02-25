package net.railwaynet.logdelivery.strolr;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.MessageAttributeValue;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.amazonaws.services.sqs.model.SendMessageResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

@RestController
public class LogsController {
    private static final Logger logger = LoggerFactory.getLogger(LogsController.class);

    private static final String QUEUE_NAME = "dev_strolrlogrequest";

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final BasicAWSCredentials bAWSc = new BasicAWSCredentials("AKIAZZT54LR2UM7XFBET", "mW8/hHj/NafsEZqJNaAnzoRSjruT/FUbQZ2YxF56");
    private static final AmazonSQS sqs = AmazonSQSClientBuilder.standard()
            .withRegion("us-east-1")
            .withCredentials(new AWSStaticCredentialsProvider(bAWSc))
            .build();
    private static String QUEUE_URL = sqs.getQueueUrl(QUEUE_NAME).getQueueUrl();

    @Autowired
    private StatusesService statusesService;

    @RequestMapping(
            value = "/request-logs",
            method = RequestMethod.POST)
    public String requestLogs(Principal principal, @RequestBody String payload) {
        UserDetails currentUser = (UserDetails) ((Authentication) principal).getPrincipal();
        logger.debug(currentUser.getUsername() + " requesting logs");
        logger.debug(payload);

        final Map<String, String> payloadMap;
        final Map<String, MessageAttributeValue> messageAttributes = new HashMap<>();

        messageAttributes.put("SCAC", new MessageAttributeValue()
                .withDataType("String")
                .withStringValue(currentUser.getUsername()));

        try {
            payloadMap = objectMapper.readValue(payload,
                    new TypeReference<Map<String, String>>() {});
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR, "Can't parse the log request JSON", e);
        }

        for (Map.Entry<String, String> entry : payloadMap.entrySet()) {
            messageAttributes.put(entry.getKey(), new MessageAttributeValue()
                    .withDataType("String")
                    .withStringValue(entry.getValue()));
        }

        SendMessageRequest send_msg_request = new SendMessageRequest()
                .withQueueUrl(QUEUE_URL)
                .withMessageBody(payload)
                .withMessageAttributes(messageAttributes)
                .withDelaySeconds(5);
        SendMessageResult result = sqs.sendMessage(send_msg_request);

        logger.info("Message ID is " + result.getMessageId());
        return result.getMessageId();
    }

    @RequestMapping(value = "/status-update/{messageId}")
    public String statusUpdate(Principal principal, @PathVariable("messageId") String messageId) {
        logger.debug("Getting status of message " + messageId);
        String status;
        try {
            status = statusesService.getStatus(messageId);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR, "Can't generate status update JSON", e);
        }
        if (status == null)
            throw new ResponseStatusException(
                    HttpStatus.NO_CONTENT, "No status update available");
        return status;
    }
}
