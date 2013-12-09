package models.daemon;

import models.utils.AppUtils;
import play.Logger;
import play.api.libs.concurrent.Promise;
import play.api.libs.ws.Response;
import play.api.libs.ws.WS;

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
	
	public static final String SESSION_MGR_URL = "http://localhost:8080/CollabDraw/serverOps?";
	
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
		}
		
		return isSuccess;
	}

	private static boolean getAPIResult(String url)
	{
		Logger.info("URL to invoke : " + url);
		Promise<Response> body = WS.url(url).get();
		String result = body.value().get().body();
		Logger.info("Result : " + result);

		if(result == null) {
			return false;
		}

		return true;
	}
	
}
