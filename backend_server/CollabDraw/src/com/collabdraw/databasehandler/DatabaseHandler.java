package com.collabdraw.databasehandler;

import java.net.UnknownHostException;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.MongoClient;


/*
 * NOTE: This class is not used at the moment
 * I intend to move db functionalities to this class later
 */
public class DatabaseHandler {
	public String getJsonString(){
		return null;
	}
	
	void setupConnection(){
		try {
			 MongoClient mongo = new MongoClient();
			  DB db = mongo.getDB("CollabDraw");
			  DBCollection table = db.getCollection("servers");
			  BasicDBObject document = new BasicDBObject();
			  document.put("serverIp","10.20.0.1");
			  document.put("status","reachable");
			  document.put("lastKnowHeartbeat",System.currentTimeMillis());
			  table.insert(document);
			
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
	}
	
	public static void main(String args[]){
		DatabaseHandler dbHandler = new DatabaseHandler();
		dbHandler.setupConnection();
	}
}
