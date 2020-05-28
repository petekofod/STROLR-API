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
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Configuration
@RestController
@Component
public class RequestsController {
    private static final Logger logger = LoggerFactory.getLogger(RequestsController.class);

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

    @RequestMapping(
            value = "/data-request",
            method = RequestMethod.POST)
    public String requestLogs(Principal principal, @RequestBody String payload) {
        UserDetails currentUser = (UserDetails) ((Authentication) principal).getPrincipal();
        logger.debug(currentUser.getUsername() + " requesting logs");
        logger.debug(payload);

        final Map<String, String> payloadMap;
        final Map<String, MessageAttributeValue> messageAttributes = new HashMap<>();

        try {
            payloadMap = objectMapper.readValue(payload,
                    new TypeReference<Map<String, String>>() {});
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR, "Can't parse the log request JSON", e);
        }
        
        //Modify Start and End Date times for Timezones

        if (!payloadMap.containsKey("timeZone") || payloadMap.get("timeZone").isEmpty()) {
            logger.error("Time zone is not specified!");
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR, "Time zone is not specified!");
        }

        String startDateTime=payloadMap.get("StartDate") + ":" + payloadMap.get("StartTime");
        String endDateTime=payloadMap.get("EndDate") + ":" + payloadMap.get("EndTime");
        try {
        	logger.debug("startDateTime: " + startDateTime);
        	logger.debug("endDateTime:   " + endDateTime);
			Date startDate = new SimpleDateFormat("yyyy-MM-dd:HH:mm").parse(startDateTime);
			Date endDate = new SimpleDateFormat("yyyy-MM-dd:HH:mm").parse(endDateTime);
	        startDate = tzModifiedDate(startDate, payloadMap.get("timeZone"), payloadMap.get("dst"));
	        endDate = tzModifiedDate(endDate, payloadMap.get("timeZone"), payloadMap.get("dst"));
	        String sStartDate = new SimpleDateFormat("yyyy-MM-dd").format(startDate);
	        String sStartTime = new SimpleDateFormat("HH:mm").format(startDate);
	        String sEndDate = new SimpleDateFormat("yyyy-MM-dd").format(endDate);
	        String sEndTime = new SimpleDateFormat("HH:mm").format(endDate);
	        logger.debug("Modified StartDate: " + sStartDate);
	        logger.debug("Modified StartTime: " + sStartTime);
	        logger.debug("Modified EndDate:   " + sEndDate);
	        logger.debug("Modified EndTime:   " + sEndTime);
	                
	        payloadMap.put("StartDate", sStartDate);
	        payloadMap.put("StartTime", sStartTime);
	        payloadMap.put("EndDate", sEndDate);
	        payloadMap.put("EndTime", sEndTime);

		} catch (ParseException e) {
            logger.error("Can't parse the payload!", e);
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR, "Can't parse the payload!");
		}

        //put payloadMap back in to payload
        try {
            payload = objectMapper.writeValueAsString(payloadMap);
        } catch (JsonProcessingException e) {
            logger.error("Can't convert the data back to JSON!", e);
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR, "Can't convert the data back to JSON!");
        }

        messageAttributes.put("SCAC", new MessageAttributeValue()
                .withDataType("String")
                .withStringValue(railroadsService.getSCACbyMARK(currentUser.getUsername(), payloadMap.get("SCACMark"))));

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
    
    public Date tzModifiedDate(Date unModifiedDate, String stringOffset, String stringDst) {
    	int offset = Integer.parseInt(stringOffset);
    	Boolean dst = Boolean.parseBoolean(stringDst);

    	//Apply DST to everything except UTC
    	if (dst==true && offset != 0) {
    		offset = offset - 1;    		
    	}

    	Calendar calendar = Calendar.getInstance();
        calendar.setTime(unModifiedDate);
        calendar.add(Calendar.HOUR_OF_DAY, offset);
        return calendar.getTime();
    }
}
