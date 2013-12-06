package com.collabdraw.sessionmanager;

import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.json.simple.JSONArray;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.MongoClient;

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
			  // Add new server to the database
			  MongoClient mongo = new MongoClient();
			  DB db = mongo.getDB("CollabDraw");
			  DBCollection table = db.getCollection("servers");
			  BasicDBObject document = new BasicDBObject();
			  document.put("serverIP",workerServerIp);
			  document.put("status","reachable");
			  document.put("lastKnowHeartbeat",System.currentTimeMillis());
			  table.insert(document);
			  out.println("Added new worker server");
		  }
	  }
	  else if (query.compareToIgnoreCase("getServerStats") == 0){
		  MongoClient mongo = new MongoClient();
		  DB db = mongo.getDB("CollabDraw");
		  DBCollection table = db.getCollection("servers");
		  BasicDBObject fields = new BasicDBObject();
		  fields.append("_id",false);
		  DBCursor cursor = table.find(null,fields);
		  JSONArray jsonArray = new JSONArray();
		  
		  while (cursor.hasNext()) {
			  jsonArray.add(cursor.next());
			}
		out.println(jsonArray.toJSONString());
	  }
	  else {
		  out.println("Undefined server operation");  
	  }
   }
}