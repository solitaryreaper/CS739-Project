package controllers;

import java.util.ArrayList;
import java.util.List;

import models.AppUtils;
import models.ServerStats;
import models.SessionStats;
import play.Logger;
import play.mvc.Controller;
import play.mvc.Result;
import views.html.serverdashboard;
//import models.ServerStatus;

/**
 * ServerStatus
 * http://54.201.156.52:8080/CollabDraw/serverOps?operation=getServerStats
 * [{"clientLoad":0,"lastKnowHeartbeat":1386758286286,"preferredServerFor":1,"serverIP":"54.204.106.44","status":"unreachable"},
 *  {"clientLoad":0,"lastKnowHeartbeat":1386758368578,"preferredServerFor":1,"serverIP":"54.196.108.77","status":"unreachable"}]
 *  
 * SessionTable
 * http://54.201.156.52:8080/CollabDraw/serverOps?operation=getSessionTable
 * [{"PreferredServers":["54.204.106.44","54.196.108.77"],"SessionId":"zen","Users":["zen1","zen2"]}]
 * 
 * UserTable
 * http://54.201.156.52:8080/CollabDraw/serverOps?operation=getUserTable
 * [{"PrimaryServer":"DISCONNECTED","SessionId:UserId":"zen:zen1"},
 * {"PrimaryServer":"DISCONNECTED","SessionId:UserId":"zen:zen2"}]
 */


/**
 * Controller class that serves as the entry point for server dashboard.
 */
public class ServerDashboard extends Controller {

	/**
	 * Lists the active paint room sessions by invoking the HeartBeat API.
	 * @return	List of active paintroom names
	 */
	public static Result index()
	{
		ArrayList<ServerStats> serverStatsList = AppUtils.getServerStats();
		ArrayList<SessionStats> sessionStatsList = AppUtils.getSessionStats();

		if(serverStatsList != null)
		for(ServerStats s : serverStatsList)
			Logger.info("ServerDashboard :: Server IP " + s.mServerIP);
		
		List<String> activePaintRooms = AppUtils.getActivePaintrooms();
		return ok(serverdashboard.render(activePaintRooms, serverStatsList, sessionStatsList));
	}
	
}
