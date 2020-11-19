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

import javax.servlet.http.HttpServletResponse;
import java.security.Principal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;

import static java.util.stream.Collectors.toList;

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
    public static final String MESSAGE_TYPE = "messageType";

    @Autowired
    private Environment env;

    @Autowired
    private RailroadsService railroadsService;

    @Autowired
    LocomotiveMessagesService locomotiveMessagesService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private AmazonSQS SQS;
    private String QUEUE_URL;
    private String STATUS_QUEUE_URL;
    private String BACKOFFICE_QUEUE_URL;
    private String FEDERATION_QUEUE_URL;

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
        String statusQueueName = Objects.requireNonNull(env.getProperty("status.request.queue.name"));
        String backofficeQueueName = Objects.requireNonNull(env.getProperty("backoffice.request.queue.name"));
        String federationQueueName = Objects.requireNonNull(env.getProperty("federation.request.queue.name"));
        logger.info("Initialing request queue: " + queueName);
        QUEUE_URL = SQS.getQueueUrl(queueName).getQueueUrl();
        logger.info("Initialing status request queue: " + statusQueueName);
        STATUS_QUEUE_URL = SQS.getQueueUrl(statusQueueName).getQueueUrl();
        logger.info("Initialing backoffice request queue: " + backofficeQueueName);
        BACKOFFICE_QUEUE_URL = SQS.getQueueUrl(backofficeQueueName).getQueueUrl();
        logger.info("Initialing federation request queue: " + federationQueueName);
        FEDERATION_QUEUE_URL = SQS.getQueueUrl(federationQueueName).getQueueUrl();
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

    private String getStatusQueueUrl() {
        if (STATUS_QUEUE_URL == null)
            init();
        return STATUS_QUEUE_URL;
    }

    private String getBackofficeQueueUrl() {
        if (BACKOFFICE_QUEUE_URL == null)
            init();
        return BACKOFFICE_QUEUE_URL;
    }

    private String getFederationQueueUrl() {
        if (FEDERATION_QUEUE_URL == null)
            init();
        return FEDERATION_QUEUE_URL;
    }

    private String sendRequest (final Map<String, String> payloadMap, UserDetails currentUser, String queue) {

        String payload;

        payloadMap.remove(MESSAGE_TYPE);

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
                .withQueueUrl(queue)
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

    private String sendLogsRequest (final Map<String, String> payloadMap, UserDetails currentUser) {
        logger.info("Handling logs retrieval request");

        if (!payloadMap.containsKey(TIME_ZONE) || payloadMap.get(TIME_ZONE).isEmpty()) {
            logger.error("Time zone is not specified!");
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR, "Time zone is not specified!");
        }

        try {
            Date startDate = tzModifiedDate(
                    getDateFromParams(payloadMap.get(START_DATE), payloadMap.get(START_TIME)),
                    payloadMap.get(TIME_ZONE), payloadMap.get("dst"));
            Date endDate = tzModifiedDate(
                    getDateFromParams(payloadMap.get(END_DATE), payloadMap.get(END_TIME)),
                    payloadMap.get(TIME_ZONE), payloadMap.get("dst"));
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

        return sendRequest(payloadMap, currentUser, getQueueUrl());
    }

    private String sendStatusRequest(final Map<String, String> payloadMap, UserDetails currentUser) {
        logger.info("Handling locomotive status request");
        return sendRequest(payloadMap, currentUser, getStatusQueueUrl());
    }

    private String sendBackofficeRequest(final Map<String, String> payloadMap, UserDetails currentUser) {
        logger.info("Handling backoffice status request");
        payloadMap.remove("LocoID");
        return sendRequest(payloadMap, currentUser, getBackofficeQueueUrl());
    }

    private String sendFederationRequest(final Map<String, String> payloadMap, UserDetails currentUser) {
        logger.info("Handling federation status request");
        List<String> federations = railroadsService.getFederationsBySCAC(currentUser.getUsername());
        payloadMap.put("federations", String.join(",", federations));
        return sendRequest(payloadMap, currentUser, getFederationQueueUrl());
    }

    private static String toCSV(List<Map<String, Object>> list) {
        List<String> headers = list.stream().flatMap(map -> map.keySet().stream()).distinct().collect(toList());
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < headers.size(); i++) {
            sb.append(headers.get(i));
            sb.append(i == headers.size()-1 ? "\n" : ",");
        }
        for (Map<String, Object> map : list) {
            for (int i = 0; i < headers.size(); i++) {
                sb.append(map.get(headers.get(i)));
                sb.append(i == headers.size()-1 ? "\n" : ",");
            }
        }
        return sb.toString();
    }

    @RequestMapping(
            value = "/locomotive-messages.csv",
            method = RequestMethod.POST)
    @ResponseBody
    public String locomotiveMessagesCSV(Principal principal, @RequestBody String payload, HttpServletResponse response) {
        UserDetails currentUser = (UserDetails) ((Authentication) principal).getPrincipal();
        logger.debug(currentUser.getUsername() + " requesting the list of locomotive messages in CSV format");
        logger.debug(payload);

        final Map<String, String> payloadMap = payloadMap(payload);

        List<Map<String, Object>> messages;

        Date startDate = getUTC(payloadMap.get(START_DATE), payloadMap.get(START_TIME), payloadMap.get("timeZone"), payloadMap.get("dst"));
        Date endDate = getUTC(payloadMap.get(END_DATE), payloadMap.get(END_TIME), payloadMap.get("timeZone"), payloadMap.get("dst"));

        messages = locomotiveMessagesService.getMessagesForCSV(
                startDate, endDate,
                payloadMap.get(MESSAGE_TYPE));

        response.setContentType("application/download");
        return toCSV(messages);
    }

    private Date getUTC(String d, String t, String tz, String dstString) {
        LocalDateTime ldt = LocalDateTime.parse(d + "T" + t + ":00");

        int offset = Integer.parseInt(tz);
        boolean dst = Boolean.parseBoolean(dstString);

        //Apply DST to everything except UTC
        if (dst && offset != 0) {
            offset = offset - 1;
        }

        return new Date(ldt.plusHours(offset).atOffset(ZoneOffset.UTC).toInstant().toEpochMilli());
    }

    @RequestMapping(
            value = "/locomotive-messages",
            method = RequestMethod.POST)
    public String locomotiveMessages(Principal principal, @RequestBody String payload) {
        UserDetails currentUser = (UserDetails) ((Authentication) principal).getPrincipal();
        logger.debug(currentUser.getUsername() + " requesting the list of locomotive messages");
        logger.debug(payload);

        final Map<String, String> payloadMap = payloadMap(payload);

        Date startDate = getUTC(payloadMap.get(START_DATE), payloadMap.get(START_TIME), payloadMap.get("timeZone"), payloadMap.get("dst"));
        Date endDate = getUTC(payloadMap.get(END_DATE), payloadMap.get(END_TIME), payloadMap.get("timeZone"), payloadMap.get("dst"));

        Map<String, Object> result = new HashMap<>();

        result.put("messages",
            locomotiveMessagesService.getMessages(startDate, endDate,payloadMap.get(MESSAGE_TYPE)));

        try {
            return objectMapper.writeValueAsString(result);
        } catch (JsonProcessingException e) {
            logger.error("Can't generate JSON with a list of messages!");
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR, "Can't generate JSON with a list of messages!", e);
        }
    }

    private Map<String, String> payloadMap(String payload) {
        final Map<String, String> payloadMap;

        try {
            payloadMap = objectMapper.readValue(payload,
                    new TypeReference<Map<String, String>>() {});
        } catch (JsonProcessingException e) {
            logger.error("Can't parse the request JSON", e);
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR, "Can't parse the request JSON", e);
        }

        return payloadMap;
    }

    @RequestMapping(
            value = "/data-request",
            method = RequestMethod.POST)
    public String requestLogs(Principal principal, @RequestBody String payload) {
        UserDetails currentUser = (UserDetails) ((Authentication) principal).getPrincipal();
        logger.debug(currentUser.getUsername() + " sent a request:");
        logger.debug(payload);

        final Map<String, String> payloadMap = payloadMap(payload);

        if (!payloadMap.containsKey(REQUEST_TYPE) || payloadMap.get(REQUEST_TYPE).isEmpty()) {
            logger.error("Request type is not specified!");
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR, "Request type is not specified!");
        }

        if (payloadMap.get(REQUEST_TYPE).equals("get-logs"))
            return sendLogsRequest(payloadMap, currentUser);

        if (payloadMap.get(REQUEST_TYPE).equals("get-status"))
            return sendStatusRequest(payloadMap, currentUser);

        if (payloadMap.get(REQUEST_TYPE).equals("get-backoffice"))
            return sendBackofficeRequest(payloadMap, currentUser);

        if (payloadMap.get(REQUEST_TYPE).equals("get-federation"))
            return sendFederationRequest(payloadMap, currentUser);

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
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR, "Can't generate status update JSON", e);
        }
        if (status == null) {
            throw new ResponseStatusException(
                    HttpStatus.NO_CONTENT, "No status update available");
        }
        return status;
    }

    /**
     * Convert string values of date and time from the request form into Date object
     * @param d Date
     * @param t Time
     * @return Date object of this datetime
     */
    private Date getDateFromParams(String d, String t) throws ParseException {
        return new SimpleDateFormat(DATE_PATTERN).parse(d + ":" + t);
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
