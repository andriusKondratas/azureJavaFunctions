package com.officei.azure;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpMethod;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.Optional;
import java.util.Properties;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import org.apache.commons.lang3.StringUtils;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;


/**
 * Azure Functions with HTTP Trigger.
 */
public class SendMailSpring {

  /**
   * This function listens at endpoint "/api/HttpTrigger-Java". Two ways to invoke it using "curl"
   * command in bash: 1. curl -d "HTTP Body" {your host}/api/HttpTrigger-Java&code={your function
   * key} 2. curl "{your host}/api/HttpTrigger-Java?name=HTTP%20Query&code={your function key}"
   * Function Key is not needed when running locally, it is used to invoke function deployed to
   * Azure. More details: https://aka.ms/functions_authorization_keys
   */
  @FunctionName("sendMailSpring")
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


        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(smtpHostServer);

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.debug", "true");



        //Properties props = System.getProperties();
        //props.put("mail.smtp.host", smtpHostServer);
        //props.put("mail.smtp.port", smtpHostPort);

        Session session = Session.getInstance(props, null);
        sendEmail(session, to, subject, body, context, mailSender);

      } catch (MessagingException | UnsupportedEncodingException e) {
        context.getLogger().info("\nGot error:\n\t" + e.getMessage());
        return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
            .body("Got Error. " + e.getMessage()).build();
      }
      return request.createResponseBuilder(HttpStatus.OK).body("Success").build();
    }
  }

  private static void sendEmail(Session session, String toEmail, String subject, String body,
      final ExecutionContext context, JavaMailSenderImpl mailSender) throws MessagingException, UnsupportedEncodingException {
    MimeMessage mimeMessage = mailSender.createMimeMessage();
    MimeMessageHelper messageHelper = new MimeMessageHelper(mimeMessage, "utf-8");
    messageHelper.setFrom("no_reply@example.com", "Test");
    messageHelper.setTo(toEmail);
    messageHelper.setSubject(subject);
    messageHelper.setText(body, false);
    context.getLogger().info("Message is ready");
    mailSender.send(messageHelper.getMimeMessage());
    context.getLogger().info("Email Sent Successfully");
  }
}


