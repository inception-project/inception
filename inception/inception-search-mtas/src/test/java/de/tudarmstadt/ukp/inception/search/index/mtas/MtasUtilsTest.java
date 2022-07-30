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

import static de.tudarmstadt.ukp.inception.search.index.mtas.MtasUtils.bytesToChars;
import static de.tudarmstadt.ukp.inception.search.index.mtas.MtasUtils.charsToBytes;
import static org.assertj.core.api.Assertions.assertThat;

import java.security.NoSuchAlgorithmException;
import java.util.Random;

import org.junit.jupiter.api.Test;

public class MtasUtilsTest
{
    Random rnd = new Random();

    @Test
    public void bytesToCharsRoundtripFixed() throws NoSuchAlgorithmException
    {
        byte[] input = new byte[] { 0x01, 0x02, (byte) 0xFE, (byte) 0xFF };

        byte[] output = charsToBytes(bytesToChars(input));

        assertThat(output).isEqualTo(input);
    }

    @Test
    public void bytesToCharsRoundtripRandom() throws NoSuchAlgorithmException
    {
        byte[] input = new byte[65535];

        rnd.nextBytes(input);

        byte[] output = charsToBytes(bytesToChars(input));

        assertThat(output).isEqualTo(input);
    }
}
