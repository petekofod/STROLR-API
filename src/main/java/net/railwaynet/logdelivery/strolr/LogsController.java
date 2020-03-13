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
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Configuration
@RestController
@Component
public class LogsController {
    private static final Logger logger = LoggerFactory.getLogger(LogsController.class);

    @Autowired
    private Environment env;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private AmazonSQS SQS;
    private String QUEUE_URL;

    @Autowired
    private StatusesService statusesService;

    private void init() {
        BasicAWSCredentials bAWSc = new BasicAWSCredentials(
                Objects.requireNonNull(env.getProperty("aws.api.key")),
                Objects.requireNonNull(env.getProperty("aws.api.secret")));
        SQS = AmazonSQSClientBuilder.standard()
                .withRegion(Objects.requireNonNull(env.getProperty("aws.region")))
                .withCredentials(new AWSStaticCredentialsProvider(bAWSc))
                .build();
        String queueName = Objects.requireNonNull(env.getProperty("request.queue.name"));
        logger.info("Initialing request queue: " + queueName);
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
                .withQueueUrl(getQueueUrl())
                .withMessageBody(payload)
                .withMessageAttributes(messageAttributes)
                .withDelaySeconds(5);
        SendMessageResult result = getSQS().sendMessage(send_msg_request);

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
