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

import java.io.PrintStream;

/******************************************************************************
 * A Token represents the smallest whole element of a source file. Tokens are
 * produced by a {@link Scanner}.
 *
 * @author Brian S O'Neill
 * @version
 * <!--$$Revision:--> 46 <!-- $-->, <!--$$JustDate:-->  5/31/01 <!-- $-->
 */
public class Token implements java.io.Serializable {

    /** Serialized version. */
    private final static long serialVersionUID = 1L;

    private int mTokenID;
    private String mName;
    private SourceInfo mInfo;

    Token(int sourceLine,
          int sourceStartPos,
          int sourceEndPos,
          int tokenID,
          String name) {

        this(sourceLine,
             sourceStartPos,
             sourceEndPos,
             sourceStartPos,
             tokenID,
             name);
    }

    Token(int sourceLine,
          int sourceStartPos,
          int sourceEndPos,
          int sourceDetailPos,
          int tokenID,
          String name) {

        mName = name;
        mTokenID = tokenID;

        if (sourceStartPos == sourceDetailPos) {
            mInfo = new SourceInfo(sourceLine,
                                   sourceStartPos, sourceEndPos);
        }
        else {
            mInfo = new SourceDetailedInfo(sourceLine,
                                           sourceStartPos, sourceEndPos,
                                           sourceDetailPos);
        }

        if (sourceStartPos > sourceEndPos) {
            // This is an internal error.
            throw new IllegalArgumentException
                ("Token start position greater than end position at line: " +
                 sourceLine);
        }
    }

    public Token(SourceInfo info, int tokenID, String name) {
        mTokenID = tokenID;
        mInfo = info;
        mName = name;
    }

    /**
     * Dumps the contents of this Token to System.out.
     */
    public final void dump() {
        dump(System.out);
    }

    /**
     * Dumps the contents of this Token.
     * @param out The PrintStream to write to.
     */
    public final void dump(PrintStream out) {
        out.println("Token [Image: " +
                    getName() + "] [Value: " + getStringValue() +
                    "] [Id: " + getID() + "] [start: " +
                    mInfo.getStartPosition() + "] [end " +
                    mInfo.getEndPosition() + "]");
    }

    /**
     * Returns the ID of this Token, which identifies what type of token it is.
     */
    public final int getID() {
        return mTokenID;
    }

    /**
     * Returns the image or name of this token.
     */
    public final String getName() {
        return mName;
    }

    /**
     * Returns information regarding where in the source file this token
     * came from.
     */
    public final SourceInfo getSourceInfo() {
        return mInfo;
    }

    public String getStringValue() {
        return null;
    }

    /**
     * Only valid if token is a number. Returns 0 if token is not a number
     * or is an invalid number. Returns 1 for int, 2 for long, 3 for
     * float and 4 for double. The token ID for all numbers (even invalid ones)
     * is NUMBER.
     *
     * @return 0, 1, 2, 3 or 4.
     */
    public int getNumericType() {
        return 0;
    }

    /** Only valid if token is a number. */
    public int getIntValue() {
        return 0;
    }

    /** Only valid if token is a number. */
    public long getLongValue() {
        return 0L;
    }

    /** Only valid if token is a number. */
    public float getFloatValue() {
        return 0.0f;
    }

    /** Only valid if token is a number. */
    public double getDoubleValue() {
        return 0.0d;
    }

    public String toString() {
        StringBuilder buf = new StringBuilder(10);

        String image = this.getName();

        if (image != null) {
            buf.append(image);
        }

        String str = getStringValue();

        if (str != null) {
            if (image != null) {
                buf.append(' ');
            }
            buf.append(str);
        }

        return buf.toString();
    }
}
