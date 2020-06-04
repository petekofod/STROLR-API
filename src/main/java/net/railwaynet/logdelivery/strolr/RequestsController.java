package net.railwaynet.logdelivery.strolr;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.AmazonSQSException;
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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Configuration
@RestController
@Component
public class RequestsController {
    private static final Logger logger = LoggerFactory.getLogger(RequestsController.class);

    public static final String TIME_ZONE = "timeZone";
    public static final String START_DATE = "StartDate";
    public static final String START_TIME = "StartTime";
    public static final String END_DATE = "EndDate";
    public static final String END_TIME = "EndTime";
    public static final String DATE_PATTERN = "yyyy-MM-dd:HH:mm";
    public static final String SCAC = "SCAC";
    public static final String SCAC_MARK = "SCACMark";
    public static final String REQUEST_TYPE = "RequestType";

    @Autowired
    private Environment env;

    @Autowired
    private RailroadsService railroadsService;

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

    private String sendLogsRequest (final Map<String, String> payloadMap, UserDetails currentUser) {
        logger.info("Handling logs retrieval request");

        String startDateTime=payloadMap.get(START_DATE) + ":" + payloadMap.get(START_TIME);
        String endDateTime=payloadMap.get(END_DATE) + ":" + payloadMap.get(END_TIME);
        try {
            logger.debug("startDateTime: " + startDateTime);
            logger.debug("endDateTime:    " + endDateTime);
            Date startDate = new SimpleDateFormat(DATE_PATTERN).parse(startDateTime);
            Date endDate = new SimpleDateFormat(DATE_PATTERN).parse(endDateTime);
            startDate = tzModifiedDate(startDate, payloadMap.get(TIME_ZONE), payloadMap.get("dst"));
            endDate = tzModifiedDate(endDate, payloadMap.get(TIME_ZONE), payloadMap.get("dst"));
            String sStartDate = new SimpleDateFormat("yyyy-MM-dd").format(startDate);
            String sStartTime = new SimpleDateFormat("HH:mm").format(startDate);
            String sEndDate = new SimpleDateFormat("yyyy-MM-dd").format(endDate);
            String sEndTime = new SimpleDateFormat("HH:mm").format(endDate);
            logger.debug("Modified StartDate: " + sStartDate);
            logger.debug("Modified StartTime: " + sStartTime);
            logger.debug("Modified EndDate:   " + sEndDate);
            logger.debug("Modified EndTime:   " + sEndTime);

            payloadMap.put(START_DATE, sStartDate);
            payloadMap.put(START_TIME, sStartTime);
            payloadMap.put(END_DATE, sEndDate);
            payloadMap.put(END_TIME, sEndTime);

        } catch (ParseException e) {
            logger.error("Can't parse the payload!", e);
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR, "Can't parse the payload!");
        }

        //put payloadMap back in to payload
        String payload;
        try {
            payload = objectMapper.writeValueAsString(payloadMap);
        } catch (JsonProcessingException e) {
            logger.error("Can't convert the data back to JSON!", e);
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR, "Can't convert the data back to JSON!");
        }

        final Map<String, MessageAttributeValue> messageAttributes = new HashMap<>();
        messageAttributes.put(SCAC, new MessageAttributeValue()
                .withDataType("String")
                .withStringValue(railroadsService.getSCACbyMARK(currentUser.getUsername(), payloadMap.get(SCAC_MARK))));

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

        SendMessageResult result;

        try {
            logger.debug("Send Message Request: " + send_msg_request.toString());
            result = getSQS().sendMessage(send_msg_request);
        } catch (AmazonSQSException e) {
            logger.error("AmazonSQSException while sending the message! " + e.getErrorMessage(), e);
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR, e.getErrorMessage(), e);
        }

        logger.info("Message ID is " + result.getMessageId());
        return result.getMessageId();
    }

    private String sendStatusRequest (final Map<String, String> payloadMap, UserDetails currentUser) {
        logger.info("Handling locomotive status request");
        String testUUID = UUID.randomUUID().toString();
        logger.debug("Return message ID = " + testUUID);
        statusesService.putStatus(testUUID, Stream.of(new String[][] {
                { "Status", "1" },
                { "TestTime", new SimpleDateFormat(DATE_PATTERN).format(new Date()) },
        }).collect(Collectors.toMap(data -> data[0], data -> data[1])));
        return testUUID;
    }

    @RequestMapping(
            value = "/data-request",
            method = RequestMethod.POST)
    public String requestLogs(Principal principal, @RequestBody String payload) {
        UserDetails currentUser = (UserDetails) ((Authentication) principal).getPrincipal();
        logger.debug(currentUser.getUsername() + " sent a request:");
        logger.debug(payload);

        final Map<String, String> payloadMap;

        try {
            payloadMap = objectMapper.readValue(payload,
                    new TypeReference<Map<String, String>>() {});
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR, "Can't parse the log request JSON", e);
        }

        if (!payloadMap.containsKey(TIME_ZONE) || payloadMap.get(TIME_ZONE).isEmpty()) {
            logger.error("Time zone is not specified!");
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR, "Time zone is not specified!");
        }

        if (!payloadMap.containsKey(REQUEST_TYPE) || payloadMap.get(REQUEST_TYPE).isEmpty()) {
            logger.error("Request type is not specified!");
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR, "Request type is not specified!");
        }

        if (payloadMap.get(REQUEST_TYPE).equals("get-logs"))
            return sendLogsRequest(payloadMap, currentUser);

        if (payloadMap.get(REQUEST_TYPE).equals("get-status"))
            return sendStatusRequest(payloadMap, currentUser);

        logger.error("Unknown request type!");
        throw new ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR, "Unknown request type!");
    }

    @RequestMapping(value = "/status-update/{messageId}")
    public String statusUpdate(Principal principal, @PathVariable("messageId") String messageId) {
        logger.debug("Getting status of message " + messageId);
        String status;
        try {
            status = statusesService.getStatus(messageId);
            logger.debug("Status is " + status);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR, "Can't generate status update JSON", e);
        }
        if (status == null) {
            logger.debug("No status available");
            throw new ResponseStatusException(
                    HttpStatus.NO_CONTENT, "No status update available");
        }
        return status;
    }
    
    public Date tzModifiedDate(Date unModifiedDate, String stringOffset, String stringDst) {
    	int offset = Integer.parseInt(stringOffset);
    	boolean dst = Boolean.parseBoolean(stringDst);

    	//Apply DST to everything except UTC
    	if (dst && offset != 0) {
    		offset = offset - 1;    		
    	}

    	Calendar calendar = Calendar.getInstance();
        calendar.setTime(unModifiedDate);
        calendar.add(Calendar.HOUR_OF_DAY, offset);
        return calendar.getTime();
    }
}
