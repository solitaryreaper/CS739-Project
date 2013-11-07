package controllers;

import play.mvc.Controller;
import play.mvc.Result;
import views.html.canvas;

/**
 * Main controller class that handles the canvas 2D drawing for the web application.
 * 
 * @author excelsior
 *
 */
public class Canvas extends Controller {
	
	public static Result index() {
		return ok(canvas.render());
	}
}
