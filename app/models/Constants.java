package models;

/**
 * All the constants used for this application.
 * 
 * @author excelsior
 *
 */
public class Constants {
	// Painter JSON attributes
	public static final String PAINTER_NAME = "name";
	public static final String PAINTER_BRUSH_SIZE = "brush_size";
	public static final String PAINTER_BRUSH_COLOR = "brush_color";
	
	public static final String START_X = "start_x";
	public static final String START_Y = "start_y";
	public static final String END_X = "end_x";
	public static final String END_Y = "end_y";
	
	public static final String EVENT_TYPE = "event_type";
	public static final String DUMMY_EVENT_MARKER = "dummy";
	
	// Database connectors
	public static final String DB_NAME = "collabdraw";
	public static final String DB_USER = "collab_user";
	public static final String DB_PWD = "collab_user";
	
	// Database table name
	public static final String PAINTBRUSH_EVENTS_TABLE = "collabdraw.paint_room_events";
	
	// Profiling events
	public static final String WEBSOCKET_EVENT_TIMER = "WEBSKT_EVENT_INGESTION_TIME";
	public static final String DB_EVENT_TIMER = "DB_INSERT_TIME";
	
	// Server meta identifiers
	public static final String LATEST_HEARTBEAT = "heartbeat";
	public static final String SERVER_ID = "server_id";
	public static final String REGISTER_SERVER = "register";
	public static final String SEND_HEARTBEAT = "heartbeat";

	// Session manager constants
	public static final String SESSION_MGR_IP_ADDRESS = "192.168.2.16";
}
