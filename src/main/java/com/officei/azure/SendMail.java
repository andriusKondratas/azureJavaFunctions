package com.officei.azure;

import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;
import org.apache.commons.lang3.StringUtils;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.Optional;
import java.util.Properties;


/**
 * Azure Functions with HTTP Trigger.
 */
public class SendMail {

    /**
     * This function listens at endpoint "/api/HttpTrigger-Java". Two ways to invoke it using "curl"
     * command in bash: 1. curl -d "HTTP Body" {your host}/api/HttpTrigger-Java&code={your function
     * key} 2. curl "{your host}/api/HttpTrigger-Java?name=HTTP%20Query&code={your function key}"
     * Function Key is not needed when running locally, it is used to invoke function deployed to
     * Azure. More details: https://aka.ms/functions_authorization_keys
     */
    @FunctionName("sendMail")
    public HttpResponseMessage run(@HttpTrigger(name = "req", methods = {HttpMethod.GET,
            HttpMethod.POST}, authLevel = AuthorizationLevel.FUNCTION) HttpRequestMessage<Optional<String>> request,
                                   final ExecutionContext context) {
        context.getLogger().info("Java HTTP trigger processed a request.");

        // Parse query parameters
        String to = request.getQueryParameters().get("to");
        String subject = request.getQueryParameters().get("subject");
        String body = request.getQueryParameters().get("body");
        String smtpHost = request.getQueryParameters().get("smtpHost");
        String smtpPort = request.getQueryParameters().get("smtpPort");

        String query = request.getQueryParameters().get("name");
        String name = request.getBody().orElse(query);

        if (name == null) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body("Please pass a name on the query string or in the request body").build();
        } else {
            try {
                String smtpHostServer =
                        StringUtils.isEmpty(smtpHost) ? System.getenv("SMTP_HOST_NAME") : smtpHost;
                String smtpHostPort = StringUtils.isEmpty(smtpPort) ? System.getenv("SMTP_PORT") : smtpPort;

                Properties props = System.getProperties();
                props.put("mail.smtp.host", smtpHostServer);
                props.put("mail.smtp.port", smtpHostPort);

                Session session = Session.getInstance(props, null);
                sendEmail(session, to, subject, body, context);

            } catch (MessagingException | UnsupportedEncodingException e) {
                context.getLogger().info("\nGot error:\n\t" + e.getMessage());
                return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Got Error. " + e.getMessage()).build();
            }
            return request.createResponseBuilder(HttpStatus.OK).body("Success").build();
        }
    }

    private static void sendEmail(Session session, String toEmail, String subject, String body,
                                  final ExecutionContext context) throws MessagingException, UnsupportedEncodingException {
        MimeMessage msg = new MimeMessage(session);
        //set message headers
        msg.addHeader("Content-type", "text/HTML; charset=UTF-8");
        msg.addHeader("format", "flowed");
        msg.addHeader("Content-Transfer-Encoding", "8bit");
        msg.setFrom(new InternetAddress("no_reply@example.com", "NoReply"));
        msg.setReplyTo(InternetAddress.parse("no_reply@example.com", false));
        msg.setSubject(subject, "UTF-8");
        msg.setText(body, "UTF-8");
        msg.setSentDate(new Date());
        msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail, false));
        context.getLogger().info("Message is ready");
        Transport.send(msg);
        Transport.send(msg);
        Transport.send(msg);
        Transport.send(msg);
        context.getLogger().info("Email Sent Successfully");
    }
}


