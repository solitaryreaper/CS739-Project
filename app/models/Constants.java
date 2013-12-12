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
	// Prashant's running tomcat instance that hosts session manager API.
	public static final String SESSION_MGR_IP_ADDRESS = "54.201.156.52";
	public static final String SESSION_MANAGER_BASE_URL = "http://" + Constants.SESSION_MGR_IP_ADDRESS +":8080/CollabDraw/serverOps?";	
	
	// EC2 worker instances
	public static final String EC1_PRIVATE_IP = "10.206.38.9";
	public static final String EC1_PUBLIC_IP = "54.204.106.44";
	public static final String EC2_PRIVATE_IP = "10.238.200.199";
	public static final String EC2_PUBLIC_IP = "54.196.108.77";
}
