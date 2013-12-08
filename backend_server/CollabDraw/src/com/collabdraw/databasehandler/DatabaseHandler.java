package com.collabdraw.databasehandler;

import java.net.UnknownHostException;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.MongoClient;
public class DatabaseHandler {
	private static DB db;
	
	public static DB getDatabaseHandle() throws UnknownHostException{
		if (db == null){
			MongoClient mongo = new MongoClient();
			db = mongo.getDB("CollabDraw");
		}
		return db;
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
