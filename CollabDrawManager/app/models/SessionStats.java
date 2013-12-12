package models;

import java.util.List;

/**
 * SessionTable
 * http://54.201.156.52:8080/CollabDraw/serverOps?operation=getSessionTable
 * [{"PreferredServers":["54.204.106.44","54.196.108.77"],"SessionId":"zen","Users":["zen1","zen2"]}]
 */

public class SessionStats 
{
	public String mSessionID;
	public List<String> mPreferredServers;
	public List<String> mUsers;
	
	public SessionStats(String sessionID, List<String> preferredServers, List<String> users)
	{
		mSessionID = sessionID;
		mPreferredServers = preferredServers;
		mUsers = users;
	}
}
