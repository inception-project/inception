/*
 * Copyright 2015
 * Ubiquitous Knowledge Processing (UKP) Lab and FG Language Technology
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
package de.tudarmstadt.ukp.clarin.webanno.constraints;

import static org.apache.uima.fit.util.CasUtil.getType;
import static org.apache.uima.util.CasCreationUtils.createCas;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

import java.io.FileInputStream;
import java.util.LinkedList;
import java.util.List;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.factory.TypeSystemDescriptionFactory;
import org.apache.uima.fit.util.FSUtil;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.util.CasCreationUtils;
import org.junit.Test;

import de.tudarmstadt.ukp.clarin.webanno.constraints.evaluator.Evaluator;
import de.tudarmstadt.ukp.clarin.webanno.constraints.evaluator.PossibleValue;
import de.tudarmstadt.ukp.clarin.webanno.constraints.evaluator.ValuesGenerator;
import de.tudarmstadt.ukp.clarin.webanno.constraints.grammar.ConstraintsGrammar;
import de.tudarmstadt.ukp.clarin.webanno.constraints.grammar.syntaxtree.Parse;
import de.tudarmstadt.ukp.clarin.webanno.constraints.model.ParsedConstraints;
import de.tudarmstadt.ukp.clarin.webanno.constraints.visitor.ParserVisitor;

public class ComplexTypeTest
{
    @Test
    public void testProfType()
        throws Exception
    {
        TypeSystemDescription tsd = TypeSystemDescriptionFactory
                .createTypeSystemDescription("desc.types.TestTypeSystemDescriptor");

        CAS cas = CasCreationUtils.createCas(tsd, null, null);
        cas.setDocumentText("I listen to lectures by Prof. Gurevych sometimes.");

        TypeSystem ts = cas.getTypeSystem();
        Type profType = ts.getType("de.tud.Prof");
        Feature profNameFeature = profType.getFeatureByBaseName("fullName");
        Feature profBossFeature = profType.getFeatureByBaseName("boss");

        AnnotationFS proemel = cas.createAnnotation(profType, 0, 0);
        proemel.setStringValue(profNameFeature, "Hans Juergen Proeml");
        cas.addFsToIndexes(proemel);

        AnnotationFS gurevych = cas.createAnnotation(profType, 24, 38);
        gurevych.setStringValue(profNameFeature, "Iryna Gurevych");
        gurevych.setFeatureValue(profBossFeature, proemel);
        cas.addFsToIndexes(gurevych);

        /*
         * for (String feature : Arrays.asList("fullName", "boss")) { Feature someFeature =
         * gurevych.getType().getFeatureByBaseName(feature); if
         * (someFeature.getRange().isPrimitive()) { String value =
         * gurevych.getFeatureValueAsString(someFeature); System.out.println(value); } else {
         * FeatureStructure value = gurevych.getFeatureValue(someFeature);
         * System.out.printf("%s (%s)%n", value.getFeatureValueAsString(profNameFeature),
         * value.getType()); } }
         */

        ConstraintsGrammar parser = new ConstraintsGrammar(new FileInputStream(
                "src/test/resources/rules/prof.rules"));
        Parse p = parser.Parse();

        ParsedConstraints constraints = p.accept(new ParserVisitor());

        Evaluator constraintsEvaluator = new ValuesGenerator();

        List<PossibleValue> possibleValues = constraintsEvaluator.generatePossibleValues(gurevych,
                "professorName", constraints);

        List<PossibleValue> exValues = new LinkedList<>();
        exValues.add(new PossibleValue("Iryna Gurevych", false));

        assertEquals(possibleValues, exValues);

    }

    @Test
    public void testCountryType()
        throws Exception
    {

        TypeSystemDescription tsd = TypeSystemDescriptionFactory
                .createTypeSystemDescription("desc.types.TestTypeSystemDescriptor");

        CAS cas = CasCreationUtils.createCas(tsd, null, null);
        cas.setDocumentText("Asia is the largest continent on Earth. Asia is subdivided into 48 "
                + "countries, two of them (Russia and Turkey) having part of their land in "
                + "Europe. The most active place on Earth for tropical cyclone activity lies "
                + "northeast of the Philippines and south of Japan. The Gobi Desert is in "
                + "Mongolia and the Arabian Desert stretches across much of the Middle East. "
                + "The Yangtze River in China is the longest river in the continent. The "
                + "Himalayas between Nepal and China is the tallest mountain range in the "
                + "world. Tropical rainforests stretch across much of southern Asia and "
                + "coniferous and deciduous forests lie farther north.");
        TypeSystem ts = cas.getTypeSystem();
        Type continentType = ts.getType("de.Continent");
        Feature continentName = continentType.getFeatureByBaseName("name");
        AnnotationFS asiaContinent = cas.createAnnotation(continentType, 0, 4);
        asiaContinent.setStringValue(continentName, "Asia");
        cas.addFsToIndexes(asiaContinent);

        Type countryType = ts.getType("de.Country");
        Feature countryName = countryType.getFeatureByBaseName("name");
        AnnotationFS russia = cas.createAnnotation(countryType, 56, 62);
        russia.setStringValue(countryName, "Russian Federation");
        Feature continentFeature = countryType.getFeatureByBaseName("continent");
        russia.setFeatureValue(continentFeature, asiaContinent);
        cas.addFsToIndexes(russia);

        ConstraintsGrammar parser = new ConstraintsGrammar(new FileInputStream(
                "src/test/resources/rules/region.rules"));
        ParsedConstraints constraints = parser.Parse().accept(new ParserVisitor());

        Evaluator constraintsEvaluator = new ValuesGenerator();

        List<PossibleValue> possibleValues = constraintsEvaluator.generatePossibleValues(russia,
                "regionType", constraints);

        List<PossibleValue> exValues = new LinkedList<>();
        exValues.add(new PossibleValue("cold", true));

        assertEquals(possibleValues, exValues);
    }
    
    @Test
    public void thatBooleanValueInConditionWorks() throws Exception
    {
        TypeSystemDescription tsd = TypeSystemDescriptionFactory
                .createTypeSystemDescription("desc.types.TestTypeSystemDescriptor");

        CAS cas = createCas(tsd, null, null);
        cas.setDocumentText("blah");
        
        AnnotationFS continent = cas.createAnnotation(getType(cas, "de.Continent"), 0, 1);
        FSUtil.setFeature(continent, "discovered", true);
        cas.addFsToIndexes(continent);

        ConstraintsGrammar parser = new ConstraintsGrammar(new FileInputStream(
                "src/test/resources/rules/region.rules"));
        ParsedConstraints constraints = parser.Parse().accept(new ParserVisitor());

        Evaluator constraintsEvaluator = new ValuesGenerator();
        List<PossibleValue> possibleValues = constraintsEvaluator.generatePossibleValues(continent,
                "name", constraints);
        
        assertThat(possibleValues)
                .extracting(PossibleValue::getValue)
                .containsExactlyInAnyOrder("America");
    }
    
    @Test
    public void thatIntegerValueInConditionWorks() throws Exception
    {
        TypeSystemDescription tsd = TypeSystemDescriptionFactory
                .createTypeSystemDescription("desc.types.TestTypeSystemDescriptor");

        CAS cas = createCas(tsd, null, null);
        cas.setDocumentText("blah");
        
        AnnotationFS continent = cas.createAnnotation(getType(cas, "de.Continent"), 0, 1);
        FSUtil.setFeature(continent, "area", 100);
        cas.addFsToIndexes(continent);

        ConstraintsGrammar parser = new ConstraintsGrammar(new FileInputStream(
                "src/test/resources/rules/region.rules"));
        ParsedConstraints constraints = parser.Parse().accept(new ParserVisitor());

        Evaluator constraintsEvaluator = new ValuesGenerator();
        List<PossibleValue> possibleValues = constraintsEvaluator.generatePossibleValues(continent,
                "name", constraints);
        
        assertThat(possibleValues)
                .extracting(PossibleValue::getValue)
                .containsExactlyInAnyOrder("Fantasy Island");
    }
}
