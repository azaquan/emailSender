package com.ims.app;

import java.lang.Exception;
import java.sql.*;
import java.sql.CallableStatement;
import java.util.Properties;
import java.util.Map;
import java.util.HashMap;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.ClassNotFoundException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class DbUtils{
	private static final Logger logger = LogManager.getLogger(App.class);
	
   public static Connection getConnection(String path, Properties props){
      DbUtils dbUtils = new DbUtils();
      Connection conn = null;
      try{
         if(props==null){
            System.out.println("No properties found");
         }else{
            conn = DriverManager.getConnection(
               "jdbc:sqlserver:"+props.getProperty("jdbc.server")+";databaseName="+props.getProperty("jdbc.database"),
               props.getProperty("jdbc.user"),
               props.getProperty("jdbc.password")
            );
         }
      }catch(SQLException e){
         System.out.println("from the server: "+e.getMessage());
      }
      return conn;
   }


   public static ResultSet getResultSet(Connection conn, String query){
      ResultSet result = null;
      logger.debug("query: {}",query);
      try{
         Statement stmt = conn.createStatement();
         result = stmt.executeQuery(query);
      }catch(SQLException e){
         logger.error("Error while running getResultSet: {}",e.getMessage());
      }
      return result;
   }


   public static boolean sendEmail(Connection dbConnection, Map<Integer,String> param){
      boolean sent = false;
      String insertStoreProc = "{call InsertEmailFax(?,?,?,?,?)}";
      CallableStatement callableStatement = null;
 		try {
         callableStatement = dbConnection.prepareCall(insertStoreProc);
         for(int i=1;i<param.size()+1;i++){
            callableStatement.setString(i, param.get(i));
         }
         callableStatement.executeUpdate();
         sent = true;
		} catch (SQLException e) {
			System.out.println(e.getMessage());
		} finally {
		   try{
		      callableStatement.close();
         } catch (SQLException e) {
            System.out.println(e.getMessage());
         }
		}
		return sent;
   }
   
   public static String getUserEmail(Connection dbConnection, String namespace, String xUser){
   	System.out.println("getUserEmail-->"+xUser);
   	String query = "select * from xuserprofile where usr_npecode = '"+namespace+"' and usr_userid='"+xUser+"'  ";
   	String email = "";
		ResultSet result = DbUtils.getResultSet(dbConnection, query);
		if(result==null){                            
		}else{
			try{
				while(result.next()){
					email=result.getString("email");
					break;
				}
			}catch (SQLException e) {
            System.out.println(e.getMessage());
         }
		}
		System.out.println("getUserEmail email -->"+email);
		return email;
   }
   
   public static Map<String, String> getEmailConfig(Connection dbConnection){
   	String query = "select top 1 * from EmailFaxConfig order by orderindex";
   	Map<String, String> map = new HashMap<String, String>();
		ResultSet result = DbUtils.getResultSet(dbConnection, query);
		if(result==null){
			System.out.println("getUserEmail() has been executed!");                                      
		}else{
			try{
				while(result.next()){
					map.put("fromEmail", result.getString("FromEmail"));
					map.put("fromName", result.getString("FromName"));
					map.put("pass", result.getString("password"));
					map.put("smtp", result.getString("SMTP"));
					map.put("port", result.getString("SendingPort"));
					break;
				}
			}catch (SQLException e) {
            System.out.println(e.getMessage());
         }
		}
		return map;
   }
}