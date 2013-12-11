import java.util.ArrayList;

import com.sun.corba.se.spi.activation.Server;

public class ServerStatus 
{
	public boolean mIsReachable;
	public ArrayList<String> mConnectedClients = null;
	public String mServerIP;
	
	public ServerStatus(String ip, boolean isReachable, ArrayList<String> connectedClients)
	{
		mIsReachable = isReachable;
		mConnectedClients = connectedClients;
		mServerIP = mServerIP;
	}
}
