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
import java.util.Stack;
import java.util.Vector;

import org.teatrove.trove.io.SourceReader;

/******************************************************************************
 * A Scanner breaks up a source file into its basic elements, called
 * {@link Token Tokens}. Add an {@link ErrorListener} to capture any syntax
 * errors detected by the Scanner.
 *
 * @author Brian S O'Neill
 * @version
 * <!--$$Revision:--> 47 <!-- $-->, <!--$$JustDate:--> 11/14/03 <!-- $-->
 */
public class Scanner {
    private Tokens mTokens;
    private SourceReader mSource;
    private CompilationUnit mUnit;

    private boolean mEmitSpecial;

    /** StringBuilder for temporary use. */
    private StringBuilder mWord = new StringBuilder(20);

    /** The scanner supports any amount of lookahead. */
    private Stack<Token> mLookahead = new Stack<Token>();

    private Token mEOFToken;

    private Vector<ErrorListener> mListeners = new Vector<ErrorListener>(1);
    private int mErrorCount = 0;

    private MessageFormatter mFormatter;

    public Scanner(SourceReader in, Annotations annotations) {
        this(in, annotations, null);
    }

    public Scanner(SourceReader in, Annotations annotations,
                   CompilationUnit unit) {
        mSource = in;
        mUnit = unit;
        mTokens = new Tokens(annotations);
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
                mListeners.elementAt(i).compileError(e);
            }
        }
    }

    private void error(String str, SourceInfo info) {
        dispatchParseError
            (new ErrorEvent(this, mFormatter.format(str), info, mUnit));
    }

    private void error(String str) {
        error(str, new SourceInfo(mSource.getLineNumber(),
                                  mSource.getStartPosition(),
                                  mSource.getEndPosition()));
    }

    /**
     * Passing true causes Scanner to emit additional tokens that should not
     * be bassed into a Parser. These are {@link Token.COMMENT},
     * {@link Token.ENTER_CODE}, and {@link Token.ENTER_TEXT}. By default,
     * these special tokens are not emitted.
     */
    public void emitSpecialTokens(boolean enable) {
        mEmitSpecial = enable;
    }

    /**
     * Returns EOF as the last token.
     */
    public synchronized Token readToken() throws IOException {
        if (mLookahead.empty()) {
            return scanToken();
        }
        else {
            return mLookahead.pop();
        }
    }

    /**
     * Returns EOF as the last token.
     */
    public synchronized Token peekToken() throws IOException {
        if (mLookahead.empty()) {
            return mLookahead.push(scanToken());
        }
        else {
            return mLookahead.peek();
        }
    }

    public synchronized void unreadToken(Token token) throws IOException {
        mLookahead.push(token);
    }

    public void close() throws IOException {
        mSource.close();
    }

    public int getErrorCount() {
        return mErrorCount;
    }

    private Token scanToken() throws IOException {
        int c;
        int peek;

        int startPos;

        while ((c = mSource.read()) != -1) {
            switch (c) {

            // handle special case of entering text-block (non code)
            case SourceReader.ENTER_TEXT:
                Token enter;
                if (mEmitSpecial) {
                    enter = mTokens.makeToken(Tokens.ENTER_TEXT, "ENTER_TEXT",
                                              mSource.getEndTag(), mSource);
                }
                else {
                    enter = null;
                }

                Token t = scanText(c);

                if (mEmitSpecial) {
                    if (t.getStringValue().length() > 0) {
                        mLookahead.push(t);
                    }
                    return enter;
                }

                if (t.getStringValue().length() == 0) {
                    continue;
                }

                return t;

            // handle special case of entering code block
            case SourceReader.ENTER_CODE:
                // Entering code while in code is illegal. Just let the parser
                // deal with it.
                return mTokens.makeToken(Tokens.ENTER_CODE, "ENTER_CODE",
                                         mSource.getBeginTag(), mSource);

            // handle floating-point literals
            case '.':
                peek = mSource.peek();
                if (peek >= '0' && peek <= '9') {
                    error("number.decimal.start");
                    return scanNumber(c);
                }

                break;

            // handle comments
            case '/':
                startPos = mSource.getStartPosition();
                peek = mSource.peek();

                if (peek == '*') {
                    mSource.read();
                    mSource.ignoreTags(true);
                    t = scanMultiLineComment(startPos);
                    mSource.ignoreTags(false);
                    if (mEmitSpecial) {
                        return t;
                    }
                    else {
                        continue;
                    }
                }
                else if (peek == '/') {
                    mSource.read();
                    t = scanOneLineComment(startPos);
                    if (mEmitSpecial) {
                        return t;
                    }
                    else {
                        continue;
                    }
                }
                else {
                    break;
                }

            // handle quoted strings
            case '\"':
            case '\'':
                mSource.ignoreTags(true);
                t = scanString(c);
                mSource.ignoreTags(false);
                return t;

            // handle numeric literals
            case '0': case '1': case '2': case '3': case '4':
            case '5': case '6': case '7': case '8': case '9':
                return scanNumber(c);

            // handle identifier/keyword literals
            case 'a': case 'b': case 'c': case 'd': case 'e':
            case 'f': case 'g': case 'h': case 'i': case 'j':
            case 'k': case 'l': case 'm': case 'n': case 'o':
            case 'p': case 'q': case 'r': case 's': case 't':
            case 'u': case 'v': case 'w': case 'x': case 'y':
            case 'z':
            case 'A': case 'B': case 'C': case 'D': case 'E':
            case 'F': case 'G': case 'H': case 'I': case 'J':
            case 'K': case 'L': case 'M': case 'N': case 'O':
            case 'P': case 'Q': case 'R': case 'S': case 'T':
            case 'U': case 'V': case 'W': case 'X': case 'Y':
            case 'Z': case '_':
                return scanIdentifier(c);

            // handle whitespace
            case ' ':
            case '\0':
            case '\t':
            case '\r':
            case '\n':
                continue;

            default:
                if (Character.isWhitespace((char)c)) {
                    continue;
                }

                if (Character.isLetter((char)c)) {
                    return scanIdentifier(c);
                }
            }

            // lookup token for best match
            Token token = mTokens.scanToken(c, mSource);
            if (token == null) {
                token = mTokens.makeToken(Tokens.UNKNOWN, "UNKNOWN",
                                          String.valueOf((char)c), mSource);
            }

            return token;
        }

        if (mEOFToken == null) {
            mEOFToken = mTokens.makeToken(Tokens.EOF, "EOF", mSource);
        }

        return mEOFToken;
    }

    // The ENTER_TEXT code has already been scanned when this is called.
    private Token scanText(int c) throws IOException {
        // Read first character in text so that source info does not include
        // tags.
        c = mSource.read();

        int startLine = mSource.getLineNumber();
        int startPos = mSource.getStartPosition();
        int endPos = mSource.getEndPosition();
        StringBuilder buf = new StringBuilder(256);

        while (c != -1) {
            if (c == SourceReader.ENTER_CODE) {
                if (mEmitSpecial) {
                    mLookahead.push
                    (
                        mTokens.makeToken(Tokens.ENTER_CODE, "ENTER_CODE",
                                          mSource.getBeginTag(), mSource)
                    );
                }
                break;
            }
            else if (c == SourceReader.ENTER_TEXT) {
                buf.append(mSource.getEndTag());
            }
            else {
                buf.append((char)c);
            }

            if (mSource.peek() < 0) {
                endPos = mSource.getEndPosition();
            }

            c = mSource.read();
        };

        if (c == -1) {
            // If the last token in the source file is text, trim all trailing
            // whitespace from it.

            int length = buf.length();

            int i;
            for (i = length - 1; i >= 0; i--) {
                if (buf.charAt(i) > ' ') {
                    break;
                }
            }

            buf.setLength(i + 1);
        }

        String str = buf.toString();
        return mTokens.makeToken(Tokens.STRING, "STRING", str,
                                startLine, startPos, endPos);
    }

    private Token scanString(int delimiter) throws IOException {
        int c;
        int startLine = mSource.getLineNumber();
        int startPos = mSource.getStartPosition();
        mWord.setLength(0);

        while ( (c = mSource.read()) != -1 ) {
            if (c == delimiter) {
                break;
            }

            if (c == '\n' || c == '\r') {
                error("string.newline");
                break;
            }

            if (c == '\\') {
                int next = mSource.read();
                switch (next) {
                case '0':
                    c = '\0';
                    break;
                case 'b':
                    c = '\b';
                    break;
                case 't':
                    c = '\t';
                    break;
                case 'n':
                    c = '\n';
                    break;
                case 'f':
                    c = '\f';
                    break;
                case 'r':
                    c = '\r';
                    break;
                case '\\':
                    c = '\\';
                    break;
                case '\'':
                    c = '\'';
                    break;
                case '\"':
                    c = '\"';
                    break;
                default:
                    error("escape.code");
                    c = next;
                    break;
                }
            }

            mWord.append((char)c);
        }

        if (c == -1) {
            error("string.eof");
        }

        Token t = mTokens.makeToken(Tokens.STRING, "STRING", mWord.toString(),
                                    startLine, startPos,
                                    mSource.getEndPosition());

        return t;
    }

    // The first character has already been scanned when this is called.
    private Token scanNumber(int c) throws IOException {
        int startLine = mSource.getLineNumber();
        int startPos = mSource.getStartPosition();
        mWord.setLength(0);

        int errorPos = -1;

        // 0 is decimal int,
        // 1 is hex int,
        // 2 is decimal long,
        // 3 is hex long,
        // 4 is float,
        // 5 is double,
        // 6 is auto-double by decimal
        // 7 is auto-double by exponent ('e' or 'E')
        int type = 0;

        if (c == '0') {
            if (mSource.peek() == 'x' || mSource.peek() == 'X') {
                type = 1;
                mSource.read(); // absorb the 'x'
                c = mSource.read(); // get the first digit after the 'x'
            }
        }

        for (; c != -1; c = mSource.read()) {
            if (c == '.') {
                int peek = mSource.peek();
                if (peek == '.') {
                    mSource.unread();
                    break;
                }
                else {
                    if (peek < '0' || peek > '9') {
                        error("number.decimal.end");
                    }

                    mWord.append((char)c);

                    if (type == 0) {
                        type = 6;
                    }
                    else if (errorPos < 0) {
                        errorPos = mSource.getStartPosition();
                    }

                    continue;
                }
            }

            if (c >= '0' && c <= '9') {
                mWord.append((char)c);

                if (type == 2 || type == 3 || type == 4 || type == 5) {
                    if (errorPos < 0) {
                        errorPos = mSource.getStartPosition();
                    }
                }

                continue;
            }

            if ((c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F')) {
                if (type == 1) {
                    mWord.append((char)c);
                    continue;
                }

                if (c == 'f' || c == 'F') {
                    if (type == 0 || type == 6 || type == 7) {
                        type = 4;
                        continue;
                    }
                }
                else if (c == 'd' || c == 'D') {
                    if (type == 0 || type == 6 || type == 7) {
                        type = 5;
                        continue;
                    }
                }
                else if (c == 'e' || c == 'E') {
                    if (type == 0 || type == 6) {
                        mWord.append((char)c);
                        type = 7;
                        int peek = mSource.peek();
                        if (peek == '+' || peek == '-') {
                            mWord.append((char)mSource.read());
                        }
                        continue;
                    }
                }

                mWord.append((char)c);

                if (errorPos < 0) {
                    errorPos = mSource.getStartPosition();
                }

                continue;
            }

            if (c == 'l' || c == 'L') {
                if (type == 0) {
                    type = 2;
                }
                else if (type == 1) {
                    type = 3;
                }
                else {
                    mWord.append((char)c);
                    if (errorPos < 0) {
                        errorPos = mSource.getStartPosition();
                    }
                }

                continue;
            }

            if (Character.isLetterOrDigit((char)c)) {
                mWord.append((char)c);

                if (errorPos < 0) {
                    errorPos = mSource.getStartPosition();
                }
            }
            else {
                mSource.unread();
                break;
            }
        }

        String str = mWord.toString();
        int endPos = mSource.getEndPosition();
        Token token;

        if (errorPos >= 0) {
            token = mTokens.makeToken(Tokens.NUMBER, "NUMBER", str,
                                      startLine, startPos, endPos, errorPos);
        }
        else {
            try {
                switch (type) {
                case 0:
                default:
                    try {
                        token = new IntToken
                            (startLine, startPos, endPos,
                             Integer.parseInt(str));
                    }
                    catch (NumberFormatException e) {
                        token = new LongToken
                            (startLine, startPos, endPos, Long.parseLong(str));
                    }
                    break;
                case 1:
                    try {
                        token = new IntToken
                            (startLine, startPos, endPos, parseHexInt(str));
                    }
                    catch (NumberFormatException e) {
                        token = new LongToken
                            (startLine, startPos, endPos, parseHexLong(str));
                    }
                    break;
                case 2:
                    token = new LongToken
                        (startLine, startPos, endPos, Long.parseLong(str));
                    break;
                case 3:
                    token = new LongToken
                        (startLine, startPos, endPos, parseHexLong(str));
                    break;
                case 4:
                    token = new FloatToken
                        (startLine, startPos, endPos, Float.parseFloat(str));
                    break;
                case 5:
                case 6:
                case 7:
                    token = new DoubleToken
                        (startLine, startPos, endPos, Double.parseDouble(str));
                    break;
                }
            }
            catch (NumberFormatException e) {
                token = new IntToken(startLine, startPos, endPos, 0);
                error("number.range", token.getSourceInfo());
            }
        }

        return token;
    }

    private int parseHexInt(String str) {
        if (str.length() > 8) {
            // Strip off any leading zeros.
            while (str.charAt(0) == '0') {
                str = str.substring(1);
            }
        }

        try {
            return Integer.parseInt(str, 16);
        }
        catch (NumberFormatException e) {
            if (str.length() == 8) {
                return (int)Long.parseLong(str, 16);
            }
            else {
                throw e;
            }
        }
    }

    private long parseHexLong(String str) {
        if (str.length() > 16) {
            // Strip off any leading zeros.
            while (str.charAt(0) == '0') {
                str = str.substring(1);
            }
        }

        try {
            return Long.parseLong(str, 16);
        }
        catch (NumberFormatException e) {
            if (str.length() == 16) {
                long v1 = Long.parseLong(str.substring(0, 8), 16);
                long v2 = Long.parseLong(str.substring(8), 16);
                return v1 << 32 + v2 & 0xffffffffL;
            }
            else {
                throw e;
            }
        }
    }

    // The first character has already been scanned when this is called.
    private Token scanIdentifier(int c) throws IOException {
        int startLine = mSource.getLineNumber();
        int startPos = mSource.getStartPosition();
        int endPos = mSource.getEndPosition();
        mWord.setLength(0);

        mWord.append((char)c);

    loop:
        while ( (c = mSource.peek()) != -1 ) {
            switch (c) {
            case 'a': case 'b': case 'c': case 'd': case 'e':
            case 'f': case 'g': case 'h': case 'i': case 'j':
            case 'k': case 'l': case 'm': case 'n': case 'o':
            case 'p': case 'q': case 'r': case 's': case 't':
            case 'u': case 'v': case 'w': case 'x': case 'y':
            case 'z':
            case 'A': case 'B': case 'C': case 'D': case 'E':
            case 'F': case 'G': case 'H': case 'I': case 'J':
            case 'K': case 'L': case 'M': case 'N': case 'O':
            case 'P': case 'Q': case 'R': case 'S': case 'T':
            case 'U': case 'V': case 'W': case 'X': case 'Y':
            case 'Z': case '_':
            case '0': case '1': case '2': case '3': case '4':
            case '5': case '6': case '7': case '8': case '9':
                mSource.read();
                endPos = mSource.getEndPosition();
                mWord.append((char)c);
                continue loop;
            }

            if (Character.isLetterOrDigit((char)c)) {
                mSource.read();
                endPos = mSource.getEndPosition();
                mWord.append((char)c);
            }
            else {
                break;
            }
        }

        KeywordDefinition def = mTokens.findKeyword(mWord.toString());
        Token t = null;
        if (def != null) {
            t = mTokens.makeToken(def.getId(), def.getName(),
                                  startLine, startPos, endPos);
        }
        else {
            t = mTokens.makeToken(Tokens.IDENT, "IDENT", mWord.toString(),
                                  startLine, startPos, endPos);
        }

        mWord.setLength(0);
        return t;
    }

    // The two leading slashes have already been scanned when this is
    // called.
    private Token scanOneLineComment(int startPos) throws IOException {
        int c;
        int startLine = mSource.getLineNumber();
        int endPos = mSource.getEndPosition();
        mWord.setLength(0);
        mWord.append('/').append('/');

        while ( (c = mSource.peek()) != -1 ) {
            if (c == '\r' || c == '\n') {
                break;
            }

            mSource.read();
            mWord.append((char)c);

            endPos = mSource.getEndPosition();
        }

        return mTokens.makeToken(Tokens.COMMENT, "COMMENT", mWord.toString(),
                                 startLine, startPos, endPos);
    }

    // The leading slash and star has already been scanned when this is
    // called.
    private Token scanMultiLineComment(int startPos) throws IOException {
        int c;
        int startLine = mSource.getLineNumber();
        mWord.setLength(0);
        mWord.append('/').append('*');

        while ( (c = mSource.read()) != -1 ) {
            mWord.append((char)c);

            if (c == '*') {
                if (mSource.peek() == '/') {
                    mWord.append('/');
                    mSource.read();
                    break;
                }
            }
        }

        if (c == -1) {
            error("comment.eof");
        }

        return mTokens.makeToken(Tokens.COMMENT, "COMMENT", mWord.toString(),
                                 startLine, startPos, mSource.getEndPosition());
    }

    /**
     * Simple test program
     */
    public static void main(String[] arg) throws Exception {
        // Tester.test(arg);

        Scanner s = new Scanner
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
            new Annotations()
        );

        s.emitSpecialTokens(true);

        Token token = null;
        while ( (token = s.readToken()).getID() != Tokens.EOF ) {
            System.out.print(token.getID() + ":" + token.getName() + ": ");
            System.out.print(token.getStringValue() + ": ");
            System.out.print(token.getSourceInfo() + ": ");
            System.out.println();
        }
    }
}
