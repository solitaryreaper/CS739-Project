package controllers;

import java.util.List;
import java.util.Map;

import models.Constants;
import models.PaintBrushEvent;
import models.PaintRoom;
import models.Painter;
import models.daemon.ServerMetaHandler;
import models.dao.DBService;
import models.dao.RelationalDBService;
import models.utils.AppUtils;

import org.codehaus.jackson.JsonNode;

import play.Logger;
import play.data.DynamicForm;
import play.libs.F;
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
	
	private static DBService dbService = new RelationalDBService();
	
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
		String replicateIP = AppUtils.getFailoverIPAddress(preferredServerIP);
		String failoverServerCanvasURL = "http://" + replicateIP + ":9000/canvas?paintroom=" + paintroom + "&painter=" + painter;
		return ok(canvas.render(paintroom, painter, preferredServerIP, replicateIP, failoverServerCanvasURL));
	}
	
	/**
	 * Controller method that enables websockets based full-duplex communication.
	 * 
	 * This websocket connection is used for the real-time canvas updates and rendering.
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
	
	/**
	 * A webscoket connection established by a client connected to some other worker server to
	 * synchronize this server with the host server.
	 * 
	 */
	public static WebSocket<JsonNode> synchronize(final String paintroom) {
		  return new WebSocket<JsonNode>() {
		      
	    // Called when the Websocket Handshake is done.
	    public void onReady(WebSocket.In<JsonNode> in, WebSocket.Out<JsonNode> out) {
	     
	      Logger.info("Received client request to replicate for paintroom " + paintroom + " ..");
	    	
	      // For each event received on the socket,
	      in.onMessage(new F.Callback<JsonNode>() {
	         public void invoke(JsonNode json) {
	        	Logger.info("Recieved a new event to replicate ..");
	            String painter = json.get(Constants.PAINTER_NAME).getTextValue();
            	int startX = json.get(Constants.START_X).getIntValue();
            	int startY = json.get(Constants.START_Y).getIntValue();
            	int endX = json.get(Constants.END_X).getIntValue();
            	int endY = json.get(Constants.END_Y).getIntValue();
	            	
            	dbService.insertPaintBrushEvents(paintroom, painter, startX, startY, endX, endY);
            	
            	Logger.info("Paintrooms : " + allPaintRooms.keySet().toString());
            	if(allPaintRooms.containsKey(paintroom)) {
            		// Replay this event on the all the clients hosted on this worker server.
            		Painter painterObj = new Painter(painter);
            		int eventId = (int)System.currentTimeMillis();
            		PaintBrushEvent event = new PaintBrushEvent(paintroom, painterObj, startX, startY, endX, endY, eventId);
            		boolean isSuccess = PaintRoom.ingestExternalEvents(event, paintroom);
            		if(isSuccess) {
            			Logger.info("Successfully ingested external events on local worker server ..");
            		}
            		else {
            			Logger.info("Failed to ingest external events on local worker server ..");
            		}            		
            	}
            	else {
            		Logger.info("Skipped replay of event on canvas because paintroom not active on server " + paintroom);
            	}
	         } 
	      });
	      
	      // When the replicate socket is closed.
	      in.onClose(new F.Callback0() {
	         public void invoke() {
	           Logger.info("Replicate websocket for paintroom " + paintroom + " disconnected ..");
	         }
	      });
	      
	    }
		    
	  };
	}
	  
}
