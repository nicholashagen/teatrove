package org.teatrove.teaservlet.listeners;

import com.go.trove.util.PropertyMap;

public abstract class ExceptionListener implements Listener {
	
	public abstract void handle(Exception e);
	
}
