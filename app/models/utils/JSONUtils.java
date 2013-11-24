package models.utils;

import java.io.IOException;
import java.util.List;

import models.PaintBrushEvent;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonProcessingException;
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
		List<JsonNode> jsonList = Lists.newArrayList();
		ObjectMapper mapper = new ObjectMapper();
		for(PaintBrushEvent event : brushEvents) {
			try {
				String json = mapper.writeValueAsString(event);
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
}
