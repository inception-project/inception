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
package de.tudarmstadt.ukp.inception.support.text;

import java.math.BigInteger;
import java.util.Comparator;

public class NaturalStringComparator
    implements Comparator<String>
{

    private enum BlockType
    {
        NUMERIC, ALPHABERICAL;
    }

    @Override
    public int compare(String aO1, String aO2)
    {
        // Current character positions being compared
        int blockBegin1 = 0;
        int blockBegin2 = 0;

        while (blockBegin1 < aO1.length() && blockBegin2 < aO2.length()) {
            BlockType bt1 = blockType(aO1.charAt(blockBegin1));
            BlockType bt2 = blockType(aO2.charAt(blockBegin2));

            // If the block types are different, then we can immediately end the comparison with
            // numbers coming before letters
            if (bt1 != bt2) {
                return bt1 == BlockType.NUMERIC ? -1 : 1;
            }

            int blockEnd1 = seekBlockEnd(aO1, blockBegin1);
            int blockEnd2 = seekBlockEnd(aO2, blockBegin2);

            int result;
            if (bt1 == BlockType.ALPHABERICAL) {
                result = aO1.substring(blockBegin1, blockEnd1)
                        .compareTo(aO2.substring(blockBegin1, blockEnd2));
            }
            else {
                BigInteger n1 = new BigInteger(aO1.substring(blockBegin1, blockEnd1));
                BigInteger n2 = new BigInteger(aO2.substring(blockBegin1, blockEnd2));
                result = n1.compareTo(n2);
            }

            if (result != 0) {
                return result;
            }

            blockBegin1 = blockEnd1;
            blockBegin2 = blockEnd2;
        }

        return 0;
    }

    private BlockType blockType(char aCharacter)
    {
        return Character.isDigit(aCharacter) ? BlockType.NUMERIC : BlockType.ALPHABERICAL;
    }

    private int seekBlockEnd(String aString, int aBegin)
    {
        int i = aBegin;
        final BlockType ibt = blockType(aString.charAt(i));

        while (i < aString.length()) {
            BlockType bt = blockType(aString.charAt(i));
            if (bt != ibt) {
                return i;
            }

            i++;
        }

        return i;
    }
}
