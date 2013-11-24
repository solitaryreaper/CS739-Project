package models.dao;

import java.util.List;

import models.PaintBrushEvent;
import models.PaintRoom;
import models.Painter;

/**
 * A generic interface to the underlying database that stores all the session, users and
 * paint brush events.
 * 
 * Support for both relational(MySQL) and non-relational (MongoDB) database has been added
 * to compare their performances.
 * 
 * @author excelsior
 *
 */
public interface DBService {
	// Create a new paint room/session
	public void createPaintRoom(int id, String name);
	
	// Create a new painter that joined a paintroom
	public void createPainter(int paintRoomId, int painterId, String name, int brushSize, String burshColor);
	
	/**
	 * Updates the list of painters for a paintroom with their activity status.
	 * 
	 * @param paintRoomId
	 * @param painterId
	 * @param isActive
	 */
	public void updatePaintRoomPaintersMap(int paintRoomId, int painterId, boolean isActive);
	
	/**
	 * Persist new brush events for a user in a particular session. This is important because when
	 * a new client/painter joins this session, all the existing brush events that have already
	 * occured in this session should be replayed to the client. To enable this functionality all
	 * the brush events are recorded in the database.
	 * @param paintRoomId
	 * @param painter
	 * @param startX
	 * @param startY
	 * @param endX
	 * @param endY
	 */
	public void insertPaintBrushEvents(int paintRoomId, Painter painter, int startX, int startY, int endX, int endY);
	
	// Returns all the active paint room sessions
	public List<PaintRoom> getActivePaintRooms();
	
	/**
	 * Returns all the paint brush events that have already occurred for a session. This is important
	 * so that old brush events can be returned for a new user who joins the paint room/session.
	 * The key idea is that all these brush events should be chronologically sorted.
	 * @return
	 */
	public List<PaintBrushEvent> getAllBrushEventsForPaintRoom(int paintRoomId);
}
