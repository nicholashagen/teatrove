package org.teatrove.teaservlet.listeners;

import javax.servlet.ServletContext;

import org.teatrove.tea.log.TeaLog;
import org.teatrove.tea.log.TeaStackTraceLine;
import org.teatrove.trove.log.Log;
import com.go.trove.util.PropertyMap;
import com.newrelic.api.agent.NewRelic;

public class NewRelicExceptionListener implements ExceptionListener {
	
	public NewRelicExceptionListener() {
		
	}
	
	public void handle(Exception e) {
		java.util.HashMap<String, String> errorParams = new java.util.HashMap<String, String>();
		String newRelicMessage = null;
		
    	TeaStackTraceLine [] lines = TeaLog.getTeaStackTraceLines(e);
    	String stackTrace = TeaLog.printTeaStackTraceLines(lines);
    	
    	String rootCause = "";
    	Throwable x = e;
    	while ( x.getCause() != null ) {
    		x = x.getCause();
    		rootCause = x.getClass().getCanonicalName();
    	}
    	for( TeaStackTraceLine oneLine : lines ) {
    		if ( oneLine.getTemplateName() != null ) {
    			newRelicMessage = rootCause + " in template " + oneLine.getTemplateName() + " at line " + oneLine.getLineNumber();
    			
    			errorParams.put("Template", oneLine.getTemplateName());
    			errorParams.put("Line Number", oneLine.getLineNumber().toString());
    			errorParams.put("Complete Line", oneLine.getLine());
    			errorParams.put("Stack Trace", stackTrace);
    		}	            		
    	}
    	if ( newRelicMessage == null ) {
    		newRelicMessage = stackTrace;
    	}
    	NewRelic.noticeError(newRelicMessage, errorParams);
	}
}
