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

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

public class NaturalStringComparatorTest
{
    @Test
    public void thatNaturalStringComparatorWorks()
    {
        NaturalStringComparator cmp = new NaturalStringComparator();

        assertThat(cmp.compare("", "")).isEqualTo(0);

        assertThat(cmp.compare("1 Monkey", "1 Monkey")).isEqualTo(0);
        assertThat(cmp.compare("1 Monkey", "2 Monkeys")).isLessThan(0);
        assertThat(cmp.compare("2 Monkeys", "1 Monkey")).isGreaterThan(0);

        assertThat(cmp.compare("1 Monkey", "10 Monkeys")).isLessThan(0);
        assertThat(cmp.compare("10 Monkeys", "1 Monkey")).isGreaterThan(0);

        assertThat(cmp.compare("Number 1", "Number 1")).isEqualTo(0);
        assertThat(cmp.compare("Number 1", "Number 10")).isLessThan(0);
        assertThat(cmp.compare("Number 10", "Number 1")).isGreaterThan(0);

        List<String> strings = asList("12 Monkeys", "1 Monkey", "2 Monkeys", "21 Monkeys");
        Collections.sort(strings, cmp);
        assertThat(strings).containsExactly("1 Monkey", "2 Monkeys", "12 Monkeys", "21 Monkeys");

        List<String> strings2 = asList("Number 1", "Number 10", "Number 2", "Number 20");
        Collections.sort(strings2, cmp);
        assertThat(strings2).containsExactly("Number 1", "Number 2", "Number 10", "Number 20");
    }
}
