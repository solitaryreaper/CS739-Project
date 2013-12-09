package controllers;

import java.util.ArrayList;
import java.util.Map;

import models.PaintRoom;
import models.utils.AppUtils;

import org.codehaus.jackson.JsonNode;

import play.Logger;
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
	/**
	 * List of active paint room sessions being serviced by this worker server.
	 */
	private static Map<String, PaintRoom> allPaintRooms = Maps.newConcurrentMap();
	
	/**
	 * Controller method that displays a canvas for a paintroom for a specific painter.
	 * 
	 * @param paintRoomName
	 * @param painterName
	 * @return
	 */
	public static Result showPaintRoom(String paintroom, String painter) {
		Logger.info("Rendering the canvas " + paintroom + " for user " + painter);
		return ok(canvas.render(paintroom, painter, AppUtils.getIPAddress()));
	}
	
	/**
	 * Controller method that enable websockets based full-duplex communication.
	 */
	public static WebSocket<JsonNode> stream(final String paintroom) {
		  return new WebSocket<JsonNode>() {

		    // Called when the Websocket Handshake is done.
		    public void onReady(WebSocket.In<JsonNode> in, WebSocket.Out<JsonNode> out) {
		    	Logger.info("Received client request to connect to paintroom " + paintroom + " ..");
		    	PaintRoom board = null;
		    	if(!allPaintRooms.containsKey(paintroom)) {
		    		Logger.debug("Created a new paint room " + paintroom + " .. ");
			    	board = new PaintRoom(paintroom);
			    	allPaintRooms.put(paintroom, board);
		    	}
		    	else {
		    		Logger.debug("Joined to an existing paintroom " + paintroom + " .. ");
		    		board = allPaintRooms.get(paintroom);
		    	}
		    	
		    	try {
		    		// Get list of Preferred Servers!
		    		ArrayList<String> destServers = getPreferredServersForPaintRoom(paintroom);
		    		
			    	board.websocketHandler(destServers, board.getName(), in, out);
			    } catch (Exception e) {
			    	Logger.error("Websocket failed ..");
	                e.printStackTrace();
	            }
		    }
		  };
		}
	
	/**
	 * Method to get list of Preferred Servers for a particular session.
	 * 
	 * @param paintRoom		PaintRoom Name
	 * @return				List of Preferred Servers
	 */
	public static ArrayList<String> getPreferredServersForPaintRoom(String paintRoom)
	{
		// TODO: Get list of Preferred Servers from Session Manager!
		
		// Right now, hardcoding servers 
		ArrayList<String> destServers = new ArrayList<String>();
    	//destServers.add("192.168.120.29");
    	destServers.add("192.168.10.131");	
    	
    	return destServers;
	}
}
