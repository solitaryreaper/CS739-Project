package com.collabdraw.sessionmanager;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.UnknownHostException;
import java.util.StringTokenizer;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.json.simple.JSONArray;

import com.collabdraw.databasehandler.DatabaseHandler;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

class Server {
String ip;
int load;
}

@WebServlet("/serverOps")  // URL for this Servlet
public class SessionManager extends HttpServlet {
   private static final long serialVersionUID = 1L;
   
   /*
    *  This method analyzes the arguments passed to server to perform required actions
    */
   @Override
   protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
	  response.setContentType("text/html");
	  PrintWriter out = response.getWriter();
	  String query = request.getParameter("operation");  
	  
	  // Handle HeartBeat
	  if (query.compareToIgnoreCase("getHeartBeat") == 0){
		  out.println(System.currentTimeMillis());  
	  }
	  // Handle Worker Server registration
	  else if (query.compareToIgnoreCase("register") == 0){
		  String workerServerIp = request.getParameter("ip");
		  if (workerServerIp == null){
			  out.println("Error!! Please pass worker server IP");
		  }
		  else{
			  // Check if the server is already in DB
			  DB db = DatabaseHandler.getDatabaseHandle();
			  DBCollection table = db.getCollection("Servers");
			  BasicDBObject dbQuery = new BasicDBObject("serverIP",workerServerIp);
			  DBCursor cursor = table.find(dbQuery);
			  if (cursor.count() > 0){
				  out.println("Server already in database.");
			  }
			  else{
			  // Add new server to the database
			  BasicDBObject document = new BasicDBObject();
			  document.put("serverIP",workerServerIp);
			  document.put("status","reachable");
			  document.put("lastKnowHeartbeat",System.currentTimeMillis());
			  document.put("preferredServerFor",0);
			  document.put("clientLoad",0);
			  table.insert(document);
			  out.println("Added new worker server");
			  }
		  }
	  }
	  else if (query.compareToIgnoreCase("getServerStats") == 0){
		  DB db = DatabaseHandler.getDatabaseHandle();
		  DBCollection table = db.getCollection("Servers");
		  BasicDBObject fields = new BasicDBObject();
		  fields.append("_id",false);
		  DBCursor cursor = table.find(null,fields);
		  JSONArray jsonArray = new JSONArray();
		  
		  while (cursor.hasNext()) {
			  jsonArray.add(cursor.next());
			}
		out.println(jsonArray.toJSONString());
	  }
	  
	  else if (query.compareToIgnoreCase("getUserTable") == 0){
		  DB db = DatabaseHandler.getDatabaseHandle();
		  DBCollection table = db.getCollection("Users");
		  BasicDBObject fields = new BasicDBObject();
		  fields.append("_id",false);
		  DBCursor cursor = table.find(null,fields);
		  JSONArray jsonArray = new JSONArray();
		  
		  while (cursor.hasNext()) {
			  jsonArray.add(cursor.next());
			}
		out.println(jsonArray.toJSONString());
	  }
	  
	  else if (query.compareToIgnoreCase("getSessionTable") == 0){
		  DB db = DatabaseHandler.getDatabaseHandle();
		  DBCollection table = db.getCollection("Sessions");
		  BasicDBObject fields = new BasicDBObject();
		  fields.append("_id",false);
		  DBCursor cursor = table.find(null,fields);
		  JSONArray jsonArray = new JSONArray();
		  
		  while (cursor.hasNext()) {
			  jsonArray.add(cursor.next());
			}
		out.println(jsonArray.toJSONString());
	  }
	  
