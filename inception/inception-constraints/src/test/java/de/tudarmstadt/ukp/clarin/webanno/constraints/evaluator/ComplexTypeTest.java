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
package de.tudarmstadt.ukp.clarin.webanno.constraints.evaluator;

import static de.tudarmstadt.ukp.clarin.webanno.constraints.grammar.ConstraintsParser.parseFile;
import static de.tudarmstadt.ukp.inception.support.uima.AnnotationBuilder.buildAnnotation;
import static de.tudarmstadt.ukp.inception.support.uima.FeatureStructureBuilder.buildFS;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

import org.apache.uima.cas.CAS;
import org.apache.uima.fit.factory.TypeSystemDescriptionFactory;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.CasCreationUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.tudarmstadt.ukp.clarin.webanno.constraints.model.Condition;
import de.tudarmstadt.ukp.clarin.webanno.constraints.model.Restriction;
import de.tudarmstadt.ukp.clarin.webanno.constraints.model.Rule;

public class ComplexTypeTest
{
    private CAS cas;
    private ConstraintsEvaluator sut;

    @BeforeEach
    public void setup() throws ResourceInitializationException
    {
        var tsd = TypeSystemDescriptionFactory
                .createTypeSystemDescription("desc.types.TestTypeSystemDescriptor");

        cas = CasCreationUtils.createCas(tsd, null, null);
        sut = new ConstraintsEvaluator();
    }

    @Test
    public void thatSlotFeatureInConditionWorks() throws Exception
    {
        cas.setDocumentText("ACME acquired Foobar.");

        // @formatter:off
        var host = buildAnnotation(cas, "webanno.custom.ComplexLinkHost")
            .on("acquired")
            .withFeature("links", asList(
                    buildFS(cas, "webanno.custom.ComplexLinkType")
                        .withFeature("target", buildAnnotation(cas, "webanno.custom.Span")
                                .on("ACME") //
                                .withFeature("value", "PER") //
                                .buildAndAddToIndexes())
                        .buildWithoutAddingToIndexes(),
                    buildFS(cas, "webanno.custom.ComplexLinkType")
                        .withFeature("target", buildAnnotation(cas, "webanno.custom.Span")
                                .on("Foobar") //
                                .withFeature("value", "LOC") //
                                .buildAndAddToIndexes())
                        .buildWithoutAddingToIndexes()))
            .buildAndAddToIndexes();
        // @formatter:on

        var constraints = parseFile("src/test/resources/rules/slot_feature_in_condition.rules");

        var possibleValues = sut.generatePossibleValues(constraints, host, "value");

        assertThat(possibleValues).containsExactly(new PossibleValue("move", false));
    }

    @Test
    public void thatFeaturePathInConditionWorks() throws Exception
    {
        cas.setDocumentText("I listen to lectures by Prof. Gurevych sometimes.");

        // @formatter:off
        var proemel = buildAnnotation(cas, "de.tud.Prof")
                .withFeature("fullName", "Hans Juergen Proeml")
                .buildAndAddToIndexes();

        var gurevych = buildAnnotation(cas, "de.tud.Prof")
                .withFeature("fullName", "Iryna Gurevych")
                .withFeature("boss", proemel)
                .buildAndAddToIndexes();
        // @formatter:on

        var constraints = parseFile("src/test/resources/rules/feature_path_in_condition.rules");

        // @formatter:off
        assertThat(constraints.getScopeByName("Prof").getRules())
                .containsExactly(new Rule(
                        new Condition("boss.fullName", "Hans Juergen Proeml"),
                        new Restriction("professorName", "Iryna Gurevych")));
        // @formatter:on

        var possibleValues = sut.generatePossibleValues(constraints, gurevych, "professorName");

        assertThat(possibleValues).containsExactly(new PossibleValue("Iryna Gurevych", false));
    }

    @Test
    public void thatConjunctionInConditionWorks() throws Exception
    {
        cas.setDocumentText("Asia is the largest continent on Earth. Asia is subdivided into 48 "
                + "countries, two of them (Russia and Turkey) having part of their land in "
                + "Europe. The most active place on Earth for tropical cyclone activity lies "
                + "northeast of the Philippines and south of Japan. The Gobi Desert is in "
                + "Mongolia and the Arabian Desert stretches across much of the Middle East. "
                + "The Yangtze River in China is the longest river in the continent. The "
                + "Himalayas between Nepal and China is the tallest mountain range in the "
                + "world. Tropical rainforests stretch across much of southern Asia and "
                + "coniferous and deciduous forests lie farther north.");

        // @formatter:off
        var asiaContinent = buildAnnotation(cas, "de.Continent")
                .at(0, 4)
                .withFeature("name", "Asia")
                .buildAndAddToIndexes();

        var russia = buildAnnotation(cas, "de.Country")
                .at(56, 62)
                .withFeature("name", "Russian Federation")
                .withFeature("continent", asiaContinent)
                .buildAndAddToIndexes();
        // @formatter:on

        var constraints = parseFile("src/test/resources/rules/region.rules");

        var possibleValues = sut.generatePossibleValues(constraints, russia, "regionType");

        assertThat(possibleValues).containsExactly(new PossibleValue("cold", true));
    }

    @Test
    public void thatBooleanValueInConditionWorks() throws Exception
    {
        cas.setDocumentText("blah");

        // @formatter:off
        var continent = buildAnnotation(cas, "de.Continent")
                .at(0, 1)
                .withFeature("discovered", true)
                .buildAndAddToIndexes();
        // @formatter:on

        var constraints = parseFile("src/test/resources/rules/region.rules");

        var possibleValues = sut.generatePossibleValues(constraints, continent, "name");

        // @formatter:off
        assertThat(possibleValues)
                .extracting(PossibleValue::getValue)
                .containsExactlyInAnyOrder("America");
        // @formatter:on
    }

    @Test
    public void thatIntegerValueInConditionWorks() throws Exception
    {
        cas.setDocumentText("blah");

        // @formatter:off
        var continent = buildAnnotation(cas, "de.Continent")
                .at(0, 1)
                .withFeature("area", 100)
                .buildAndAddToIndexes();
        // @formatter:on

        var constraints = parseFile("src/test/resources/rules/region.rules");

        var possibleValues = sut.generatePossibleValues(constraints, continent, "name");

        // @formatter:off
        assertThat(possibleValues)
                .extracting(PossibleValue::getValue)
                .containsExactlyInAnyOrder("Fantasy Island");
        // @formatter:on
    }
}
