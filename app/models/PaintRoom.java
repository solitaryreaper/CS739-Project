package models;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.codehaus.jackson.JsonNode;

import play.Logger;
import play.libs.F;
import play.mvc.WebSocket;

/**
 * Represents a single drawing board which has multiple connected clients, collaborating to 
 * modify/create the same object. In the case of collaborative editor, this represents a shared
 * HTML5 canvas object.
 * 
 * @author excelsior
 * 
 * TODO :
 * 1) Assign paintroom a handle (name). This would serve as the session identifier.
 *
 */
public class PaintRoom {
	// List of current painters using the shared canvas.
	public Map<String, Painter> painters = new ConcurrentHashMap<String, models.Painter>();
	
	/**
	 * Utility method that processes various websocket events
	 * 
	 * @param in	Websocket connection from client => server
	 * @param out	Websocket connection from server => client
	 */
	public void websocketHandler(final WebSocket.In<JsonNode> in, final WebSocket.Out<JsonNode> out)
	{
        // in: handle incoming messages from the client
        in.onMessage(new F.Callback<JsonNode>() {
            @Override
            public void invoke(JsonNode json) throws Throwable {
            	String userId = json.get("id").getTextValue();
            	Painter painter = null;
            	// Check if a client has joined the session or is this an existing client
            	if(!painters.containsKey(userId)) {
            		painter = new Painter(userId, out);
            		painters.put(userId, painter);
            		
            		Logger.info("Added a new painter " + painter.id);
            	}

            	Logger.info("New message from user " + userId + " is " + json.toString());
            	for(Painter p : painters.values()) {
            		// Optimization : Don't broadcast the message to the originating client.
            		if(p.equals(painter)) {
            			Logger.info("Skipped for painter " + p.id + " because same as message origin " + userId);
            			continue;
            		}
            		Logger.info("Writing message to painter : " + p.id);
            		p.channel.write(json);
            	}
            	
            	// Add the new painter information and the drawn points to the database, so that
            	// it can be sent to any new clients.
            }
        });

        // Client has disconnected.
        in.onClose(new F.Callback0() {
            @Override
            public void invoke() throws Throwable {
            	Logger.info("Websocket disconnected .. ");
            }
        });		
	}
}
