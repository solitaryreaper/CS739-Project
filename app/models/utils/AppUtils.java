package models.utils;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Maps;

import play.Logger;
import play.api.libs.concurrent.Promise;
import play.api.libs.ws.Response;
import play.api.libs.ws.WS;

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
		local2PublicIPMap.put("10.206.38.9", 		"54.204.106.44");
		local2PublicIPMap.put("10.238.200.199", 	"54.196.108.77");
		local2PublicIPMap.put("10.210.175.237", 	"54.226.244.84");
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
}
