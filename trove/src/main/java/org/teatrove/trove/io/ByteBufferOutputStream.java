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

package org.teatrove.trove.io;

import java.io.OutputStream;
import java.io.IOException;

/**
 * An OutputStream that writes into a ByteBuffer.
 *
 * @author Brian S O'Neill
 */
public class ByteBufferOutputStream extends OutputStream {
    private ByteBuffer mBuffer;
    private boolean mClosed;

    public ByteBufferOutputStream(ByteBuffer buffer) {
        mBuffer = buffer;
    }

    public void write(int b) throws IOException {
        checkIfClosed();
        mBuffer.append((byte)b);
    }

    public void write(byte[] bytes) throws IOException {
        checkIfClosed();
        mBuffer.append(bytes);
    }

    public void write(byte[] bytes, int offset, int length)
        throws IOException {
        checkIfClosed();
        mBuffer.append(bytes, offset, length);
    }

    public void flush() throws IOException {
        checkIfClosed();
    }

    public void close() {
        mClosed = true;
    }

    private void checkIfClosed() throws IOException {
        if (mClosed) {
            throw new IOException("OutputStream closed");
        }
    }
}
