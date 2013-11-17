package controllers;

import models.PaintRoom;

import org.codehaus.jackson.JsonNode;

import play.Logger;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.WebSocket;

import views.html.canvas;
/**
 * Main controller class that handles the canvas 2D drawing for the web application.
 * 
 * @author excelsior
 *
 */
public class Canvas extends Controller {
	
	public static PaintRoom board = new PaintRoom();
	
	public static Result index() {
		Logger.info("Rendering the canvas ..");
		return ok(canvas.render());
	}
	
	/**
	 * Controller method that enable websockets based full-duplex communication.
	 */
	public static WebSocket<JsonNode> stream() {
		  return new WebSocket<JsonNode>() {

		    // Called when the Websocket Handshake is done.
		    public void onReady(WebSocket.In<JsonNode> in, WebSocket.Out<JsonNode> out) {
		    	Logger.info("Web socket ready ..");
		    	try {
			    	board.websocketHandler(in, out);
			    } catch (Exception e) {
			    	Logger.error("Websocket failed ..");
	                e.printStackTrace();
	            }
		    }
		  };
		}
	
}
