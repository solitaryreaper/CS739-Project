package models.utils;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import models.Constants;
import play.Logger;
import play.api.libs.concurrent.Promise;
import play.api.libs.ws.Response;
import play.api.libs.ws.WS;

import com.google.common.collect.Maps;

/**
 * Simple utility functions for this application.
 * 
 * @author excelsior
 *
 */
public class AppUtils {

	// Hack : Create a mapping of local and public IP addresses
	public static Map<String, String> local2PublicIPMap = Maps.newHashMap();
	static {
		local2PublicIPMap.put(Constants.EC1_PRIVATE_IP, Constants.EC1_PUBLIC_IP);
		local2PublicIPMap.put(Constants.EC2_PRIVATE_IP, Constants.EC2_PUBLIC_IP);
	}
	
	public static final String HTTP_STR = "http://";
	public static final String WORKER_SERVER_URL = ":9000/replicate?";
	public static final String PAINTROOM = "paintroom";
	public static final String PAINTER = "painter";
	public static final String BEGINX = "startX";
	public static final String BEGINY = "startY";
	public static final String ENDX = "endX";
	public static final String ENDY = "endY";
	
	/**
	 * Call replicate API with these parameters.
	 */
	public static boolean replicateDataOnServers(List<String> destServers, String paintRoom, String painterName, 
											  int beginX, int beginY, int endX, int endY)
	{
		// If no servers to update.
		if(destServers == null || destServers.isEmpty()) return false;
		
		String hostIPAddress = getIPAddress();		

		// Send this data to all the servers in the list
		for(String ipAddress : destServers)
		{
			if(ipAddress.equals(hostIPAddress))
				continue;

			Logger.info("Sending the data to [" + ipAddress + "] worker server.");
			try 
			{
				String url = HTTP_STR + ipAddress + WORKER_SERVER_URL + 
								PAINTROOM 	+ "=" + paintRoom 	+ "&" +
								PAINTER 	+ "=" + painterName + "&" +
								BEGINX 		+ "=" + beginX 		+ "&" +
								BEGINY 		+ "=" + beginY 		+ "&" +
								ENDX 		+ "=" + endX 		+ "&" +
								ENDY 		+ "=" + endY;
				
				Logger.info("Server_Replication :: URL Generated" + url);
				Promise<Response> body = WS.url(url).get();
				String result = body.value().get().body();
				Logger.info("Result from Server: " + result);

				// if(result == null), Do nothing!
			} 
			catch (Exception e) 
			{
				return false;
			}
		}
		
		return true;
	}
	
	/**
	 * Gets the local IP address.
	 * @return
	 */
	public static String getIPAddress()
	{
		String ipAddress = null;
		try {
			String locaIPAddress = InetAddress.getLocalHost().getHostAddress();
			ipAddress = local2PublicIPMap.get(locaIPAddress);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		
		return ipAddress;
	}
	
	/**
	 * Gets a failover IP address for the current worker server.
	 * @param primaryIP
	 * @return
	 */
	public static String getFailoverIPAddress(String primaryIP)
	{
		String failoverIP = null;
		Collection<String> workerServerIPList = local2PublicIPMap.values();
		for(String ip : workerServerIPList) {
			if(!ip.equals(primaryIP)) {
				failoverIP = ip;
				break;
			}
		}
		
		Logger.info("Found failover IP " + failoverIP + " for primary IP " + primaryIP);
		return failoverIP;
	}
	
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
		String ipAddress = getPreferredServerForClient(paintRoomName, painter);
		String workerServerCanvasURL = "http://" + ipAddress + ":9000/canvas?paintroom=" + 
				paintRoomName + "&painter=" + painter;
		return workerServerCanvasURL;
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
