package models.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import models.Constants;
import models.PaintBrushEvent;
import models.PaintRoom;
import models.Painter;
import play.Logger;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * A interface to do database operations using a relatinal database backend. MySQL has been used 
 * as the relational database here.
 * @author excelsior
 *
 */
public class RelationalDBService implements DBService{

	/**
	 * Creates a JDBC connection to the MySQL database 
	 */
    private Connection getDBConnection()
    {
        // JDBC connection to the database
        Connection dbConn = null;
        
        try {
            Class.forName("com.mysql.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to load mysql jdbc connector ..");
        }
        
        try {
            dbConn = DriverManager.getConnection(
            		"jdbc:mysql://localhost:3306/" + Constants.DB_NAME,
            		Constants.DB_USER, 
            		Constants.DB_PWD);
        } catch (SQLException e) {
            e.printStackTrace();                        
            throw new RuntimeException("Connection Failed! Check output console");
        }
        
        if(dbConn != null) {
            System.out.println("Connected to the MySQL database ..");
        }
        else {
            throw new RuntimeException("Failed to connect to the MySQL database ..");
        }
        
        return dbConn;
    }
    
    /**
     * Important to close the database connection, so that database resources are not tied up.
     * @param dbConn        JDBC connection to the database
     */
    private void closeDBConnection(Connection dbConn)
    {
        try {
            dbConn.close();
            System.out.println("Closed db connection ..");
        } catch (SQLException e) {
            System.err.println("Failed to close connection to the MySQL database ..");
            e.printStackTrace();
        }
    }
    
