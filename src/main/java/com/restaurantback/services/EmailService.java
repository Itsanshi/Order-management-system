package com.restaurantback.services;

import com.amazonaws.services.simpleemail.AmazonSimpleEmailService;
import com.amazonaws.services.simpleemail.model.RawMessage;
import com.amazonaws.services.simpleemail.model.SendRawEmailRequest;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.Properties;

public class EmailService {

    private final AmazonSimpleEmailService sesClient;

    public EmailService(AmazonSimpleEmailService sesClient) {
        this.sesClient = sesClient;
    }

    public void sentReportEmail(byte[] staffReport, byte[] locationReport, String sender, String recipient, String reportPeriod) {

        try {
            System.out.println("Preparing For sending email... ");

            String subject = "Performance Report - " + reportPeriod;
            String textBody = "Please find attached the performance report for " + reportPeriod + ".";
            String htmlBody = "<html><body><h2>Performance Report</h2>"
                    + "<p>Please find attached the performance report for " + reportPeriod + ".</p>"
                    + "<p>This report contains both staff performance metrics and sales performance metrics.</p>"
                    + "<p>Thank you,<br>Green And Tasty Restaurant</p>"
                    + "</body></html>";

            // Create attachment
//            String filename = "Performance_Report_" + reportPeriod.replace(" to ", "_") + ".xlsx";
            String staffFilename = "Staff_Performance_Report_" + reportPeriod.replace(" to ", "_") + ".xlsx";
            String locationFilename = "Location_Performance_Report_" + reportPeriod.replace(" to ", "_") + ".xlsx";

            SendRawEmailRequest sendRawEmailRequest = createEmailWithAttachment(
                    sender,
                    recipient,
                    subject,
                    textBody,
                    htmlBody,
                    staffReport,
                    locationReport,
                    staffFilename,
                    locationFilename
            );

            sesClient.sendRawEmail(sendRawEmailRequest);
            System.out.println("Email Sent.");
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("error sending email", e);
        }
    }

    private SendRawEmailRequest createEmailWithAttachment(
            String sender,
            String recipient,
            String subject,
            String textBody,
            String htmlBody,
            byte[] staffAttachment,
            byte[] locationAttachment,
            String waiterFilename,
            String locationFilename
    ) {
        try {

            Session session = Session.getDefaultInstance(new Properties());
            MimeMessage message = new MimeMessage(session);

            message.setSubject(subject, "UTF-8");
            message.setFrom(new InternetAddress(sender));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipient));

            MimeMultipart msgBody = new MimeMultipart("alternative");
            MimeBodyPart textPart = new MimeBodyPart();
            textPart.setContent(textBody, "text/plain; charset=UTF-8");

            MimeBodyPart htmlPart = new MimeBodyPart();
            htmlPart.setContent(htmlBody, "text/html; charset=UTF-8");

            msgBody.addBodyPart(textPart);
            msgBody.addBodyPart(htmlPart);

            // Wrap the multipart/alternative in a multipart/mixed with attachment
            MimeMultipart msg = new MimeMultipart("mixed");
            MimeBodyPart wrap = new MimeBodyPart();
            wrap.setContent(msgBody);
            msg.addBodyPart(wrap);

            // Add the attachment
//            MimeBodyPart attachment_part = new MimeBodyPart();
//            attachment_part.setContent(attachment, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
//            attachment_part.setFileName(filename);
//            msg.addBodyPart(attachment_part);

            MimeBodyPart waiterReportAttachment = createAttachmentFiles(staffAttachment, waiterFilename);
            msg.addBodyPart(waiterReportAttachment);

            MimeBodyPart locationReportAttachment = createAttachmentFiles(locationAttachment, locationFilename);
            msg.addBodyPart(locationReportAttachment);

            message.setContent(msg);

            // Create raw message
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            message.writeTo(outputStream);

            RawMessage rawMessage = new RawMessage(ByteBuffer.wrap(outputStream.toByteArray()));

            SendRawEmailRequest sendRawEmailRequest = new SendRawEmailRequest(rawMessage);
            System.out.println("raw email request: " + sendRawEmailRequest);

            return sendRawEmailRequest;

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error in creating email with attachment.", e);
        }
    }

    private MimeBodyPart createAttachmentFiles(byte[] attachment, String filename) throws MessagingException {
        MimeBodyPart attachment_part = new MimeBodyPart();
        attachment_part.setContent(attachment, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        attachment_part.setFileName(filename);

        return attachment_part;
    }

}
