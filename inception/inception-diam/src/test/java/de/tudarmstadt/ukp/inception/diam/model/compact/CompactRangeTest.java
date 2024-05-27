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
package de.tudarmstadt.ukp.inception.diam.model.compact;

import static de.tudarmstadt.ukp.inception.support.json.JSONUtil.fromJsonString;
import static de.tudarmstadt.ukp.inception.support.json.JSONUtil.toJsonString;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.exc.MismatchedInputException;

class CompactRangeTest
{
    @Test
    void thatSerializationWorks() throws Exception
    {
        assertThat(toJsonString(new CompactRange(10, 20))).isEqualTo("[10,20]");
    }

    @Test
    void thatDeserializationWorks() throws Exception
    {
        assertThat(fromJsonString(CompactRange.class, "[10,20]"))
                .isEqualTo(new CompactRange(10, 20));
    }

    @Test
    void thatDeserializationFails() throws Exception
    {
        assertThatExceptionOfType(MismatchedInputException.class)
                .isThrownBy(() -> fromJsonString(CompactRange.class, "[false,20]"))
                .withMessageContaining("Expecting begin offset as integer");
    }
}
