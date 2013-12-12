package models;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.codehaus.jackson.JsonNode;

import play.Logger;
import play.api.libs.concurrent.Promise;
import play.api.libs.ws.Response;
import play.api.libs.ws.WS;
import play.libs.Json;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

//import models.ServerStatus;

/**
 * Utility functions for session management.
 * 
 * @author excelsior
 *
 */
public class AppUtils {

	// Prashant's running Tomcat instance that hosts session manager API.
	public static final String SESSION_MGR_IP_ADDRESS = "54.201.156.52";
	public static final String SESSION_MANAGER_BASE_URL = "http://" + SESSION_MGR_IP_ADDRESS +":8080/CollabDraw/serverOps?";
	
	/**
	 * Returns the canvas URL on the chosen worker server for rendering the requested paintroom
	 * for a client.
	 * 
	 * @param paintRoomName
	 * @param painter
	 * @return
	 */
	public static String getWorkerServerCanvasURL(String paintRoomName, String painter)
	{
		Logger.info("Inside worker server canvas url ..");
		String ipAddress = getPreferredServerForClient(paintRoomName, painter);
		String workerServerCanvasURL = "http://" + ipAddress + ":9000/canvas?paintroom=" + 
				paintRoomName + "&painter=" + painter;
		return workerServerCanvasURL;
	}
	
	/**
	 * Returns the list of active paintroom sessions.
	 * @return
	 */
	public static List<String> getActivePaintrooms()
	{
		String sessionsURL = SESSION_MANAGER_BASE_URL + "operation=getSessionTable";
		String sessions = getAPIResult(sessionsURL);
		Logger.info("Sessions string : " + sessions);
		
		Set<String> activePaintRooms = Sets.newHashSet();		
		JsonNode node = Json.parse(sessions);
		
		if(node == null)
		{
			Logger.info("getActivePaintrooms :: JSON Parsing failed");
			return null;
		}
		
		List<JsonNode> sessionNodes = node.findValues("SessionId");
		for(JsonNode n : sessionNodes) {
			Logger.info("JSON : " + n.toString());
			activePaintRooms.add(n.getTextValue());
		}

		Logger.info("Found " + activePaintRooms.size() + " active paintrooms ..");
		List<String> activePaintroomsList = Lists.newArrayList(activePaintRooms);
		return activePaintroomsList;
	}
	
	/**
	 * Returns the preferred server's IP address to be used for serving the current client.
	 * 
	 * This information is calculated based on the load balancing heuristics for each worker servers.
	 * @param paintRoomName
	 * @param painter
	 * @return
	 */
	private static String getPreferredServerForClient(String paintRoomName, String painter)
	{
		String urlToInvoke = SESSION_MANAGER_BASE_URL + "operation=getWorkerServer&sessionId=" + 
				paintRoomName + "&userId=" + painter;
		String preferredServerIP = null;
		try {
			preferredServerIP = getAPIResult(urlToInvoke);
		} catch (Exception e) {
			Logger.error("RESTFul API failed. Reason : " + e.getMessage());
		}
		
		Logger.info("Found preferred server : " + preferredServerIP);
		return preferredServerIP;
	}

	private static String getAPIResult(String url)
	{
		Logger.info("getAPIResult :: URL to invoke = " + url);
		Promise<Response> body = WS.url(url).get();
		String result = body.value().get().body().trim();
		Logger.info("getAPIResult :: Result = " + result);

		return result;
	}
	
