package models.daemon;

import java.util.List;

import org.codehaus.jackson.JsonNode;

import com.google.common.collect.Lists;

import models.Constants;
import models.utils.AppUtils;
import play.Logger;
import play.api.libs.concurrent.Promise;
import play.api.libs.ws.Response;
import play.api.libs.ws.WS;
import play.libs.Json;

/**
 * A daemon class that would be continously running in the background and sending meta information
 * about the local worker server to the session manager.
 * 
 * The information exchanged between the servers would be :
 * 1) The worker server would initially register with the session manager.
 * 2) Worker server would send heartbeat stats to the session manager every fixed number of seconds.
 * @author excelsior
 *
 */
public class ServerMetaHandler {
	
	public static final String SESSION_MGR_URL = "http://" + Constants.SESSION_MGR_IP_ADDRESS +":8080/CollabDraw/serverOps?";
	
	/**
	 * Registers the worker server with the session manager.
	 * 
	 * @return
	 */
	public static boolean registerServer()
	{
		String ipAddress = AppUtils.getIPAddress();
		if(ipAddress == null) {
			throw new RuntimeException("Failed to determine the IP address of local worker server !!");
		}
	
		Logger.info("Registering the worker server " + ipAddress);
		boolean isSuccess = true;
		try {
			String url = SESSION_MGR_URL + "operation=register&ip=" + ipAddress;
			getAPIResult(url);
		} catch (Exception e) {
			isSuccess = false;
			String message = "Failed to register worker server with the session manager ..";
			Logger.error(message);
			throw new RuntimeException(message);
		}
		
		return isSuccess;
	}

	/**
	 * Unregisters a painter (client) from a paintroom session when the websocket connection is
	 * closed.
	 * @param paintroom
	 * @param painter
	 * @return
	 */
	public static boolean unregisterClient(String paintroom, String painter)
	{
		String url = SESSION_MGR_URL + "operation=unregisterUser&sessionId=" + paintroom + "&userId=" + painter;
		boolean isSuccess = true;
		try {
			getAPIResult(url);
		} catch (Exception e) {
			isSuccess = false;
		}
		
		return isSuccess;		
	}
	
	/**
	 * Method to get list of Preferred Servers for a particular session.
	 * 
	 * @param paintRoom		PaintRoom Name
	 * @return				List of Preferred Servers
	 */	
	public static List<String> getPreferredServersForPaintRoom(String paintRoom)
	{
		String url = SESSION_MGR_URL + "operation=getServerStats";
		String activeServers = getAPIResult(url);
		
		List<String> preferredServers = Lists.newArrayList();
		JsonNode node = Json.parse(activeServers);
		List<JsonNode> servers = node.findValues("serverIP");
		for(JsonNode n : servers) {
			Logger.info("JSON : " + n.toString());
			preferredServers.add(n.getTextValue());
		}

		Logger.info("Found " + preferredServers.size() + " preferred servers for paintroom " + paintRoom);
		return preferredServers;
	}
	
	// Utility method that invokes a REST GET request at the specified URL.
	private static String getAPIResult(String url)
	{
		Logger.info("URL to invoke : " + url);
		Promise<Response> body = WS.url(url).get();
		String result = body.value().get().body();
		Logger.info("Result : " + result);
		
		return result;
	}
	
}
