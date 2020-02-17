package net.railwaynet.logdelivery.strolr;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
public class LogsController {
    private static final Logger logger = LoggerFactory.getLogger(LogsController.class);

    private static final String QUEUE_NAME = "dev_strolrlogrequest";

    @RequestMapping(
            value = "/request-logs",
            method = RequestMethod.POST)
    public String requestLogs(Principal principal, @RequestBody String payload) {
        UserDetails currentUser = (UserDetails) ((Authentication) principal).getPrincipal();
        logger.debug(currentUser.getUsername() + " requesting logs");

        logger.debug(payload);

        BasicAWSCredentials bAWSc = new BasicAWSCredentials("AKIAZZT54LR2UM7XFBET", "mW8/hHj/NafsEZqJNaAnzoRSjruT/FUbQZ2YxF56");
        AmazonSQS sqs = AmazonSQSClientBuilder.standard().withRegion("us-east-1").withCredentials(new AWSStaticCredentialsProvider(bAWSc)).build();

        String queueUrl = sqs.getQueueUrl(QUEUE_NAME).getQueueUrl();

        SendMessageRequest send_msg_request = new SendMessageRequest()
                .withQueueUrl(queueUrl)
                .withMessageBody(payload)
                .withDelaySeconds(5);
        sqs.sendMessage(send_msg_request);

        return "OK";
    }

}
