package controllers;

import java.util.List;

import models.AppUtils;

import play.mvc.Controller;
import play.mvc.Result;
import views.html.serverdashboard;

/**
 * Controller class that serves as the entry point for this application.
 * 
 * Shows the list of active paintroom sessions and gives user the option to start a new paintroom.
 * @author excelsior
 *
 */
public class ServerDashboard extends Controller {

	/**
	 * Lists the active paint room sessions by invoking the HeartBeat API.
	 * @return	List of active paintroom names
	 */
	public static Result index()
	{
		List<String> activePaintRooms = AppUtils.getActivePaintrooms();
		return ok(serverdashboard.render(activePaintRooms));
	}
}
