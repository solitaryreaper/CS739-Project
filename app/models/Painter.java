package models;

import java.util.Date;

import org.codehaus.jackson.JsonNode;

import com.google.common.base.Objects;

import play.mvc.WebSocket;

/**
 * Represents each of the distributed client connecting to the web application
 * via websockets. In the case of collaborative editor, various clients can join via
 * web browser or mobile client.
 * 
 * @author excelsior
 * 
 */
public class Painter {
	private Integer id;
	private String name;
	private int brushSize;
	private String brushColor;
	private Date startTime;
	
	private WebSocket.Out<JsonNode> channel;

	// Represents the outgoing channel from server to this client
	public Painter(WebSocket.Out<JsonNode> channel) {
		this.channel = channel;
	}

	public Painter(String name, WebSocket.Out<JsonNode> channel) {
		this.name = name;
		this.channel = channel;
	}

	public Painter(String name, int brushSize, String brushColor) {
			this.name = name;
			this.brushSize = brushSize;
			this.brushColor = brushColor;
	}
	
	public Painter(Integer id, String name, int brushSize, String brushColor) {
		this.id = id;
		this.name = name;
		this.brushSize = brushSize;
		this.brushColor = brushColor;
	}
	
	@Override
	public String toString() {
		return Objects.toStringHelper(Painter.class.getSimpleName())
				.add("Id", getId())
				.add("Name", getName())
				.toString();
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(this.name);
	}

	@Override
	public boolean equals(Object obj) {
		if(obj instanceof Painter) {
			Painter that = (Painter)obj;
			return Objects.equal(this.name, that.name);
		}
		
		return false;
	}

	// Returns a unique numeric identifier for the user. Serves as a user id !!
	public int getId()
	{
		if(id == null) {
			id = hashCode();
		}
		
		return id;
	}
	
	public void setId(int id)
	{
		this.id = id;
	}
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getBrushSize() {
		return brushSize;
	}

	public void setBrushSize(int brushSize) {
		this.brushSize = brushSize;
	}

	public String getBrushColor() {
		return brushColor;
	}

	public void setBrushColor(String brushColor) {
		this.brushColor = brushColor;
	}

	public Date getStartTime() {
		return startTime;
	}

	public void setStartTime(Date startTime) {
		this.startTime = startTime;
	}

	public WebSocket.Out<JsonNode> getChannel() {
		return channel;
	}

	public void setChannel(WebSocket.Out<JsonNode> channel) {
		this.channel = channel;
	}
}