	  else if (query.compareToIgnoreCase("getWorkerServer") == 0){
		  String sessionId = request.getParameter("sessionId");
		  String userId = request.getParameter("userId");
		  if (sessionId == null || userId == null){
			  out.println("Invalid Arguments.Missing sessionId and/or userId");
		  }
		  else{
			  /*
			   * Algorithm - 
			   * 1) Check if key "sessionId":"userId" is present in Users Table
			   * 2) If present, move to step 4 
			   * 3) If not present, query Sessions table with sessionId key
			   *   3.a) If found, we have a new user. Update the users for this session
			   *   3.b) If not found, create a new entry for this session
			   * 4) Find the primary server for this user and return it
			   */
			  
			  //Check if sessionId:userId is present in Users table 
			  DB db = DatabaseHandler.getDatabaseHandle();
			  DBCollection sessionsTable = db.getCollection("Sessions");
			  DBCollection serversTable = db.getCollection("Servers");
			  DBCollection usersTable = db.getCollection("Users");
			  BasicDBObject dbQuery = new BasicDBObject("SessionId:UserId",sessionId+":"+userId);
			  DBCursor cursor = usersTable.find(dbQuery);
			  boolean userFound = false;
			  String primaryFromUserTable = null;
			  if (cursor.count() > 0){
				  // Entry found, move to primary server selection
				  userFound = true;
				  primaryFromUserTable = cursor.next().get("PrimaryServer").toString();
			 }
			  else{
				  // Check if sessionId exists in Sessions Table
				  dbQuery = new BasicDBObject("SessionId",sessionId);
				  cursor = sessionsTable.find(dbQuery);
				  if (cursor.count() > 0){
					  // Session found. Update users for this session
					  sessionsTable.update(cursor.next(), new BasicDBObject("$push", new BasicDBObject("Users",userId)));  
				  }
				  else{
					  // Insert new entry in Sessions table
					  
					  BasicDBList serverList = new BasicDBList();
					  DBCursor preferredServers = getPreferredServers();
					  String serverIP;
					  int preferredServerCount;
					  while (preferredServers.hasNext()){
						  DBObject object = preferredServers.next();
						  serverIP = object.get("serverIP").toString();
						  preferredServerCount = Integer.parseInt(object.get("preferredServerFor").toString());
						  serverList.add(serverIP);
						  updateValues(serversTable,"serverIP",serverIP,"preferredServerFor",null,preferredServerCount+1);
					  }
					  BasicDBList userList = new BasicDBList();
					  userList.add(userId);
					  BasicDBObject document = new BasicDBObject();
					  document.put("SessionId",sessionId);
					  document.put("PreferredServers",serverList);
					  document.put("Users",userList);
					  sessionsTable.insert(document);
				  }
			  }
			  // At this stage, we have Session and User entry in Sessions table
			  // Next, we find the least loaded server from preferred Servers for this session
			  String returnStatus = null;
			  if (!userFound){
				  Server primaryServer = getPrimaryServer(sessionId,userId); 
				  if (primaryServer.ip == null){
					  returnStatus = "DISCONNECTED";
				  }
				  else{
					  returnStatus = primaryServer.ip;
				  }
				  // Insert new entry in Users Table
				  BasicDBObject document = new BasicDBObject();
				  document.put("SessionId:UserId",sessionId+":"+userId);
				  document.put("PrimaryServer",returnStatus);
				  usersTable.insert(document);
			      // Update clientLoad in Servers Table only if primary Ip is present
				  if (primaryServer.ip != null ){
					 updateValues(serversTable, "serverIP",returnStatus, "clientLoad",null, primaryServer.load+1);
				  }
				  out.println(returnStatus);
			  }
			  else{
			      // Check if the "primaryFromUserTable" is active
			      boolean reachable = true;
				  if (primaryFromUserTable.compareTo("DISCONNECTED") == 0){
					  reachable = false;
				  }
				  else{
					  BasicDBObject searchQuery = new BasicDBObject("serverIP",primaryFromUserTable);
					  String status = serversTable.findOne(searchQuery,null).get("status").toString();
					  if (status.compareTo("unreachable") == 0){
						  reachable = false;
					  }
				  }
				  // If server is reachable, simply return the ip
				  if(reachable){
					  out.println(primaryFromUserTable);
				  }
				  else{
					// Find a new primary server
					Server primaryServer = getPrimaryServer(sessionId,userId);
					if (primaryServer.ip == null){
						  returnStatus = "DISCONNECTED";
					  }
					  else{
						  returnStatus = primaryServer.ip;
					  }
					  // Update entry in Users Table
					  updateValues(usersTable,"SessionId:UserId",sessionId+":"+userId,"PrimaryServer",returnStatus,-1);
					  
				      // Update clientLoad in Servers Table only if primary Ip is present
					  if (primaryServer.ip != null ){
						 updateValues(serversTable, "serverIP",returnStatus, "clientLoad",null, primaryServer.load+1);
					  }
					  out.println(returnStatus);
				  }
			 }
		  }
		  }
	  else {
		  out.println("Undefined server operation");  
	  }
   }
   
   /*
    * This method returns two preferred Servers from Servers table
    * It finds two live servers with minimum value of "preferredServerFor".
    */
   DBCursor getPreferredServers() throws UnknownHostException{
	  DB db = DatabaseHandler.getDatabaseHandle();
	  DBCollection serverTable = db.getCollection("Servers");
	  BasicDBObject fields = new BasicDBObject();
	  fields.append("_id",false);
	  BasicDBObject searchQuery = new BasicDBObject("status", "reachable");
	  DBCursor cursor = serverTable.find(searchQuery,fields).sort(new BasicDBObject("preferredServerFor",1)).limit(2);
	  return cursor;
   }
   
   void updateValues(DBCollection table,String searchField, String searchValue,String updateField, String newStringValue, int newIntValue){
	   BasicDBObject newDocument = new BasicDBObject();
	   if (newStringValue == null){
		   newDocument.append("$set", new BasicDBObject().append(updateField,newIntValue));
	   }
	   else{
		   newDocument.append("$set", new BasicDBObject().append(updateField,newStringValue));
	   }
	   BasicDBObject searchQuery = new BasicDBObject().append(searchField,searchValue);
	   table.update(searchQuery, newDocument);
   }
   
   /*
    * This method finds least loaded available server from preferred server list of a given session.
    * If no available servers are found, it returns null
    */
   Server getPrimaryServer(String sessionId, String userId) throws UnknownHostException{
	   DB db = DatabaseHandler.getDatabaseHandle();
	   DBCollection sessionsTable = db.getCollection("Sessions");
	   BasicDBObject searchQuery = new BasicDBObject().append("SessionId",sessionId);
	   String serverList = sessionsTable.findOne(searchQuery).get("PreferredServers").toString();
	   serverList = serverList.replaceAll("\\s+","").replaceAll("\"","").replace("[","").replace("]","");
	   StringTokenizer st = new StringTokenizer(serverList,",");
	   DBCollection serversTable = db.getCollection("Servers");
	   int primaryLoad = 100;
	   String primaryIp = null;
	   int serverLoad;
	   while (st.hasMoreTokens()){
		   serverLoad = -1;
		   String curServer = st.nextElement().toString();
		   searchQuery = new BasicDBObject("status", "reachable").append("serverIP", curServer);
		   DBObject result = serversTable.findOne(searchQuery);
		   if (result != null){
		   serverLoad = Integer.parseInt(result.get("clientLoad").toString());
		   }
		   if (serverLoad < primaryLoad && serverLoad != -1){
			primaryLoad = serverLoad;
			primaryIp = curServer;
		}
	   }
	   Server primaryServer = new Server();
	   primaryServer.ip = primaryIp;
	   primaryServer.load = primaryLoad;
	   return primaryServer;
   	}
}