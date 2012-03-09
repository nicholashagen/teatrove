package org.teatrove.tea.annotations;

import org.teatrove.tea.compiler.Token;
import org.teatrove.trove.io.SourceReader;

public interface TokenCallback {
    Token scanToken(SourceReader reader);
}
