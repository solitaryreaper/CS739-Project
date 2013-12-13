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
            //Logger.debug("Connected to the MySQL database ..");
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
            //Logger.debug("Closed db connection ..");
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
	public void insertPaintBrushEvents(String paintRoom, String painter, int startX, int startY, int endX, int endY) {
		String sql = " INSERT INTO " + Constants.PAINTBRUSH_EVENTS_TABLE + "(paint_room, painter, start_x, start_y, end_x, end_y) " + 
				 " VALUES(?, ?, ?, ?, ?, ?)";

		Connection dbConn = getDBConnection();
		PreparedStatement prepStmt = null;
		try {
			prepStmt = dbConn.prepareStatement(sql);
			prepStmt.setString(1, paintRoom);
			prepStmt.setString(2, painter);
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
	public List<PaintBrushEvent> getAllBrushEventsForPaintRoom(String paintRoom) {
		List<PaintBrushEvent> brushEvents = Lists.newArrayList();
		
		String sql = " SELECT id, paint_room, painter, start_x, start_y, end_x, end_y " +
				"FROM collabdraw.paint_room_events WHERE paint_room = ? " +
				"ORDER BY id DESC";
		
		Map<String, Painter> painters = Maps.newHashMap();
		Connection dbConn = getDBConnection();
		PreparedStatement prepStmt = null;
		try {
			prepStmt = dbConn.prepareStatement(sql);
			prepStmt.setString(1, paintRoom);
			ResultSet rs = prepStmt.executeQuery();
			while(rs.next()) {
				int eventId = rs.getInt("id");
				String painterName = rs.getString("painter");
				int startX = rs.getInt("start_x");
				int startY = rs.getInt("start_y");
				int endX = rs.getInt("end_x");
				int endY = rs.getInt("end_y");
				
				Painter painter = null;
				if(!painters.containsKey(painterName)) {
					painter = new Painter(painterName);
					painters.put(painterName, painter);
				}
				else {
					painter = painters.get(painterName);
				}
				
				PaintBrushEvent evt = new PaintBrushEvent(paintRoom, painter, startX, startY, endX, endY, eventId);
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
		Logger.info("Returned " + brushEvents.size() + " total brush events for paint room " + paintRoom + " .. ");				
		
		return brushEvents;
	}

}
