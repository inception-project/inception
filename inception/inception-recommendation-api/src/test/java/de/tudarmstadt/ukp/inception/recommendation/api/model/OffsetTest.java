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
package de.tudarmstadt.ukp.inception.recommendation.api.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;

public class OffsetTest
{
    @Test
    public void thatCompareToWorks()
    {
        Offset o1 = new Offset(480, 486);
        Offset o1a = new Offset(480, 486);
        Offset o2 = new Offset(480, 487);
        Offset o3 = new Offset(479, 487);

        assertThat(o1).isEqualByComparingTo(o1);
        assertThat(o1).isEqualByComparingTo(o1a);
        assertThat(o1a).isEqualByComparingTo(o1);
        assertThat(o1).isNotEqualByComparingTo(o2);
        assertThat(o2).isNotEqualByComparingTo(o1);

        Set<Offset> treeset = new TreeSet<>();
        treeset.add(o1);
        assertThat(treeset).doesNotContain(o2);
        assertThat(treeset).contains(o1a);

        assertThat(o2).isLessThan(o1);
        assertThat(o1).isGreaterThan(o2);
        assertThat(o3).isLessThan(o2);
        assertThat(o2).isGreaterThan(o3);
    }
}
