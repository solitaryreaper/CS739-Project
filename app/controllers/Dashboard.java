package controllers;

import java.util.List;

import models.PaintRoom;
import models.dao.DBService;
import models.dao.RelationalDBService;
import play.Logger;
import play.mvc.Controller;
import play.mvc.Result;

import views.html.dashboard;
/**
 * Controller class that serves as the entry point for this application.
 * 
 * Shows the list of active paintroom sessions and gives user the option to start a new paintroom.
 * @author excelsior
 *
 */
public class Dashboard extends Controller{

	private static DBService dbService = new RelationalDBService();
	
	// Render the list of active paintrooms, so that clients can join in.
	public static Result index()
	{
		List<PaintRoom> activePaintRooms = dbService.getActivePaintRooms();
		Logger.info("Found " + activePaintRooms.size() + " active paint rooms.");
		return ok(dashboard.render(activePaintRooms));
	}
	
}
