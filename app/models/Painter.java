package models;

import org.codehaus.jackson.JsonNode;

import play.mvc.WebSocket;

import com.google.common.base.Objects;

/**
 * Represents each of the distributed client connecting to the web application
 * via websockets. In the case of collaborative editor, various clients can join via
 * web browser or mobile client.
 * 
 * @author excelsior
 * 
 */
public class Painter {
	private String name;
	private WebSocket.Out<JsonNode> channel;

	public Painter(String name)
	{
		this.name = name;
	}
	
	// Represents the outgoing channel from server to this client
	public Painter(WebSocket.Out<JsonNode> channel) {
		this.channel = channel;
	}

	public Painter(String name, WebSocket.Out<JsonNode> channel) {
		this.name = name;
		this.channel = channel;
	}

	@Override
	public String toString() {
		return Objects.toStringHelper(Painter.class.getSimpleName())
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

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public WebSocket.Out<JsonNode> getChannel() {
		return channel;
	}

	public void setChannel(WebSocket.Out<JsonNode> channel) {
		this.channel = channel;
	}
}
