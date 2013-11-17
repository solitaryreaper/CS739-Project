package models;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ObjectNode;

import play.libs.Json;
import play.mvc.WebSocket;

/**
 * Represents each of the distributed client connecting to the web application
 * via websockets.
 * 
 * @author excelsior
 * 
 */
public class Painter {
	public String id;
	public final WebSocket.Out<JsonNode> channel;

	// Represents the outgoing channel from server to this client
	public Painter(WebSocket.Out<JsonNode> channel) {
		this.channel = channel;
	}

	public Painter(String id, WebSocket.Out<JsonNode> channel) {
		this.id = id;
		this.channel = channel;
	}

	@Override
	public String toString() {
		return "Painter [id=" + id + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Painter other = (Painter) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		return true;
	}

	
}
