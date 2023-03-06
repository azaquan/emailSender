package com.ims.app;

import java.util.Properties;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.HashMap;
import java.io.File;
import java.sql.Connection;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.Transport;
import javax.activation.*;
import java.util.Date;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Utils{
	private static final Logger logger = LogManager.getLogger(Utils.class);
	
   public static Properties getProperties(String path){
      Properties props = new Properties();
      FileInputStream input = null;
      String fileName = "emailSender.properties";
      try{
         input = new FileInputStream(path+"resources/"+fileName);
         if(input==null){
            System.out.println("Sorry, unable to find "+fileName);
            return props;
         }
         props.load(input);
      }catch(IOException e){
         System.out.println("No properties could be loaded:");
         e.printStackTrace();
      }finally{
         if (input != null) {
            try {
               input.close();
            } catch (IOException e) {
               e.printStackTrace();
            }
         }
      }
      return props;
   }

	public static int stringToInt(String value, int _default) {
		 try {
			  return Integer.parseInt(value);
		 } catch (NumberFormatException e) {
			  return _default;
		 }
	}
	
   public static boolean sendEmail(Connection conn, Properties props, Map<String, String> emailMap){
   	boolean sent = false;
   	String attachment = emailMap.get("emailAttachmentFile");
		String to = emailMap.get("emailRecipientStr");
		String msgText = emailMap.get("emailBody");
		logger.debug("msgText - - - - ->{}", msgText);

		String subject = emailMap.get("emailSubject");
		
		Map<String, String> map = DbUtils.getEmailConfig(conn);
		if(map!=null){
			String host = map.get("smtp");
			String from = map.get("fromEmail");
			String fromName = map.get("fromName");
			String mailer = map.get("fromName");
			String port = map.get("port");
			String pass = map.get("pass");
			Properties emailProps = System.getProperties();
			
			emailProps.put("mail.debug", "true");

			emailProps.put("mail.transport.protocol", "smtp");

			emailProps.put("mail.smtp.auth", "true");
			
			emailProps.put("mail.smtp.host", host);	
			emailProps.put("mail.from", from);
			emailProps.put("mail.smtp.starttls.enable", "true");
			emailProps.put("mail.smtp.port", port);
			emailProps.put("mail.smtp.ssl.trust", host);
			emailProps.put("mail.smtp.ssl.protocols", "TLSv1.2");
			
			emailProps.setProperty("mail.debug", "false");
			Session session = Session.getInstance(emailProps, null);
			try {
				MimeMessage msg = new MimeMessage(session);
				msg.setFrom(new InternetAddress(from, fromName));
				msg.addRecipients(Message.RecipientType.TO,(InternetAddress.parse(to))); 
				msg.setSubject(subject);
				msg.setSentDate(new Date());
				//msg.settestText(msgText,"text/plain");
				 
				Multipart multipart = new MimeMultipart();
				MimeBodyPart textPart = new MimeBodyPart();
				textPart.setContent(msgText, "text/plain");
				MimeBodyPart attachementPart = new MimeBodyPart();
				attachementPart.attachFile(new File(attachment));
				multipart.addBodyPart(textPart);
				multipart.addBodyPart(attachementPart);
				msg.setContent(multipart);  
				 
				Transport transport = session.getTransport("smtp");
				transport.connect(from, pass);
				transport.sendMessage(msg, msg.getAllRecipients());
				transport.close();
				sent = true;
			}catch (MessagingException e) {
				logger.error("Error while trying to sendEmail: {}", e.getMessage());
			}catch (IOException e){
				logger.error("Error while trying to get attachment file: {}", e.getMessage());
			}
		}
		return sent;
   }
}