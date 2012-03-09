package org.teatrove.tea.compiler;

import java.io.IOException;
import java.io.OutputStream;

public abstract class CodeOutput {
	public abstract OutputStream getOutputStream()
		throws IOException;

	public abstract OutputStream getOutputStream(String innerClass)
		throws IOException;
}
