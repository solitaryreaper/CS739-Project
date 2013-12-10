package controllers;

import models.AppUtils;
import play.Logger;
import play.data.DynamicForm;
import play.mvc.Controller;
import play.mvc.Result;

/**
 * Handles the session management for the application by co-ordinating with the HeartBeat API
 * to evenly distribute load to the backend worker servers.
 * 
 * @author excelsior
 *
 */
public class Sessionmanager extends Controller {
	
	/**
	 * Redirects control to one of the worker servers to let the client join a paintroom session.
	 * 
	 * Chooses the preferred server for this paint room session using load balanching heuristics.
	 * These decisions are taken by the HeartBeat API.
	 * 
	 * @return redirect("http://<preferred server>?paint_room=<paint room name>")
	 */
	public static Result index()
	{
		DynamicForm dynamicForm = form().bindFromRequest();
		Logger.info(dynamicForm.data().toString());
		String painterName = dynamicForm.get("painter_name");
		String sessionName = null;
		
		String chosenActiveSession = dynamicForm.get("active_session_name");
		String newActiveSession = dynamicForm.get("new_session_name");
		
		if(chosenActiveSession != null && chosenActiveSession.length() > 0) {
			sessionName = chosenActiveSession; 
		}
		else if(newActiveSession != null && newActiveSession.length() > 0) {
			sessionName = newActiveSession;
		}
		else {
			throw new RuntimeException("Session name cannot be null. Please fix !!");
		}

		String workerServerCanvasURL = AppUtils.getWorkerServerCanvasURL(sessionName, painterName);
		Logger.info("Painter : " + painterName + ", Paintroom : " + sessionName + " , URL : " + workerServerCanvasURL);

		// Redirect to error page, if no live preferred server found !!
		if(workerServerCanvasURL.contains("DISCONNECTED")) {
			return badRequest("Failed to get preferred server for paintroom " + sessionName + 
					", painter " + painterName + ". URL generated : " + workerServerCanvasURL);
		}
		
		// Else redirect to the canvas app on the chosen worker server URL.
		return redirect(workerServerCanvasURL);
	}
}
