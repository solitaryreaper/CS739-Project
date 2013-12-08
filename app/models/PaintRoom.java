package models;

import java.util.List;
import java.util.Map;

import models.dao.DBService;
import models.dao.RelationalDBService;
import models.utils.JSONUtils;

import org.codehaus.jackson.JsonNode;

import play.Logger;
import play.libs.F;
import play.mvc.WebSocket;

import com.google.common.base.Stopwatch;
import com.google.common.collect.Maps;

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
	// Name of the current paint room (session)
	private String name;
	
	// List of current painters using the shared canvas.
	private Map<String, Painter> painters = Maps.newConcurrentMap();
	
	// Map of incoming client to server channels to the corresponding client painter name
	private Map<WebSocket.In<JsonNode>, String> serverToClientChannels = Maps.newConcurrentMap();
	
	private static DBService dbService = new RelationalDBService();

	// Profiling data structures
	private static int totalWebSktEvents = 0;
	private static int totalDbEvents = 0;
	private static Stopwatch websktIngestionEventWatch = new Stopwatch();
	private static Stopwatch dbIngestionEventWatch = new Stopwatch();
	private static Map<String, Long> runTimeStats = Maps.newConcurrentMap();
	static {
		runTimeStats.put(Constants.WEBSOCKET_EVENT_TIMER, 0L);
		runTimeStats.put(Constants.DB_EVENT_TIMER, 0L);
	}
	
	public PaintRoom(String name)
	{
		this.name = name;
	}
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Map<String, Painter> getPainters() {
		return painters;
	}

	public void setPainters(Map<String, Painter> painters) {
		this.painters = painters;
	}

	/**
	 * Utility method that processes various websocket events
	 * 
	 * @param in	Websocket connection from client => server
	 * @param out	Websocket connection from server => client
	 */
	public void websocketHandler(final String paintRoom, final WebSocket.In<JsonNode> in, final WebSocket.Out<JsonNode> out)
	{
        // in: handle incoming messages from the client
        in.onMessage(new F.Callback<JsonNode>() {
            @Override
            public void invoke(JsonNode json) throws Throwable {
            	websktIngestionEventWatch.reset();
            	dbIngestionEventWatch.reset();
            	
            	websktIngestionEventWatch.start();

            	String name = json.get(Constants.PAINTER_NAME).getTextValue();
            	Painter painter = null;
            	boolean isDummyEvent = isDummyBrushEvent(json);
            	
            	/**
            	 * Check if this is a new client joining an existing session. If yes, do the following :
            	 * 
            	 * 1)Add this to the list of painters listening to this paintroom.
            	 * 2)Get the list of all the brush events to have happened in the paintroom so far.
            	 *   Replay all these brush events before recieveing any further messages.
            	 */
            	// Check if a client has joined the session or is this an existing client
            	List<PaintBrushEvent> pastEvents = null;
            	if(!painters.containsKey(name)) {
            		painter = new Painter(name);
            		// Store the outgoing server to client channel for pushing events later
            		painter.setChannel(out);
            		painters.put(name, painter);

            		serverToClientChannels.put(in, painter.getName());
            		Logger.info("Added a new painter " + painter.getName());
            		
            		pastEvents = dbService.getAllBrushEventsForPaintRoom(paintRoom);
            		if(!pastEvents.isEmpty()) {
                		Logger.info("Found " + pastEvents.size() + " past events for paint room " + paintRoom);            			
            		}

            	}
            	else {
            		painter = painters.get(name);
            	}

            	int eventsWritten = 0;
            	Logger.debug("Total active painters for paintroom " + paintRoom + " are " + painters.size());
            	for(Painter p : painters.values()) {
            		// Optimization : Don't broadcast the message to the originating client.
            		if(p.equals(painter)) {
            			if(pastEvents != null) {
            				List<JsonNode> events = JSONUtils.convertPOJOToJSON(pastEvents);
                    		for(JsonNode event : events) {
                        		p.getChannel().write(event);
                        		++eventsWritten;
                    		}
                    		
                    		Logger.info("Wrote " + eventsWritten + " past events to client " + 
                    				p.getName() + " for paintroom " + paintRoom);
            			}
            			continue;
            		}

            		if(!isDummyEvent) {
                		Logger.debug("Writing message to painter : " + p.getName() + ", message : " + json.toString());
                		p.getChannel().write(json);
            		}
            	}
            	
            	// Add the new painter information and the drawn points to the database, so that
            	// it can be sent to any new clients.
            	int startX = json.get(Constants.START_X).getIntValue();
            	int startY = json.get(Constants.START_Y).getIntValue();
            	int endX = json.get(Constants.END_X).getIntValue();
            	int endY = json.get(Constants.END_Y).getIntValue();
            	
            	// TODO : This should be done in a parallel thread and should be non-blocking for
            	// the other streaming paint brush events.
            	//Logger.debug("Adding paint brush events ..");
            	dbIngestionEventWatch.start();
            	dbService.insertPaintBrushEvents(paintRoom, painter.getName(), startX, startY, endX, endY);
            	dbIngestionEventWatch.stop();
            	
            	websktIngestionEventWatch.stop();
            	
            	// Collect the profiling stats for the current event ingestion
            	++totalWebSktEvents;
            	++totalDbEvents;
            	long runningWebSocketEvtTime = websktIngestionEventWatch.elapsedMillis() + 
            			runTimeStats.get(Constants.WEBSOCKET_EVENT_TIMER);
            	long runningDbEvtTime = dbIngestionEventWatch.elapsedMillis() + 
            			runTimeStats.get(Constants.DB_EVENT_TIMER);
            	
            	runTimeStats.put(Constants.WEBSOCKET_EVENT_TIMER, runningWebSocketEvtTime);
            	runTimeStats.put(Constants.DB_EVENT_TIMER, runningDbEvtTime);
            }
        });

        // Client has disconnected.
        in.onClose(new F.Callback0() {
            @Override
            public void invoke() throws Throwable {
            	Logger.info("Websocket disconnected .. ");
            	
            	/**
            	 * Inform session manager that a painter has disconnected from a paintroom.
            	 * TODO
            	 */
            	String painter = serverToClientChannels.get(in);
            	
            	// Publish the stats for the websocket to profile the application
            	double avgWebSktEvtTime = runTimeStats.get(Constants.WEBSOCKET_EVENT_TIMER)/(double)totalWebSktEvents;
            	double avgDbEvtTime = runTimeStats.get(Constants.DB_EVENT_TIMER)/(double)totalDbEvents;
            	
            	Logger.info("Average websocket event time for paintroom " + getName() + " is " + avgWebSktEvtTime + " ms .");
            	Logger.info("Average DB insert event time for paintroom " + getName() + " is " + avgDbEvtTime + " ms .");
            }
        });		
	}
	
	/**
	 * Determines if the brush event is a dummy event.
	 * 
	 * Dummy events are sent to announce the presence of a new painter who has joined the session,
	 * so that his/her canvas can be bootstrapped with prior events for this session from the
	 * server.
	 * 
	 * @param event
	 * @return
	 */
	private static boolean isDummyBrushEvent(JsonNode event)
	{
		boolean isDummyEvent = false;
		if(event.get(Constants.EVENT_TYPE) != null) {
			String eventType = event.get(Constants.EVENT_TYPE).getTextValue().trim();
			isDummyEvent = eventType.equals(Constants.DUMMY_EVENT_MARKER.trim()) ? true : false;
			Logger.debug("Found dummy event for event type " + eventType);
		}

		return isDummyEvent;
	}
}
