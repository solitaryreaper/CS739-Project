import models.daemon.ServerMetaHandler;
import play.Application;
import play.GlobalSettings;
import play.Logger;

/**
 * Bootstraps the application with startup settings.
 * 
 * In our case, register this local server with the Session Manager to know it of this worker server's
 * availability to service requests.
 * 
 * @author excelsior
 *
 */
public class Global extends GlobalSettings{
	@Override
	  public void onStart(Application app) {
	    Logger.info("Application has started ..");
	    /*
	    boolean isServerRegistered = ServerMetaHandler.registerServer();
	    if(!isServerRegistered) {
	    	throw new RuntimeException("Failed to register the local worker server with session manager.");
	    }
	    else {
	    	Logger.info("Successfully registered the local worker server !!");
	    }
	    */
	  }  
	  
}
