package models;

import com.google.common.base.Objects;

/**
 * Models a single brush event on the canvas.
 * 
 * @author excelsior
 *
 */
public class PaintBrushEvent{
	private Integer eventId = null; // db strictly increasing identifier for this event

	private int paintRoomId;
	private Painter painter;

	private int startPointX;
	private int startPointY;
	private int endPointX;
	private int endPointY;

	public PaintBrushEvent(int paintRoomId, Painter painter, int startPointX,
			int startPointY, int endPointX, int endPointY, int eventId) {
		super();
		this.paintRoomId = paintRoomId;
		this.painter = painter;
		this.startPointX = startPointX;
		this.startPointY = startPointY;
		this.endPointX = endPointX;
		this.endPointY = endPointY;
		this.eventId = eventId;
	}
	
	@Override
	public int hashCode()
	{
		return Objects.hashCode(this.paintRoomId, this.painter, this.eventId);
	}
	
	@Override
	public boolean equals(Object obj)
	{
		if(obj instanceof PaintBrushEvent) {
			PaintBrushEvent that = (PaintBrushEvent)obj;
			return Objects.equal(this.paintRoomId, that.paintRoomId) &&
					Objects.equal(this.painter, that.painter) &&
					Objects.equal(this.eventId, that.eventId);
		}
		
		return false;
	}

	public Integer getEventId() {
		return eventId;
	}

	public void setEventId(Integer eventId) {
		this.eventId = eventId;
	}

	public int getPaintRoomId() {
		return paintRoomId;
	}

	public void setPaintRoomId(int paintRoomId) {
		this.paintRoomId = paintRoomId;
	}

	public Painter getPainter() {
		return painter;
	}

	public void setPainter(Painter painter) {
		this.painter = painter;
	}

	public int getStartPointX() {
		return startPointX;
	}

	public void setStartPointX(int startPointX) {
		this.startPointX = startPointX;
	}

	public int getStartPointY() {
		return startPointY;
	}

	public void setStartPointY(int startPointY) {
		this.startPointY = startPointY;
	}

	public int getEndPointX() {
		return endPointX;
	}

	public void setEndPointX(int endPointX) {
		this.endPointX = endPointX;
	}

	public int getEndPointY() {
		return endPointY;
	}

	public void setEndPointY(int endPointY) {
		this.endPointY = endPointY;
	}
	
}
