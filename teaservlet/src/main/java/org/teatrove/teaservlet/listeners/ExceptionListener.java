package org.teatrove.teaservlet.listeners;

import com.go.trove.util.PropertyMap;

public interface ExceptionListener extends Listener {
	
	public void handle(Exception e);
	
}