	/**
	 * Method to get server stats from the Session Manager and pass it on to the Server Dashboard.
	 */
	public static ArrayList<ServerStats> getServerStats()
	{
		String urlToInvoke = SESSION_MANAGER_BASE_URL + "operation=getServerStats";
		Logger.info("getServerStatus :: urlToInvoke = " + urlToInvoke);
				
		String jsonServerStatusList = null;
		try { jsonServerStatusList = getAPIResult(urlToInvoke);} 
		catch (Exception e)
		{
			Logger.error("getServerStatus :: RESTFul API failed. Reason : " + e.getMessage());
			return null;
		}
		
		Logger.info("getServerStatus :: jsonServerStatusList = " + jsonServerStatusList);
		try
		{
			// Parse JSON List
			JsonNode root = Json.parse(jsonServerStatusList);
			
			// Basic Error Checking
			if(root == null) { Logger.error("getServerStatus :: JSON Parsing failed"); return null;	}
			else if(!root.isArray()) { Logger.error("getServerStatus :: isArray = false"); return null;	}
			
			ArrayList<ServerStats> serverStatsList = new ArrayList<ServerStats>();
			Iterator<JsonNode> ite = root.getElements();
			while(ite.hasNext())
			{
				JsonNode serverStatsJSONNode = ite.next();
				if(serverStatsJSONNode == null) continue;
				
				String serverIP = "";
				boolean isReachable = false;
				int sessionServingCount = 0, clientServingCount = 0;
				long lastHeartbeat = 0;
				
				// Extracting individual JSON nodes 
				JsonNode serverIPNode = serverStatsJSONNode.path("serverIP");
				JsonNode isReachableNode = serverStatsJSONNode.path("status");
				JsonNode sessionServingCountNode = serverStatsJSONNode.path("preferredServerFor");
				JsonNode clientServingCountNode = serverStatsJSONNode.path("clientLoad");
				JsonNode lastHeartbeatNode = serverStatsJSONNode.path("lastKnowHeartbeat");
					
				if(serverIPNode == null || isReachableNode == null || sessionServingCountNode == null 
						|| clientServingCountNode == null || lastHeartbeatNode == null)
					continue;
				
				// Extracting data from JSON Nodes
				serverIP = serverIPNode.getTextValue();
				isReachable = (isReachableNode.getTextValue().equals("unreachable")) ? false : true;
				sessionServingCount = sessionServingCountNode.getIntValue();
				clientServingCount = clientServingCountNode.getIntValue();
				lastHeartbeat = lastHeartbeatNode.getLongValue();
				
				// Update the list
				serverStatsList.add(new ServerStats(serverIP, isReachable, sessionServingCount, clientServingCount, lastHeartbeat));
			}
			
			return serverStatsList;
		}
		catch (RuntimeException e)
		{
			Logger.error("getServerStatus :: Parsing failed. Reason : " + e.getMessage());
		}
		
		return null;
	}
	
	/**
	 * Method to get Session Stats from the Session Manager and pass it on to the Server Dashboard.
	 * [{"PreferredServers":["54.204.106.44","54.196.108.77"],"SessionId":"zen","Users":["zen1","zen2"]}]
	 **/
	public static ArrayList<SessionStats> getSessionStats()
	{
		String urlToInvoke = SESSION_MANAGER_BASE_URL + "operation=getSessionTable";
		Logger.info("getSessionStats :: urlToInvoke = " + urlToInvoke);
				
		String jsonSessionStatusList = null;
		try { jsonSessionStatusList = getAPIResult(urlToInvoke);} 
		catch (Exception e)
		{
			Logger.error("getSessionStats :: RESTFul API failed. Reason : " + e.getMessage());
			return null;
		}
		
		Logger.info("getSessionStats :: jsonSessionStatusList = " + jsonSessionStatusList);
		try
		{
			// Parse JSON List
			JsonNode rootNode = Json.parse(jsonSessionStatusList);
			
			// Basic Error Checking
			if(rootNode == null) { Logger.error("getSessionStats :: JSON Parsing failed"); return null;	}
			else if(!rootNode.isArray()) { Logger.error("getSessionStats :: isArray = false"); return null;	}
			
			ArrayList<SessionStats> sessionStatsList = new ArrayList<SessionStats>();
			Iterator<JsonNode> ite = rootNode.getElements();
			while(ite.hasNext())
			{
				JsonNode sessionStatsJSONNode = ite.next();
				if(sessionStatsJSONNode == null) continue;
				
				String sessionID = "";
				List<String> preferredServers = new ArrayList<String>();
				List<String> users = new ArrayList<String>();
				
				// Extracting individual JSON nodes 
				JsonNode sessionIDNode = sessionStatsJSONNode.path("SessionId");
				JsonNode preferredServersNode = sessionStatsJSONNode.path("PreferredServers");
				JsonNode usersNode = sessionStatsJSONNode.path("Users");

				if(sessionIDNode == null || preferredServersNode == null || usersNode == null
						|| !preferredServersNode.isArray() || !usersNode.isArray()) 
					continue;
				
				// Extracting data from JSON Nodes
				sessionID = sessionIDNode.getTextValue();
				
				Iterator<JsonNode> preferredServersIte = preferredServersNode.getElements();
				while(preferredServersIte.hasNext())
					preferredServers.add(preferredServersIte.next().getTextValue());
				
				Iterator<JsonNode> usersIte = usersNode.getElements();
				while(usersIte.hasNext())
					users.add(usersIte.next().getTextValue());
				
				// Update the list
				sessionStatsList.add(new SessionStats(sessionID, preferredServers, users));
			}
			
			return sessionStatsList;
		}
		catch (RuntimeException e)
		{
			Logger.error("getSessionStats :: Parsing failed. Reason : " + e.getMessage());
		}
		
		return null;
	}

}