    /**
     * Clean up the database resources after the query has completed.
     * @param dbConn
     * @param prepStmt
     */
    private void cleanDBResources(Connection dbConn, PreparedStatement prepStmt)
    {
    	closeDBConnection(dbConn);
    	try {
			prepStmt.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
    }
    
	@Override
	public void createPaintRoom(int id, String name) {
		String sql = " INSERT INTO " + Constants.PAINTROOMS_TABLE + "(id, name, start_time, is_active) " + 
					 " VALUES(?, ?, UTC_TIMESTAMP() , ?)";

		Connection dbConn = getDBConnection();
		PreparedStatement prepStmt = null;
		try {
			prepStmt = dbConn.prepareStatement(sql);
			prepStmt.setInt(1, id);
			prepStmt.setString(2, name);
			prepStmt.setBoolean(3, true);
			
			prepStmt.execute();
		}
		catch(Exception e) {
			Logger.error("Failed to execute the query " + sql);
			e.printStackTrace();
		}
		finally {
			cleanDBResources(dbConn, prepStmt);
		}
		
		Logger.info("Created a new paintroom with name " + name + " .. ");
	}

	@Override
	public void createPainter(int paintRoomId, int painterId, String name, int brushSize, String burshColor) {
		String sql = " INSERT INTO " + Constants.PAINTERS_TABLE + "(id, name, brush_size, brush_color, start_time) " + 
				 " VALUES(?, ?, ?, ?, UTC_TIMESTAMP())";

		Connection dbConn = getDBConnection();
		PreparedStatement prepStmt = null;
		try {
			prepStmt = dbConn.prepareStatement(sql);
			prepStmt.setInt(1, painterId);
			prepStmt.setString(2, name);
			prepStmt.setInt(3, brushSize);
			prepStmt.setString(4, burshColor);
			
			prepStmt.execute();
		}
		catch(Exception e) {
			Logger.error("Failed to execute the query " + sql);
			e.printStackTrace();
		}
		finally {
			cleanDBResources(dbConn, prepStmt);
		}
		
		updatePaintRoomPaintersMap(paintRoomId, painterId, true);
		Logger.info("Created a new painter with name " + name + " .. ");
	}

	/**
	 * Updates the list of painters for a paintroom with their activity status.
	 * 
	 * @param paintRoomId
	 * @param painterId
	 * @param isActive
	 */
	@Override
	public void updatePaintRoomPaintersMap(int paintRoomId, int painterId, boolean isActive)
	{
		String insertSQL = " INSERT INTO " + Constants.PAINTROOM_TO_PAINTERS_MAP_TABLE + "(paint_room_id, painter_id, is_active) " + 
				 " VALUES(?, ?, 1)";
		String updateSQL = "UPDATE " + Constants.PAINTROOM_TO_PAINTERS_MAP_TABLE + 
				" SET is_active = 0 WHERE paint_room_id = ? AND painter_id = ?";
		
		Connection dbConn = getDBConnection();
		PreparedStatement prepStmt = null;
		try {
			if(isActive) {
				prepStmt = dbConn.prepareStatement(insertSQL);				
			}
			else {
				prepStmt = dbConn.prepareStatement(updateSQL);				
			}

			prepStmt.setInt(1, paintRoomId);
			prepStmt.setInt(2, painterId);
			
			prepStmt.execute();
		}
		catch(Exception e) {
			Logger.error("Failed to execute the query " + insertSQL);
			e.printStackTrace();
		}
		finally {
			cleanDBResources(dbConn, prepStmt);
		}
		
		if(isActive) {
			Logger.info("Added a new painter id " + painterId + " to paint room " + paintRoomId + " .. ");			
		}
		else {
			Logger.info("Deactivated a painter id " + painterId + " in the paintroom " + paintRoomId + " .. ");
		}
		
		checkAndDeactivateIfPaintRoomEmpty(paintRoomId);
	}
	
	/**
	 * Checks if the current paintroom is empty because of all the clients having disconnected.
	 * If yes, deactivates the paintroom session.
	 * 
	 * @param paintRoomId
	 */
	private void checkAndDeactivateIfPaintRoomEmpty(int paintRoomId)
	{
		String updateSQL = 
				"UPDATE paint_rooms p JOIN (SELECT paint_room_id, MAX(is_active) AS is_active " +
				"FROM paint_room_painters_map WHERE paint_room_id = ?) r ON r.paint_room_id = p.id " +
				"SET p.is_active = r.is_active";
		
		Connection dbConn = getDBConnection();
		PreparedStatement prepStmt = null;
		try {
			prepStmt = dbConn.prepareStatement(updateSQL);
			prepStmt.setInt(1, paintRoomId);
			
			prepStmt.execute();
		}
		catch(Exception e) {
			Logger.error("Failed to execute the query " + updateSQL);
			e.printStackTrace();
		}
		finally {
			cleanDBResources(dbConn, prepStmt);
		}
		
		Logger.info("Updated the status of paintroom " + paintRoomId + " .. ");
	}
	
	@Override
	public void insertPaintBrushEvents(int paintRoomId, Painter painter,
			int startX, int startY, int endX, int endY) {
		String sql = " INSERT INTO " + Constants.PAINTBRUSH_EVENTS_TABLE + "(paint_room_id, painter_id, start_x, start_y, end_x, end_y) " + 
				 " VALUES(?, ?, ?, ?, ?, ?)";

		Connection dbConn = getDBConnection();
		PreparedStatement prepStmt = null;
		try {
			prepStmt = dbConn.prepareStatement(sql);
			prepStmt.setInt(1, paintRoomId);
			prepStmt.setInt(2, painter.getId());
			prepStmt.setInt(3, startX);
			prepStmt.setInt(4, startY);
			prepStmt.setInt(5, endX);
			prepStmt.setInt(6, endY);			
			
			prepStmt.execute();
		}
		catch(Exception e) {
			Logger.error("Failed to execute the query " + sql);
			e.printStackTrace();
		}
		finally {
			cleanDBResources(dbConn, prepStmt);
		}
		
		//Logger.info("Created a new paint brush event for painter " + painter.getName() + " for board " + paintRoomId + " .. ");
	}

	@Override
	public List<PaintRoom> getActivePaintRooms() {
		List<PaintRoom> paintRooms = Lists.newArrayList();
		
		String sql = "SELECT id, name FROM " + Constants.PAINTROOMS_TABLE + " WHERE is_active=1";
		Connection dbConn = getDBConnection();
		PreparedStatement prepStmt = null;
		try {
			prepStmt = dbConn.prepareStatement(sql);
			ResultSet rs = prepStmt.executeQuery();
			while(rs.next()) {
				int id = rs.getInt("id");
				String name = rs.getString("name");
				paintRooms.add(new PaintRoom(id, name));
			}
		}
		catch(Exception e) {
			Logger.error("Failed to execute the query " + sql);
			e.printStackTrace();
		}
		finally {
			cleanDBResources(dbConn, prepStmt);
		}
		Logger.info("Returned " + paintRooms.size() + " active paintrooms ..");		

		return paintRooms;
	}

	@Override
	public List<PaintBrushEvent> getAllBrushEventsForPaintRoom(int paintRoomId) {
		List<PaintBrushEvent> brushEvents = Lists.newArrayList();
		
		String sql = " SELECT evt.id, painters.id AS painter_id, painters.name , painters.brush_size, painters.brush_color, evt.start_x, evt.start_y, evt.end_x, evt.end_y " +
				" FROM paint_events evt JOIN painters painters ON (evt.painter_id = painters.id) " +
				" WHERE evt.paint_room_id = ? " +
				" ORDER by evt.id";
		
		Map<Integer, Painter> painters = Maps.newHashMap();
		Connection dbConn = getDBConnection();
		PreparedStatement prepStmt = null;
		try {
			prepStmt = dbConn.prepareStatement(sql);
			prepStmt.setInt(1, paintRoomId);
			ResultSet rs = prepStmt.executeQuery();
			while(rs.next()) {
				int eventId = rs.getInt("id");
				int painterId = rs.getInt("painter_id");
				String painterName = rs.getString("name");
				int brushSize = rs.getInt("brush_size");
				String brushColor = rs.getString("brush_color");
				int startX = rs.getInt("start_x");
				int startY = rs.getInt("start_y");
				int endX = rs.getInt("end_x");
				int endY = rs.getInt("end_y");
				
				Painter painter = null;
				if(!painters.containsKey(painterId)) {
					painter = new Painter(painterId, painterName, brushSize, brushColor);
					painters.put(painterId, painter);
				}
				else {
					painter = painters.get(painterId);
				}
				
				PaintBrushEvent evt = new PaintBrushEvent(paintRoomId, painter, startX, startY, endX, endY, eventId);
				brushEvents.add(evt);
			}
		}
		catch(Exception e) {
			Logger.error("Failed to execute the query " + sql);
			e.printStackTrace();
		}
		finally {
			cleanDBResources(dbConn, prepStmt);
		}
		Logger.info("Returned " + brushEvents.size() + " total brush events for paint room " + paintRoomId + " .. ");				
		
		return brushEvents;
	}

}
