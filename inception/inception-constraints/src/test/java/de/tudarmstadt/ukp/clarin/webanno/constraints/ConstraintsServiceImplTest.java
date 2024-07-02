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
package de.tudarmstadt.ukp.clarin.webanno.constraints;

import static de.tudarmstadt.ukp.clarin.webanno.constraints.ConstraintsServiceImpl.mergeConstraintSets;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;

import de.tudarmstadt.ukp.clarin.webanno.constraints.model.Condition;
import de.tudarmstadt.ukp.clarin.webanno.constraints.model.ParsedConstraints;
import de.tudarmstadt.ukp.clarin.webanno.constraints.model.Restriction;
import de.tudarmstadt.ukp.clarin.webanno.constraints.model.Rule;
import de.tudarmstadt.ukp.clarin.webanno.constraints.model.Scope;

class ConstraintsServiceImplTest
{
    @Test
    void testMergeConstraintSets() throws Exception
    {
        var rule1 = new Rule(new Condition("foo", "lala"), new Restriction("fee", "lili"));
        var set1 = new ParsedConstraints(Map.of("Foo", "some.Foo"),
                asList(new Scope("Foo", asList(rule1))));

        var rule2 = new Rule(new Condition("bar", "lolo"), new Restriction("faa", "lulu"));
        var set2 = new ParsedConstraints(Map.of("Bar", "some.Bar"),
                asList(new Scope("Bar", asList(rule2))));

        var merged = mergeConstraintSets(asList(set1, set2));

        assertThat(merged.getImports()) //
                .hasSize(2) //
                .containsEntry("Foo", "some.Foo") //
                .containsEntry("Bar", "some.Bar");

        assertThat(merged.getShortName("some.Foo")).isEqualTo("Foo");
        assertThat(merged.getShortName("some.Bar")).isEqualTo("Bar");

        assertThat(merged.getScopes()) //
                .extracting(Scope::getScopeName) //
                .containsExactlyInAnyOrder("Foo", "Bar");

        assertThat(merged.getScopeByName("Foo")) //
                .extracting(Scope::getScopeName) //
                .isEqualTo("Foo");
        assertThat(merged.getScopeByName("Foo").getRules()) //
                .containsExactly(rule1);

        assertThat(merged.getScopeByName("Bar")) //
                .extracting(Scope::getScopeName) //
                .isEqualTo("Bar");
        assertThat(merged.getScopeByName("Bar").getRules()) //
                .containsExactly(rule2);
    }
}
