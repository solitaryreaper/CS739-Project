package controllers;

import models.Constants;

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
}
