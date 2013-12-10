package controllers;

import models.Constants;
import models.PaintBrushEvent;
import models.PaintRoom;
import models.Painter;
import models.dao.DBService;
import models.dao.RelationalDBService;

import org.codehaus.jackson.node.ObjectNode;

import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import play.Logger;

/**
 * Controller that provides various end points to the get the metadata for this worker server.
 * @author excelsior
 *
 */
public class Servermeta extends Controller {

	private static DBService dbService = new RelationalDBService();
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
		String heartbeat = new Long(System.currentTimeMillis()).toString();
		return ok(heartbeat);
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
		Logger.info("Server_Replication :: replicateData called");
		dbService.insertPaintBrushEvents(paintRoom, painter, startX, startY, endX, endY);
		
		// Replay this event on the all the clients hosted on this worker server.
		Painter painterObj = new Painter(painter);
		int eventId = (int)System.currentTimeMillis();
		PaintBrushEvent event = new PaintBrushEvent(paintRoom, painterObj, startX, startY, endX, endY, eventId);
		
		boolean isSuccess = PaintRoom.ingestExternalEvents(event, paintRoom);
		if(isSuccess) {
			Logger.info("Successfully ingested external events on local worker server ..");
		}
		else {
			Logger.info("Failed to ingest external events on local worker server ..");
		}
		
		// Returning the latest timestamp as heartbeat.
		ObjectNode result = Json.newObject();
		result.put(Constants.LATEST_HEARTBEAT, System.currentTimeMillis());
		return ok(result);
	}
}
