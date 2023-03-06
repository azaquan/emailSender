package com.ims.app;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.CallableStatement;
import java.sql.Timestamp;
import java.io.IOException;
import java.io.FileInputStream;
import java.text.SimpleDateFormat;
import java.util.Map;
import java.util.HashMap;
import java.util.Properties;
import java.lang.Integer;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.LocalDate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class App {
	private static final Logger logger = LogManager.getLogger(App.class);

	DateTime now = new DateTime();
	DateTimeFormatter fmtTime=DateTimeFormat.forPattern("yyyy-MM-dd_HH-mm-ss");
	DateTimeFormatter fmt=DateTimeFormat.forPattern("yyyy-MM-dd");	
	
    public static  void main(String[] args) {
    	run();
    }
    
    private static void run(){
    	String path = System.getProperty("java.class.path");
        int lastPoint = path.indexOf("/")+path.indexOf("\\");
        logger.info("_____________00 path:{}",path);	
        path = path.substring(0,lastPoint+2);
        logger.debug("Into run");
        Properties props = Utils.getProperties(path);
        Connection conn = DbUtils.getConnection(path, props);
        if(pendingEmails(conn, props)){
        	logger.info("ready");
        }
    }
    
    private static boolean pendingEmails(Connection conn, Properties props){
    	logger.debug("Into pendingEmails");
        String query = "select * from EmailFax where Send is NULL and PDFFile is null";
        try{
            ResultSet result = DbUtils.getResultSet(conn, query);
            if(result==null){
            	logger.debug("Zero pending emails to be sent");
            	return false;
            }else{
            	if(result.next()){
					logger.debug("result has data");
					while(result.next()){
						logger.debug("reaading data...");
						String attachmentFile = result.getString("attachmentFile");
						logger.debug("attachmentFile={}",attachmentFile);
						
						String pdfFile = result.getString("PDFFile");
						logger.debug("pdfFile={}",pdfFile);
						if(attachmentFile.equals(pdfFile)){
							if(attachmentFile==null){
								return true;
							}else{
								logger.debug("attachmentFile={}", attachmentFile);
							}
						}else{
							processEmails(conn, props, result);
						}
					}
				}
            }
        }catch(SQLException e){
        	logger.error("Error while trying to get valid emails: {}",e.getMessage());
        }
        return false;
    }
    
    private static void processEmails (Connection conn, Properties props, ResultSet result) throws SQLException{
    	String errorDetails ="";
    	try{
			String emailSubject= result.getString("Subject");
			String emailBody=result.getString("Body");
			String emailAttachmentFile=result.getString("AttachmentFile");
			String emailRecipientStr=result.getString("RecepientStr");
			String emailCreaUser=result.getString("creauser");
			
			logger.debug("rowid={}",result.getLong("rowid"));
	
			Map<String, String> map = new HashMap<String, String>();
			map.put("emailSubject", emailSubject);
			map.put("emailBody", emailBody);
			map.put("emailAttachmentFile", emailAttachmentFile);
			map.put("emailRecipientStr", emailRecipientStr);
			map.put("emailCreaUser", emailCreaUser);
			
			if(Utils.sendEmail(conn, props, map)){
				logger.info("The email has been routed to be sent succesfully");
			}else{
				logger.debug("Something went wrong.  The email was not routed to be sent");
			}
		}catch(SQLException e){
			logger.error("Error while trying to processEmails: {}", e);
			errorDetails=e.getMessage();
		}finally{
			registerSendingEmail(conn, result, errorDetails);
		}
    }
    //update Emailfax set send=@send, ErrorDetails = @ErrorDetails, PDFFile =@PDFFile where rowid = @rowid
    private static void registerSendingEmail(Connection conn, ResultSet result, String errorDetails) throws SQLException{
    CallableStatement cs = null;
		try {
			cs = conn.prepareCall("{call UpdateEmailFaxSendFlag(?, ?, ?, ?)}");
			cs.setFloat(1, result.getLong("rowid"));
			cs.setTimestamp(2, new Timestamp(System.currentTimeMillis()));
			cs.setString(3, result.getString("AttachmentFile"));
			cs.setString(4, errorDetails);
			cs.executeUpdate();
		} catch (SQLException e) {
			logger.error("Error while trying to registerSendingEmail: {}", e);
		} finally {
			if (cs != null) {
				cs.close(); 
			}
		}
    }
}
