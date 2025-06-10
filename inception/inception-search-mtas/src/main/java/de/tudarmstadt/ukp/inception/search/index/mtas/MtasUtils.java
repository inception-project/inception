/*
 * Licensed to the Technische Universität Darmstadt under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The Technische Universität Darmstadt 
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.
 *  
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.inception.search.index.mtas;

import java.nio.Buffer;
import java.nio.ByteBuffer;

import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.util.BytesRef;

import mtas.analysis.token.MtasTokenCollection;
import mtas.analysis.util.MtasParserException;

public final class MtasUtils
{
    private MtasUtils()
    {
        // No instances
    }

    public static void print(MtasTokenCollection aTC)
    {
        try {
            String[][] dump = aTC.getList();

            int[] widths = new int[dump[0].length];
            for (int n = 0; n < dump.length; n++) {
                for (int i = 0; i < widths.length; i++) {
                    widths[i] = Math.max(widths[i], String.valueOf(dump[n][i]).length());
                }
            }

            for (int n = 0; n < dump.length; n++) {
                for (int i = 0; i < dump[n].length; i++) {
                    System.out.print(StringUtils.rightPad(String.valueOf(dump[n][i]), widths[i]));
                    System.out.print(" | ");
                }
                System.out.println();
            }
        }
        catch (MtasParserException e) {
            e.printStackTrace();
        }
    }

    public static BytesRef encodeFSAddress(int aFeatureStructureAddress)
    {
        return new BytesRef(ByteBuffer.allocate(4).putInt(aFeatureStructureAddress).array());
    }

    public static int decodeFSAddress(BytesRef aBytesRef)
    {
        var buffer = ByteBuffer.allocate(4).put(aBytesRef.bytes, aBytesRef.offset,
                aBytesRef.length);
        // Cast to buffer to permit code to run on Java 8.
        // See: https://github.com/inception-project/inception/issues/1828#issuecomment-717047584
        ((Buffer) buffer).flip();
        return buffer.getInt();
    }

    public static char[] bytesToChars(byte[] aBytes)
    {
        // Reserve sufficient space of the length and the char-encoded byte array
        char[] chars = new char[2 + (aBytes.length / 2) + (aBytes.length % 2)];

        // Encode the length of the byte array
        chars[0] = (char) ((aBytes.length & 0xFFFF0000) >> 16);
        chars[1] = (char) (aBytes.length & 0x0000FFFF);

        // Encode the byte array into the char array
        for (int i = 0; i < aBytes.length; i++) {
            if (i % 2 == 0) {
                chars[2 + (i / 2)] |= (char) (aBytes[i] << 8);
            }
            else {
                chars[2 + (i / 2)] |= (char) (aBytes[i] & 0x00FF);
            }
        }

        return chars;
    }

    public static byte[] charsToBytes(char[] aChars)
    {
        int len = ((int) aChars[0] << 16) | aChars[1];
        byte[] bytes = new byte[len];

        for (int i = 0; i < len; i++) {
            if (i % 2 == 0) {
                bytes[i] = (byte) (aChars[2 + (i / 2)] >>> 8);
            }
            else {
                bytes[i] = (byte) (aChars[2 + (i / 2)] & 0x00FF);
            }
        }

        return bytes;
    }
}
