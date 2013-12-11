package controllers;

import java.util.List;
import java.util.Map;

import models.PaintRoom;
import models.daemon.ServerMetaHandler;
import models.utils.AppUtils;

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
	/**
	 * List of active paint room sessions being serviced by this worker server.
	 */
	private static Map<String, PaintRoom> allPaintRooms = Maps.newConcurrentMap();
	
	/**
	 * Controller method that displays a canvas for a paintroom for a specific painter.
	 * 
	 * @param paintRoomName
	 * @param painterName
	 * @return	An up-to-date canvas for the chosen paintroom !!
	 */
	public static Result showPaintRoom(String paintroom, String painter) {
		Logger.info("Rendering the canvas " + paintroom + " for user " + painter);
		String preferredServerIP = AppUtils.getIPAddress();
		String failoverIP = AppUtils.getFailoverIPAddress(preferredServerIP);
		String failoverServerCanvasURL = "http://" + failoverIP + ":9000/canvas?paintroom=" + paintroom + "&painter=" + painter;
		return ok(canvas.render(paintroom, painter, preferredServerIP, failoverServerCanvasURL));
	}
	
	/**
	 * Controller methods that displays a canvas for a specific painter, but on a new server than
	 * the one to which the client had initially connected.
	 * 
	 *  This method represents the failover situation in which a painter was connected to a paintroom
	 *  on one of the worker servers. Due to some reason, the worker server went down and we need to
	 *  relocate the client to a new worker server, if available. If no other worker servers are
	 *  available the client operates in disconnected mode.
	 */
	public static Result showPaintRoomOnNewServer()
	{
		DynamicForm dynamicForm = form().bindFromRequest();
		Logger.info(dynamicForm.data().toString());
		String painterName = dynamicForm.get("painter_name_redirect");
		String sessionName = dynamicForm.get("paint_room_name_redirect");
		
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
		    		List<String> destServers = ServerMetaHandler.getPreferredServersForPaintRoom(paintroom);
		    		Logger.info("Preferred severs : " + destServers.toString());
			    	board.websocketHandler(destServers, board.getName(), in, out);
			    } catch (Exception e) {
			    	Logger.error("Websocket failed ..");
	                e.printStackTrace();
	            }
		    }
		  };
		}
}
