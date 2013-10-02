/*
 *  Copyright 1997-2011 teatrove.org
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.teatrove.tea.log;

/**
 * The TeaStackTraceLine class contains the values of a stack trace line.
 * It contains the template name and line number.  It also includes the
 * original stack trace line from the JVM.
 *
 * @author Reece Wilton
 */
public class TeaStackTraceLine {

    private String mTemplateName;
    private Integer mLineNumber;
    private String mLine;

    public TeaStackTraceLine(String templateName,
                             Integer lineNumber,
                             String line) {
        mTemplateName = templateName;
        mLineNumber = lineNumber;
        mLine = line;
    }

    /**
     * @return  the original stack trace line
     */
    public String getLine() {
        return mLine;
    }

    /**
     * @return  the Tea template line number.  May be null even if a
     * template name exists.
     */
    public Integer getLineNumber() {
        return mLineNumber;
    }

    /**
     * @return  the template name.  Will be null if the line isn't for a
     * Tea template.
     */
    public String getTemplateName() {
        return mTemplateName;
    }

    public String toString() {
        if (mLineNumber != null) {
            return "\tat " +
                   ((mLineNumber != null) ? "line " + mLineNumber + " of "
                                            : "unknown line of ")
                   + "template " + mTemplateName;
        }
        else {
            return mLine;
        }
    }
}