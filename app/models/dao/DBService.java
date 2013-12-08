package models.dao;

import java.util.List;

import models.PaintBrushEvent;
import models.Painter;

/**
 * A generic interface to the underlying database that stores all the paint brush events for
 * various paintrooms being serviced by this worker server.
 * 
 * @author excelsior
 *
 */
public interface DBService {
	/**
	 * Persist new brush events for a user in a particular session. This is important because when
	 * a new client/painter joins this session, all the existing brush events that have already
	 * occured in this session should be replayed to the client. To enable this functionality all
	 * the brush events are recorded in the database.
	 * @param paintRoom
	 * @param painter
	 * @param startX
	 * @param startY
	 * @param endX
	 * @param endY
	 */
	public void insertPaintBrushEvents(String paintRoom, String painter, int startX, int startY, int endX, int endY);
	
	/**
	 * Returns all the paint brush events that have already occurred for a session. This is important
	 * so that old brush events can be returned for a new user who joins the paint room/session.
	 * The key idea is that all these brush events should be chronologically sorted.
	 * @return
	 */
	public List<PaintBrushEvent> getAllBrushEventsForPaintRoom(String paintRoom);
}
