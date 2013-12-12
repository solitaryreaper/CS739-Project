package models;

/**
 * ServerStatus
 * http://54.201.156.52:8080/CollabDraw/serverOps?operation=getServerStats
 * [{"clientLoad":0,"lastKnowHeartbeat":1386758286286,"preferredServerFor":1,"serverIP":"54.204.106.44","status":"unreachable"},
 *  {"clientLoad":0,"lastKnowHeartbeat":1386758368578,"preferredServerFor":1,"serverIP":"54.196.108.77","status":"unreachable"}]
**/

public class ServerStats 
{
	public String mServerIP;
	public boolean mIsReachable;
	public int mSessionServingCount;
	public int mClientServingCount;
	public long mLastHeartBeat;
	
	public ServerStats(String ip, boolean isReachable, int sessionServingCount, int clientServingCount, long lastHeartBeat)
	{
		mServerIP = ip;
		mIsReachable = isReachable;
		mSessionServingCount = sessionServingCount;
		mClientServingCount = clientServingCount;
		mLastHeartBeat = lastHeartBeat;
	}
}
