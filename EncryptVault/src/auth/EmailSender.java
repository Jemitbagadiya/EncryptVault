package auth;

import java.util.Properties;
import javax.mail.*;
import javax.mail.internet.*;

public class EmailSender {

    public static void sendOTP(String to, String otp) {

        // Sender email credentials
        final String from = "encryptvaultj@gmail.com";
        final String pass = System.getenv("EMAIL_PASS");

        // Configure SMTP properties for Gmail
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");

        // Create mail session with authentication
        Session session = Session.getInstance(
            props,
            new Authenticator() {
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(from, pass);
                }
            }
        );

        try {
            // Create email message
            Message message = new MimeMessage(session);

            // Set sender address
            message.setFrom(new InternetAddress(from));

            // Set recipient email address
            message.setRecipients(
                Message.RecipientType.TO,
                InternetAddress.parse(to)
            );

            // Set email subject
            message.setSubject("EncryptVault OTP");
            
            // Define HTML content for OTP email
            String emailContent =
                "<html>" +
                "<body style='font-family: Arial, sans-serif;'>" +
                "<p>Dear User,</p>" +
                "<p>We received a request to verify your identity for your EncryptVault account.</p>" +
                "<p>Your One-Time Password (OTP) is:</p>" +
                "<h2 style='text-align:center; color:#2c3e50;'>" + otp + "</h2>" +
                "<p><b>This OTP is valid for 3 minutes.</b></p>" +
                "<p>For your security, please do not share this OTP with anyone.</p>" +
                "<p>If you did not request this, please ignore this email.</p>" +
                "<br>" +
                "<p>Regards,<br>EncryptVault Security Team</p>" +
                "</body>" +
                "</html>";

            // Set email content as HTML
            message.setContent(emailContent, "text/html");

            // Send the email
            Transport.send(message);

            // Log success message
            System.out.println("OTP sent successfully");

        } catch (Exception e) {
            // Handle any exceptions during email sending
            e.printStackTrace();
        }
    }
}