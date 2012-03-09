/* ====================================================================
 * Tea - Copyright (c) 1997-2000 Walt Disney Internet Group
 * ====================================================================
 * The Tea Software License, Version 1.1
 *
 * Copyright (c) 2000 Walt Disney Internet Group. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution,
 *    if any, must include the following acknowledgment:
 *       "This product includes software developed by the
 *        Walt Disney Internet Group (http://opensource.go.com/)."
 *    Alternately, this acknowledgment may appear in the software itself,
 *    if and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "Tea", "TeaServlet", "Kettle", "Trove" and "BeanDoc" must
 *    not be used to endorse or promote products derived from this
 *    software without prior written permission. For written
 *    permission, please contact opensource@dig.com.
 *
 * 5. Products derived from this software may not be called "Tea",
 *    "TeaServlet", "Kettle" or "Trove", nor may "Tea", "TeaServlet",
 *    "Kettle", "Trove" or "BeanDoc" appear in their name, without prior
 *    written permission of the Walt Disney Internet Group.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE WALT DISNEY INTERNET GROUP OR ITS
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY
 * OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * ====================================================================
 *
 * For more information about Tea, please see http://opensource.go.com/.
 */

package org.teatrove.tea.compiler;


import java.io.IOException;
import java.io.StringReader;
import java.lang.annotation.Annotation;
import java.util.Vector;

import org.teatrove.tea.annotations.TemplateParser;
import org.teatrove.tea.parsetree.Template;
import org.teatrove.trove.io.SourceReader;

/******************************************************************************
 * A Parser creates the parse tree for a template by reading tokens emitted by
 * a {@link Scanner}. The parse tree represents the entire template as a
 * data structure composed of specialized nodes. Add an {@link ErrorListener}
 * to capture any syntax errors detected by the Parser.
 *
 * @author Brian S O'Neill
 * @version
 * <!--$$Revision:--> 66 <!-- $-->, <!--$$JustDate:--> 11/14/03 <!-- $-->
 */
public class Parser {
    private Scanner mScanner;
    private Parsers mParsers;
    private CompilationUnit mUnit;

    private Vector<ErrorListener> mListeners =
        new Vector<ErrorListener>(1);

    private int mErrorCount = 0;
    private int mEOFErrorCount = 0;

    private MessageFormatter mFormatter;

    public Parser(Scanner scanner, Annotations annotations) {
        this(scanner, annotations, null);
    }

    public Parser(Scanner scanner, Annotations annotations,
                  CompilationUnit unit) {
        mScanner = scanner;
        mUnit = unit;
        mParsers = new Parsers(annotations);
        mFormatter = MessageFormatter.lookup(this);
    }

    public void addErrorListener(ErrorListener listener) {
        mListeners.addElement(listener);
    }

    public void removeErrorListener(ErrorListener listener) {
        mListeners.removeElement(listener);
    }

    private void dispatchParseError(ErrorEvent e) {
        mErrorCount++;

        synchronized (mListeners) {
            for (int i = 0; i < mListeners.size(); i++) {
                ((ErrorListener)mListeners.elementAt(i)).compileError(e);
            }
        }
    }

    public void error(String str, Token culprit) {
        str = mFormatter.format(str);

        if (culprit.getID() == Tokens.EOF) {
            if (mEOFErrorCount++ == 0) {
                str = mFormatter.format("error.at.end", str);
            }
            else {
                return;
            }
        }

        dispatchParseError(new ErrorEvent(this, str, culprit, mUnit));
    }

    public void error(String str, String arg, Token culprit) {
        str = mFormatter.format(str, arg);

        if (culprit.getID() == Tokens.EOF) {
            if (mEOFErrorCount++ == 0) {
                str = mFormatter.format("error.at.end", str);
            }
            else {
                return;
            }
        }

        dispatchParseError(new ErrorEvent(this, str, culprit, mUnit));
    }

    public void error(String str, SourceInfo info) {
        str = mFormatter.format(str);
        dispatchParseError(new ErrorEvent(this, str, info, mUnit));
    }

    public int getErrorCount() {
        return mErrorCount;
    }

    public Token read() throws IOException {
        return mScanner.readToken();
    }

    public Token read(int id) throws IOException {
        Token token = mScanner.readToken();
        if (token.getID() != id) {
            error("unexpected id: " + id, token);
            return null;
        }

        return token;
    }

    public Token peek() throws IOException {
        return mScanner.peekToken();
    }

    public void unread(Token token) throws IOException {
        mScanner.unreadToken(token);
    }

    public Template parse() throws IOException {
        return (Template) parse(TemplateParser.class);
    }

    @SuppressWarnings({ "unchecked" })
    public <T> T parse(Class<T> parserType)
        throws IOException {

        ParserDefinition[] definitions = mParsers.getParsers(parserType);
        if (definitions.length == 0) {
            throw new IOException(
                "unprocessed type: ".concat(parserType.getName()));
        }

        return (T) parse(definitions);
    }

    @SuppressWarnings("unchecked")
    public <T> T parse(Class<? extends Annotation> annotationType,
                       Class<T> parserType)
        throws IOException {

        ParserDefinition[] definitions =
            mParsers.getParsers(annotationType, parserType);
        if (definitions.length == 0) {
            throw new IOException(
                "unprocessed type: " +
                annotationType.getName() + '/' + parserType.getName()
            );
        }

        return (T) parse(definitions);
    }

    protected Object parse(ParserDefinition[] definitions) {
        // load first processor that returns valid result
        for (ParserDefinition definition : definitions) {
            Object result =
                definition.invoke(new Chain(mParsers, definition), this);
            if (result != null) { return result; }
        }

        // none found
        return null;
    }

    /** Test program */
    public static void main(String[] arg) throws Exception {

        Annotations annotations = new Annotations();
        Scanner scanner = new Scanner
        (
            new SourceReader
            (
                new StringReader
                (
                    "<% template test(int id, String name) { ... } " +
                    "a = 5; b = a; ...; d = a + b; e = #(a, b); " +
                    "foreach (f in 1..5) { f } " +
                    "d " +
                    "%>"
                ), "<%", "%>"
            ),
            annotations
        );

        Parser parser = new Parser(scanner, annotations);
        Template t = parser.parse();
        System.out.println("TEMPLATE: " + t);
        if (t == null) {
            throw new IllegalStateException("invalid template");
        }

        // on invoke
            // foreach
                // if @Intro
                    // invoke

        // chain.define
            // reset
            // implement interface
            // on first invoke, setup by method
            // invoke first tree

        // chain.next
            // invoke next in tree


        // load all parsers
        // organize
        // load first
        // proxy each parser
        // while true
            // invoke top of tree (by before/around, priority)
            // if null, next in tree

        // Tester.test(arg);
    }
}
