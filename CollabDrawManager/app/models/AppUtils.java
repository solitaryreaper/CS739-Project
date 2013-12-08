package models;

import play.Logger;
import play.api.libs.concurrent.Promise;
import play.api.libs.ws.Response;
import play.api.libs.ws.WS;

/**
 * Utility functions for session management.
 * 
 * @author excelsior
 *
 */
public class AppUtils {

	public static final String SESSION_MANAGER_BASE_URL = "http://localhost:8080/CollabDraw/serverOps?";
	
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
	
	public static String getWokerServerCanvasURL(String paintRoomName, String painter)
	{
		Logger.info("Inside worker server canvas url ..");
		String ipAddress = getPreferredServerForClient(paintRoomName, painter);
		String workerServerCanvasURL = "http://" + ipAddress + ":9000/canvas";
		return workerServerCanvasURL;
	}
	
	private static String getAPIResult(String url)
	{
		Logger.info("URL to invoke : " + url);
		Promise<Response> body = WS.url(url).get();
		String result = body.value().get().body();
		Logger.info("Result : " + result);

		return result;
	}
}
