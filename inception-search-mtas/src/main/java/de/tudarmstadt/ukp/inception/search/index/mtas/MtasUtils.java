/*
 * Copyright 2018
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.inception.search.index.mtas;

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

    public static void print(MtasTokenCollection aTC) throws MtasParserException
    {
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
    
    public static BytesRef encodeFSAddress(int aFeatureStructureAddress)
    {
        return new BytesRef(ByteBuffer.allocate(4).putInt(aFeatureStructureAddress).array());
    }

    public static int decodeFSAddress(BytesRef aBytesRef)
    {
        ByteBuffer buffer = ByteBuffer.allocate(4).put(aBytesRef.bytes, aBytesRef.offset,
                aBytesRef.length);
        buffer.flip();
        return buffer.getInt();
    }
}
