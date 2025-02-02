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

import static de.tudarmstadt.ukp.clarin.webanno.constraints.grammar.ConstraintsParser.parse;
import static java.util.Arrays.asList;
import static org.apache.uima.fit.util.JCasUtil.select;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.ArrayList;
import java.util.List;

import org.apache.uima.UIMAException;
import org.apache.uima.UIMAFramework;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.factory.CasFactory;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.factory.TypeSystemDescriptionFactory;
import org.apache.uima.fit.testing.factory.TokenBuilder;
import org.apache.uima.fit.util.FSUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.util.CasCreationUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import de.tudarmstadt.ukp.clarin.webanno.constraints.model.ParsedConstraints;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import de.tudarmstadt.ukp.dkpro.core.api.metadata.type.DocumentMetaData;
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Lemma;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.Dependency;
import de.tudarmstadt.ukp.inception.support.uima.AnnotationBuilder;

class ConstraintsEvaluatorTest
{
    private ConstraintsEvaluator sut;

    @BeforeEach
    void setup()
    {
        sut = new ConstraintsEvaluator();
    }

    @Nested
    class AnyRuleAffectingFeatureMatchesAllConditionsTest
    {
        TypeSystemDescription tsd;
        CAS cas;
        ParsedConstraints constraints;
        AnnotationFeature featX;
        AnnotationFeature featY;
        AnnotationFeature featZ;
        AnnotationFS context;

        @BeforeEach
        void setup() throws Exception
        {
            tsd = UIMAFramework.getResourceSpecifierFactory().createTypeSystemDescription();
            var spanType = tsd.addType("my.Span", "", CAS.TYPE_NAME_ANNOTATION);
            var spanFeatX = spanType.addFeature("X", "", CAS.TYPE_NAME_STRING);
            var spanFeatY = spanType.addFeature("Y", "", CAS.TYPE_NAME_STRING);
            var spanFeatZ = spanType.addFeature("Z", "", CAS.TYPE_NAME_STRING);

            cas = CasFactory.createCas(tsd);
            cas.setDocumentText("text");

            constraints = parse("""
                    import my.Span as Span;

                    Span {
                      X = "A" -> Y = "1";
                      Z = "B" -> Y = "2";
                      X = "C" & Z = "D" -> Y = "3";
                    }
                    """);
            featX = AnnotationFeature.builder() //
                    .withName(spanFeatX.getName()) //
                    .build();
            featY = AnnotationFeature.builder() //
                    .withName(spanFeatY.getName()) //
                    .build();
            featZ = AnnotationFeature.builder() //
                    .withName(spanFeatZ.getName()) //
                    .build();
            context = AnnotationBuilder.buildAnnotation(cas, spanType.getName()) //
                    .on("text") //
                    .buildAndAddToIndexes();
        }

        static List<Arguments> data()
        {
            return asList( //
                    arguments("A", null, null, "X", false), //
                    arguments("A", null, null, "Y", true), //
                    arguments("B", null, null, "Y", false), //
                    arguments("A", null, null, "Z", false), //
                    arguments(null, "A", null, "X", false), //
                    arguments(null, "A", null, "Y", false), //
                    arguments(null, "A", null, "Z", false), //
                    arguments(null, null, "A", "X", false), //
                    arguments(null, null, "A", "Y", false), //
                    arguments(null, null, "B", "Y", true), //
                    arguments(null, null, "A", "Z", false), //
                    arguments("A", null, "B", "Y", true), //
                    arguments("C", null, "C", "Y", false), //
                    arguments("C", null, "D", "Y", true));
        }

        @ParameterizedTest(name = "[{index}] X:[{0}] Y:[{1}] Z:[{2}] -- [{3}] -> {4}")
        @MethodSource("data")
        void test(String aX, String aY, String aZ, String aFeat, boolean aExpected) throws Exception
        {
            var feat = switch (aFeat) {
            case "X" -> featX;
            case "Y" -> featY;
            case "Z" -> featZ;
            default -> throw new IllegalArgumentException("Unknown feature");
            };

            FSUtil.setFeature(context, featX.getName(), aX);
            FSUtil.setFeature(context, featY.getName(), aY);
            FSUtil.setFeature(context, featZ.getName(), aZ);
            assertThat(sut.anyRuleAffectingFeatureMatchesAllConditions(constraints, context, feat))
                    .isEqualTo(aExpected);
        }
    }

    @Nested
    class IsHiddenConditionalFeatureTest
    {
        CAS cas;
        ParsedConstraints constraints;
        AnnotationFeature featPos;
        AnnotationFeature featCoarse;
        POS context;

        @BeforeEach
        void setup() throws Exception
        {
            cas = CasFactory.createText("text");
            constraints = parse("""
                    import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS as POS;

                    POS {
                      PosValue = "X" -> coarseValue = "1";
                    }
                    """);
            featPos = AnnotationFeature.builder() //
                    .withName(POS._FeatName_PosValue) //
                    .build();
            featCoarse = AnnotationFeature.builder() //
                    .withName(POS._FeatName_coarseValue) //
                    .withHideUnconstraintFeature(true) //
                    .build();
            context = AnnotationBuilder.buildAnnotation(cas, POS.class) //
                    .on("text") //
                    .buildAndAddToIndexes();
        }

