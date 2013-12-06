package com.collabdraw.statsupdater;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;

class Server{
	 String ip;
	 String status;
	 long heartBeat;
}


/*
 * Methods defined in this class would run as daemon on session manager
 * to update stats in the database
 */

public class StatsHandler {
	 List<Server> servers;
	 
	 public StatsHandler(){
		 servers = new ArrayList<Server>();
	 }
	
	// This method reads the list of servers from db
	public void getServerList() throws UnknownHostException{
		MongoClient mongo = new MongoClient();
		DB db = mongo.getDB("CollabDraw");
		DBCollection table = db.getCollection("servers");
		BasicDBObject fields = new BasicDBObject();
		fields.append("_id",false);
		DBCursor cursor = table.find(null,fields);
		while (cursor.hasNext()) {
			DBObject dbObject = cursor.next(); 
			Server server = new Server();
			server.ip = (String) dbObject.get("serverIP");
			server.heartBeat = (long) dbObject.get("lastKnowHeartbeat");
			server.status = (String) dbObject.get("status");
			servers.add(server);
			}
		mongo.close();
	}
	
	// This method pings servers to get their latest stats
	@SuppressWarnings({ "resource", "deprecation" })
	public void getServerStats() {
		HttpClient client = new DefaultHttpClient();
		HttpGet request;
		HttpResponse response;
		HttpParams params = client.getParams();
		HttpConnectionParams.setConnectionTimeout(params,500);
		for (Server server: servers){
			 String url = "http://" + server.ip + ":8080" +
					      "/CollabDraw/serverOps?operation=getheartbeat";
			try {
			request = new HttpGet(url);
			response = client.execute(request);
			BufferedReader rd = new BufferedReader(
					new InputStreamReader(response.getEntity().getContent()));
			server.heartBeat = Long.parseLong(rd.readLine());
			server.status = "reachable";
			EntityUtils.consumeQuietly(response.getEntity());
			}	
			
			catch (IOException e) {
				// Server unreachable
				server.status = "unreachable";
			}
		}// End of For loop
	}
	
	// This method updates the new stats in the database
	public void pushServerStats() throws UnknownHostException{
		MongoClient mongo = new MongoClient();
		DB db = mongo.getDB("CollabDraw");
		DBCollection table = db.getCollection("servers");
		for (Server server: servers){
			BasicDBObject update = new BasicDBObject();
			update.append("$set", new BasicDBObject()
			      .append("status",server.status)
			      .append("lastKnowHeartbeat",server.heartBeat));
		    BasicDBObject searchQuery = new BasicDBObject().append("serverIP",server.ip);
		 	table.update(searchQuery, update);
		}
	}
	
	public static void main (String args[]) throws IllegalStateException, IOException, InterruptedException{
		StatsHandler handler = new StatsHandler();
		//TODO: Uncomment when run as service
		//while(true)
		{
			handler.getServerList();
			handler.getServerStats();
			handler.pushServerStats();
			handler.getServerList();
			//Thread.sleep(5000);
		}
		for (Server server: handler.servers){
			System.out.println(server.ip + "," + server.heartBeat + "," + server.status);
		}
	}
}
