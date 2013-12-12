package models;

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

/**
 * Utility functions for session management.
 * 
 * @author excelsior
 *
 */
public class AppUtils {

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
		String sessionsURL = Constants.SESSION_MANAGER_BASE_URL + "operation=getSessionTable";
		String sessions = getAPIResult(sessionsURL);
		Logger.info("Sessions string : " + sessions);
		
		Set<String> activePaintRooms = Sets.newHashSet();		
		JsonNode node = Json.parse(sessions);
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
		String urlToInvoke = Constants.SESSION_MANAGER_BASE_URL + "operation=getWorkerServer&sessionId=" + 
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
		Logger.info("URL to invoke : " + url);
		Promise<Response> body = WS.url(url).get();
		String result = body.value().get().body().trim();
		Logger.info("Result : " + result);

		return result;
	}
}