        @Test
        void conditionMatches_restrictionMatches() throws Exception
        {
            context.setPosValue("X");
            context.setCoarseValue("1");
            assertThat(sut.isHiddenConditionalFeature(constraints, context, featCoarse)).isFalse();
        }

        @Test
        void conditionMatches_restrictionDoesNotMatch() throws Exception
        {
            context.setPosValue("X");
            context.setCoarseValue("2");
            assertThat(sut.isHiddenConditionalFeature(constraints, context, featCoarse)).isFalse();

        }

        @Test
        void conditionDoesNotMatch_restrictionMatches() throws Exception
        {
            context.setPosValue("Y");
            context.setCoarseValue("1");
            assertThat(sut.isHiddenConditionalFeature(constraints, context, featCoarse)).isTrue();
        }

        @Test
        void conditionDoesNotMatch_restrictionDoesNotMatch() throws Exception
        {
            context.setPosValue("Y");
            context.setCoarseValue("2");
            assertThat(sut.isHiddenConditionalFeature(constraints, context, featCoarse)).isTrue();
        }
    }

    @Test
    void testSimpleFeature() throws Exception
    {
        var constraints = parse("""
                import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Lemma as Lemma;

                Lemma {
                  text() = "is" -> value = "be" (!);
                }
                """);

        var jcas = JCasFactory.createText("is");

        var lemma = new Lemma(jcas, 0, 2);
        lemma.setValue("be");
        lemma.addToIndexes();

        var possibleValues = sut.generatePossibleValues(constraints, lemma, "value");

        assertThat(possibleValues).containsExactly(new PossibleValue("be", true));
    }

    @Test
    void testSimplePath() throws Exception
    {
        var constraints = parse(
                """
                        import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.Dependency as DEPENDENCY;

                        DEPENDENCY {
                          Governor.pos.PosValue = "NN" & Dependent.pos.PosValue = "DET" -> DependencyType = "det";
                        }
                        """);

        var jcas = JCasFactory.createJCas();
        jcas.setDocumentText("The sun.");

        // Add token annotations
        var t_the = new Token(jcas, 0, 3);
        t_the.addToIndexes();
        var t_sun = new Token(jcas, 0, 3);
        t_sun.addToIndexes();

        // Add POS annotations and link them to the tokens
        var p_the = new POS(jcas, t_the.getBegin(), t_the.getEnd());
        p_the.setPosValue("DET");
        p_the.addToIndexes();
        t_the.setPos(p_the);
        var p_sun = new POS(jcas, t_sun.getBegin(), t_sun.getEnd());
        p_sun.setPosValue("NN");
        p_sun.addToIndexes();
        t_sun.setPos(p_sun);

        // Add dependency annotations
        var dep_the_sun = new Dependency(jcas);
        dep_the_sun.setGovernor(t_sun);
        dep_the_sun.setDependent(t_the);
        dep_the_sun.setDependencyType("det");
        dep_the_sun.setBegin(dep_the_sun.getGovernor().getBegin());
        dep_the_sun.setEnd(dep_the_sun.getGovernor().getEnd());
        dep_the_sun.addToIndexes();

        var possibleValues = sut.generatePossibleValues(constraints, dep_the_sun, "DependencyType");

        var expectedOutput = asList(new PossibleValue("det", false));

        assertEquals(expectedOutput, possibleValues);
    }

    @Test
    void testTwoConditions() throws Exception
    {
        var constraints = parse("""
                import webanno.custom.Relation as RELATIONS;

                RELATIONS {
                    Governor.value = "Animal" & Dependent.value = "Weight" -> label = "hasWeight";
                }
                """);

        var jcas = makeJCasOneSentence();
        var cas = jcas.getCas();

        var tokens = new ArrayList<>(select(jcas, Token.class));

        var t1 = tokens.get(0);
        var t2 = tokens.get(tokens.size() - 1);

        var gov = new NamedEntity(jcas, t1.getBegin(), t1.getEnd());
        gov.setValue("Animal");
        gov.addToIndexes();
        var dep = new NamedEntity(jcas, t2.getBegin(), t2.getEnd());
        dep.setValue("NotWeight");
        dep.addToIndexes();

        var relationType = cas.getTypeSystem().getType("webanno.custom.Relation");

        var fs1 = cas.createAnnotation(relationType, dep.getBegin(), dep.getEnd());
        FSUtil.setFeature(fs1, "Governor", gov);
        FSUtil.setFeature(fs1, "Dependent", dep);
        cas.addFsToIndexes(fs1);

        var possibleValues = sut.generatePossibleValues(constraints, fs1, "label");

        // System.out.println(possibleValues);

        // "Weight" != "NotWeight", so the rule should not match
        assertThat(possibleValues).isEmpty();
    }

    private JCas makeJCasOneSentence() throws UIMAException
    {
        var global = TypeSystemDescriptionFactory.createTypeSystemDescription();
        var local = TypeSystemDescriptionFactory.createTypeSystemDescriptionFromPath(
                "src/test/resources/desc/types/webannoTestTypes.xml");

        var merged = CasCreationUtils.mergeTypeSystems(asList(global, local));

        var jcas = JCasFactory.createJCas(merged);

        DocumentMetaData.create(jcas).setDocumentId("doc");

        var tb = new TokenBuilder<>(Token.class, Sentence.class);
        tb.buildTokens(jcas, "This is a test .");

        return jcas;
    }
}
