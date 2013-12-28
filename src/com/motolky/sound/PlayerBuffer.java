/*
Copyright (c) 2013, Alexandru Sutii
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:
    * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright
      notice, this list of conditions and the following disclaimer in the
      documentation and/or other materials provided with the distribution.
    * Neither the name of the author nor the
      names of its contributors may be used to endorse or promote products
      derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY
DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.motolky.sound;

import com.motolky.Common;

/**
 * This class implements a buffer. The data sent to an encoder is split in
 * fixed size frames. However, the size of the encoded frame varies.
 * These encoded frames are sent over bluetooth. In order to decode them,
 * we need to know where a frame starts and where it ends. For this we use
 * a separator character between the chunks. When we receive the data from
 * the bluetooth socket we receive bursts of data. This class implements a
 * buffer in which all the data from the bluetooth connection is stored in.
 * A thread from the Player continuously asks this buffer whether it has
 * an encoded frame to provide it. This buffer makes sure to remove the
 * frame separator characters from the socket stream.
 */
public class PlayerBuffer
{
    private byte[] mBuffer;
    private int mFirst = 0;
    private int mLength = 0;

    public PlayerBuffer(int maxBufLen) {
        mBuffer = new byte[maxBufLen];
    }

    /**
     * Returns an encode frame if one exists.
     * @return an encoded frame
     */
    public byte[] getFrame() {
        // A frame is not available
        if (mLength <= 2)
            return null;

        /**
         * Look for the first separator in the received data. There a frame ends.
         */
        int i = 0;
        while (!((mBuffer[(i + mFirst) % mBuffer.length] == Common.SEPARATOR) &&
                (mBuffer[(1 + i + mFirst) % mBuffer.length] == Common.SEPARATOR))) {
            i++;
            if (i >= mLength - 1) {
                mLength = 0;
                return null;
            }
        }

        byte[] data = null;

        if (i > 0) {
            // We found a separator, so we return the frame by copying it
            // in the provided array
            data = new byte[i];
            if (i + mFirst <= mBuffer.length)
                System.arraycopy(mBuffer, mFirst, data, 0, data.length);
            else {
                System.arraycopy(mBuffer, mFirst, data, 0, mBuffer.length - mFirst);
                System.arraycopy(mBuffer, 0, data, mBuffer.length - mFirst, i - (mBuffer.length - mFirst));
            }
        }

        mFirst = (2 + (i + mFirst)) % mBuffer.length;
        mLength -= i + 2;

        return data;
    }

    /**
     * Inserts data in the buffer
     * @param data
     * @param noBytes
     */
    public void insertData(byte[] data, int noBytes) {
        if (noBytes + mLength > mBuffer.length)
            return;

        for (int i = 0; i < noBytes; i++)
            mBuffer[(i + mFirst + mLength) % mBuffer.length] = data[i];

        mLength = noBytes + mLength;
    }
}