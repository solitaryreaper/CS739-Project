package controllers;

import models.Constants;
import models.dao.DBService;
import models.dao.RelationalDBService;

import org.codehaus.jackson.node.ObjectNode;

import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;

/**
 * Controller that provides various end points to the get the metadata for this worker server.
 * @author excelsior
 *
 */
public class Servermeta extends Controller {

	/**
	 * Returns a JSON object representing the heartbeat of this worker server.
	 * 
	 * This API would be called by the Session Manager to check if this worker server is up or not.
	 * If it returns a heartbeat greater than the one which the session manager has for this server
	 * already, then this worker server is in good health.
	 * @return
	 */
	public static Result getHeartBeat()
	{
		ObjectNode result = Json.newObject();
		result.put(Constants.LATEST_HEARTBEAT, System.currentTimeMillis());
		return ok(result);
	}
	
	/**
	 * Method gets called from another server to replicate its data to this server.
	 * 
	 * @param paintRoom		Paint Room Name
	 * @param painter		Painter Name
	 * @param startX		Start Point X
	 * @param startY		Start Point Y
	 * @param endX			End Point X
	 * @param endY			End Point Y
	 * @return
	 */
	public static Result replicateData(String paintRoom, String painter, int startX, int startY, int endX, int endY)
	{
		// Starting a DB Service to write the points coming in from another server to the local database.
		DBService dbService = new RelationalDBService();
		dbService.insertPaintBrushEvents(paintRoom, painter, startX, startY, endX, endY);
		
		// TODO: Not sure if returning result is necessary. If yes,
		// Returning the latest timestamp as heartbeat.
		ObjectNode result = Json.newObject();
		result.put(Constants.LATEST_HEARTBEAT, System.currentTimeMillis());
		return ok(result);
	}
}
