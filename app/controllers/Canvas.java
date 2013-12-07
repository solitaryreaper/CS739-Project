package controllers;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;

import models.PaintRoom;
import models.dao.DBService;
import models.dao.RelationalDBService;

import org.codehaus.jackson.JsonNode;

import play.Logger;
import play.data.DynamicForm;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.WebSocket;
import views.html.canvas;

import com.google.common.collect.Maps;
/**
 * Main controller class that handles the canvas 2D drawing for the web application.
 * 
 * @author excelsior
 *
 */
public class Canvas extends Controller {
	
	private static DBService dbService = new RelationalDBService();
	private static Map<String, PaintRoom> allPaintRooms = Maps.newConcurrentMap();
	
	public static Result index() {
        // Get form parameters
        DynamicForm dynamicForm = form().bindFromRequest();
        String paintRoomName = dynamicForm.get("session_name");
		Logger.info("Rendering the canvas " + paintRoomName + " .. ");
		
		String ipAddress = null;
		try {
			ipAddress = InetAddress.getLocalHost().getHostAddress();
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return ok(canvas.render(paintRoomName, ipAddress));
	}
	
	/**
	 * Controller method that enable websockets based full-duplex communication.
	 */
	public static WebSocket<JsonNode> stream(final String paintRoomName) {
		  return new WebSocket<JsonNode>() {

		    // Called when the Websocket Handshake is done.
		    public void onReady(WebSocket.In<JsonNode> in, WebSocket.Out<JsonNode> out) {
		    	Logger.info("Received " + paintRoomName + " from client ..");
		    	PaintRoom board = null;
		    	if(!allPaintRooms.containsKey(paintRoomName)) {
		    		Logger.debug("Created a new paint room " + paintRoomName + " .. ");
			    	board = new PaintRoom(paintRoomName);
			    	allPaintRooms.put(paintRoomName, board);
					dbService.createPaintRoom(board.getId(), board.getName());			    	
		    	}
		    	else {
		    		Logger.debug("Joined to an existing paintroom " + paintRoomName + " .. ");
		    		board = allPaintRooms.get(paintRoomName);
		    	}

		    	try {
			    	board.websocketHandler(board.getId(), in, out);
			    } catch (Exception e) {
			    	Logger.error("Websocket failed ..");
	                e.printStackTrace();
	            }
		    }
		  };
		}
	
}
