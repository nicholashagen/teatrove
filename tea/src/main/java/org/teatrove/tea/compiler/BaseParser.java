package org.teatrove.tea.compiler;

import java.io.IOException;

import org.teatrove.tea.modules.core.CoreTokens;
import org.teatrove.tea.parsetree.Name;
import org.teatrove.tea.parsetree.Statement;
import org.teatrove.tea.parsetree.StatementList;
import org.teatrove.tea.parsetree.TypeName;

public class BaseParser {

    Scanner scanner;

    public Statement readStatement() {
        // TODO: process @Statement handlers
        return new StatementList(peekToken().getSourceInfo(), new Statement[0]);
    }

    public Name readName() {
        // TODO: process @Name handlers
        // name.invoke(parser) [setup a blocker so that if readName invoked
        //    while in invoke, exception is thrown or create new delegate
        //    parser whose implementation is the below
        Token token = readToken(Tokens.IDENT);
        return new Name(token.getSourceInfo(), token.getStringValue());
    }

    public TypeName readType() {
        // TODO: process @Type handlers
        Name name = readName();
        SourceInfo source = name.getSourceInfo();

        int dims = 0;
        while (peekToken().getID() == CoreTokens.LBRACK) {
            dims++;
            readToken(CoreTokens.LBRACK);
            Token end = readToken(CoreTokens.RBRACK);
            source.setEndPosition(end.getSourceInfo());
        }

        return new TypeName(source, name, dims);
    }

    public Token readToken() {
        return readToken(new int[] { Tokens.ANY });
    }

    public Token readToken(int id) {
        return readToken(new int[] { id });
    }

    public Token readToken(int... ids) {
        try {
            Token token = scanner.readToken();

            boolean found = false;
            for (int id : ids) {
                if (token.getID() == id) {
                    found = true;
                    break;
                }
            }

            if (!found) {
                // error("unexpected token");
            }

            return token;
        }
        catch (IOException ioe) {
            // error("error reading token", ioe);
            return null;
        }
    }

    public Token peekToken() {
        return peekToken(new int[] { Tokens.ANY });
    }

    public Token peekToken(int id) {
        return peekToken(new int[] { id });
    }

    public Token peekToken(int... ids) {
        try {
            Token token = scanner.peekToken();

            boolean found = false;
            for (int id : ids) {
                if (token.getID() == id) {
                    found = true;
                    break;
                }
            }

            if (!found) {
                // error("unexpected token");
            }

            return token;
        }
        catch (IOException ioe) {
            // error("error reading token", ioe);
            return null;
        }
    }
}
