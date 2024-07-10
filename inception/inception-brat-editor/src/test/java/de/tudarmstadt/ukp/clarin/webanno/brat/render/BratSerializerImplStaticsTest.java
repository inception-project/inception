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
package de.tudarmstadt.ukp.clarin.webanno.brat.render;

import static de.tudarmstadt.ukp.clarin.webanno.brat.render.BratSerializerImpl.split;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import de.tudarmstadt.ukp.clarin.webanno.brat.render.model.Offsets;

class BratSerializerImplStaticsTest
{
    @Test
    void thatSplitWorks() throws Exception
    {
        var rows = asList(new Offsets(0, 2), new Offsets(2, 4));
        var doc = "abcd";

        assertThat(split(rows, doc, 0, 0, 0)) //
                .containsExactly(new Offsets(0, 0));
        assertThat(split(rows, doc, 0, 0, 1)) //
                .containsExactly(new Offsets(0, 1));
        assertThat(split(rows, doc, 0, 0, 2)) //
                .containsExactly(new Offsets(0, 2));
        assertThat(split(rows, doc, 0, 0, 3)) //
                .containsExactly(new Offsets(0, 2), new Offsets(2, 3));
        assertThat(split(rows, doc, 0, 0, 4)) //
                .containsExactly(new Offsets(0, 2), new Offsets(2, 4));
        assertThat(split(rows, doc, 0, 1, 4)) //
                .containsExactly(new Offsets(1, 2), new Offsets(2, 4));
        assertThat(split(rows, doc, 0, 2, 4)) //
                .containsExactly(new Offsets(2, 4));
        assertThat(split(rows, doc, 0, 3, 4)) //
                .containsExactly(new Offsets(3, 4));
        assertThat(split(rows, doc, 0, 4, 4)) //
                .containsExactly(new Offsets(4, 4));
    }
}
