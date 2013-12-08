package models.utils;

import java.util.List;

import models.PaintBrushEvent;
import models.Painter;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;

import play.Logger;

import com.google.common.collect.Lists;

/**
 * Utility methods for processing JSON data.
 * 
 * @author excelsior
 *
 */
public class JSONUtils {
	
	/**
	 * Converts paint brush java model objects to a corresponding JSON representation.
	 * @param brushEvents
	 * @return
	 */
	public static List<JsonNode> convertPOJOToJSON(List<PaintBrushEvent> brushEvents)
	{
		JSONUtils utils = new JSONUtils();
		List<JsonNode> jsonList = Lists.newArrayList();
		ObjectMapper mapper = new ObjectMapper();
		for(PaintBrushEvent event : brushEvents) {
			Painter p = event.getPainter();
			DrawEvent clientFormatEvent = utils.new DrawEvent(p.getName(), event.getStartPointX(), 
					event.getStartPointY(), event.getEndPointX(), event.getEndPointY());
			try {
				String json = mapper.writeValueAsString(clientFormatEvent);
				JsonNode jsonNode = mapper.readTree(json);
				Logger.info("JSON conversion : " + jsonNode.toString());
				if(jsonNode != null) {
					jsonList.add(jsonNode);									
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		Logger.info("Converted " + jsonList.size() + " objects to JSON Nodes ..");
		return jsonList;
	}
	
	/**
	 * Paint brush event in the client expected format.
	 * @author excelsior
	 *
	 */
	private class DrawEvent
	{
		public String name;
		
		public int start_x;
		public int start_y;
		public int end_x;
		public int end_y;
		
		public DrawEvent(String name, int start_x, int start_y, int end_x, int end_y) {
			super();
			this.name = name;
			this.start_x = start_x;
			this.start_y = start_y;
			this.end_x = end_x;
			this.end_y = end_y;
		}
	}
}
