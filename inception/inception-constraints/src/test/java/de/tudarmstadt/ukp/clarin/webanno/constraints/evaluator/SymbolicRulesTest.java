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
import static de.tudarmstadt.ukp.inception.support.uima.AnnotationBuilder.buildAnnotation;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.Files.createTempFile;
import static java.nio.file.Files.write;
import static org.apache.uima.fit.factory.CollectionReaderFactory.createReader;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

import org.apache.uima.collection.CollectionException;
import org.apache.uima.fit.testing.junit.ManagedJCas;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.dkpro.core.io.conll.Conll2006Reader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Lemma;

public class SymbolicRulesTest
{
    private static @RegisterExtension ManagedJCas jcas = new ManagedJCas();

    private ConstraintsEvaluator sut;

    @BeforeEach
    void setup()
    {
        sut = new ConstraintsEvaluator();
    }

    @Test
    public void testSimpleSymbolicRules() throws Exception
    {
        var constraints = parse(
                """
                        import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS as pos;
                        import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Lemma as lemma;
                        import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.Dependency as DEPENDENCY;


                        lemma{
                          @DEPENDENCY.@pos.@pos.PosValue="pronoun" -> value = "good" (!);
                        }
                        """);

        loadConll(jcas.get(), "src/test/resources/text/1.conll");

        var pos = new POS(jcas.get(), 8, 9);
        pos.setPosValue("pronoun");
        pos.addToIndexes();

        var lemma = jcas.get().select(Lemma.class).get();

        var possibleValues = sut.generatePossibleValues(constraints, lemma, Lemma._FeatName_value);

        assertThat(possibleValues).containsExactly(new PossibleValue("good", true));
    }

    @Test
    public void testSimpleSymbolicRules2() throws Exception
    {
        var constraints = parse(
                """
                        import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS as pos;
                        import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Lemma as lemma;
                        import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.Dependency as DEPENDENCY;


                        lemma{
                          @DEPENDENCY.@pos.@pos.text()="a" -> value = "good" (!);
                        }
                        """);

        loadConll(jcas.get(), "src/test/resources/text/1.conll");

        var pos = new POS(jcas.get(), 8, 9);
        pos.setPosValue("pronoun");
        pos.addToIndexes();

        var lemma = jcas.get().select(Lemma.class).get();

        var possibleValues = sut.generatePossibleValues(constraints, lemma, Lemma._FeatName_value);

        assertThat(possibleValues).containsExactly(new PossibleValue("good", true));
    }

    @Test
    void thatFeatureValueIsValid() throws Exception
    {
        var constraints = parse("""
                import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Lemma as Lemma;


                Lemma {
                  text() = "a" -> value = "A";
                  text() = "c" -> value = "X";
                }
                """);

        jcas.get().setDocumentText("abc");

        var lemmaValueFeature = AnnotationFeature.builder().withName(Lemma._FeatName_value).build();

        var lemmaA = buildAnnotation(jcas.get(), Lemma.class) //
                .on("a") //
                .withFeature(Lemma._FeatName_value, "A") //
                .buildAndAddToIndexes();

        var lemmaB = buildAnnotation(jcas.get(), Lemma.class) //
                .on("b") //
                .withFeature(Lemma._FeatName_value, "B") //
                .buildAndAddToIndexes();

        var lemmaC = buildAnnotation(jcas.get(), Lemma.class) //
                .on("c") //
                .withFeature(Lemma._FeatName_value, "C") //
                .buildAndAddToIndexes();

        assertThat(sut.isValidFeatureValue(constraints, lemmaA, lemmaValueFeature)).isTrue();
        assertThat(sut.isValidFeatureValue(constraints, lemmaB, lemmaValueFeature)).isFalse();
        assertThat(sut.isValidFeatureValue(constraints, lemmaC, lemmaValueFeature)).isFalse();
    }

    private void loadConll(JCas aJCas, String aLocation)
        throws ResourceInitializationException, IOException, CollectionException
    {
        var reader = createReader( //
                Conll2006Reader.class, //
                Conll2006Reader.PARAM_SOURCE_LOCATION, aLocation);

        reader.getNext(aJCas.getCas());
    }

    private static void popuplateCasFromConllString(JCas aJCas, String aConllString)
        throws IOException, ResourceInitializationException, CollectionException
    {
        var tempFile = createTempFile("test", ".conll");
        write(tempFile, aConllString.getBytes(UTF_8));
        var reader = createReader( //
                Conll2006Reader.class, //
                Conll2006Reader.PARAM_SOURCE_LOCATION, tempFile.toAbsolutePath().toString());

        reader.getNext(aJCas.getCas());
    }
}
