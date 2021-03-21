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
package de.tudarmstadt.ukp.clarin.webanno.tsv.internal.tsv3x;

import static java.util.Arrays.asList;
import static org.apache.commons.lang3.StringUtils.join;
import static org.apache.uima.fit.factory.JCasFactory.createJCas;
import static org.apache.uima.fit.factory.TypeSystemDescriptionFactory.createTypeSystemDescription;
import static org.apache.uima.fit.factory.TypeSystemDescriptionFactory.createTypeSystemDescriptionFromPath;
import static org.apache.uima.fit.util.JCasUtil.select;
import static org.apache.uima.util.CasCreationUtils.mergeTypeSystems;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.apache.uima.UIMAException;
import org.apache.uima.cas.Type;
import org.apache.uima.fit.testing.factory.TokenBuilder;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.junit.jupiter.api.Test;

import de.tudarmstadt.ukp.clarin.webanno.tsv.internal.tsv3x.model.FeatureType;
import de.tudarmstadt.ukp.clarin.webanno.tsv.internal.tsv3x.model.LayerType;
import de.tudarmstadt.ukp.clarin.webanno.tsv.internal.tsv3x.model.TsvColumn;
import de.tudarmstadt.ukp.clarin.webanno.tsv.internal.tsv3x.model.TsvDocument;
import de.tudarmstadt.ukp.clarin.webanno.tsv.internal.tsv3x.model.TsvSchema;
import de.tudarmstadt.ukp.dkpro.core.api.metadata.type.DocumentMetaData;
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.Dependency;

public class Tsv3XSerializerTest
{
    @Test
    public void testStackedSingleTokenWithValue() throws Exception
    {
        // Create test document
        JCas cas = makeJCasOneSentence("This is a test .");
        NamedEntity ne1 = addNamedEntity(cas, 0, 4, "PER");
        NamedEntity ne2 = addNamedEntity(cas, 0, 4, "ORG");

        // Set up TSV schema
        TsvSchema schema = new TsvSchema();
        Type namedEntityType = cas.getCasType(NamedEntity.type);
        schema.addColumn(
                new TsvColumn(namedEntityType, LayerType.SPAN, "value", FeatureType.PRIMITIVE));

        // Convert test document content to TSV model
        TsvDocument doc = Tsv3XCasDocumentBuilder.of(schema, cas);
        doc.getSentences().get(0).getTokens().get(0).addUimaAnnotation(ne1, true);
        doc.getSentences().get(0).getTokens().get(0).addUimaAnnotation(ne2, true);

        assertEquals("1-1\t0-4\tThis\tPER[1]|ORG[2]\t",
                doc.getSentences().get(0).getTokens().get(0).toString());

        // @formatter:off
        String expectedSentence = 
                "#Text=This is a test .\n" + 
                "1-1\t0-4\tThis\tPER[1]|ORG[2]\t\n" + 
                "1-2\t5-7\tis\t_\t\n" + 
                "1-3\t8-9\ta\t_\t\n" + 
                "1-4\t10-14\ttest\t_\t\n" + 
                "1-5\t15-16\t.\t_\t\n";
        // @formatter:on
        assertEquals(expectedSentence, doc.getSentences().get(0).toString());
    }

    @Test
    public void testSingleSubTokenWithValue() throws Exception
    {
        // Create test document
        JCas cas = makeJCasOneSentence("This is a test .");
        addNamedEntity(cas, 1, 3, "PER");

        // Set up TSV schema
        TsvSchema schema = new TsvSchema();
        Type namedEntityType = cas.getCasType(NamedEntity.type);
        schema.addColumn(
                new TsvColumn(namedEntityType, LayerType.SPAN, "value", FeatureType.PRIMITIVE));

        // Convert test document content to TSV model
        TsvDocument doc = Tsv3XCasDocumentBuilder.of(schema, cas);

        // @formatter:off
        String expectedSentence = 
                "#Text=This is a test .\n" + 
                "1-1\t0-4\tThis\t_\t\n" + 
                "1-1.1\t1-3\thi\tPER\t\n" + 
                "1-2\t5-7\tis\t_\t\n" + 
                "1-3\t8-9\ta\t_\t\n" + 
                "1-4\t10-14\ttest\t_\t\n" + 
                "1-5\t15-16\t.\t_\t\n";
        // @formatter:on
        assertEquals(expectedSentence, doc.getSentences().get(0).toString());
    }

    @Test
    public void testSingleZeroWidthTokenWithoutValue() throws Exception
    {
        // Create test document
        JCas cas = makeJCasOneSentence("This is a test .");
        addNamedEntity(cas, 0, 0, null);

        // Set up TSV schema
        TsvSchema schema = new TsvSchema();
        Type namedEntityType = cas.getCasType(NamedEntity.type);
        schema.addColumn(
                new TsvColumn(namedEntityType, LayerType.SPAN, "value", FeatureType.PRIMITIVE));

        // Convert test document content to TSV model
        TsvDocument doc = Tsv3XCasDocumentBuilder.of(schema, cas);

        // @formatter:off
        String expectedSentence = 
                "#Text=This is a test .\n" + 
                "1-1\t0-4\tThis\t_\t\n" + 
                "1-1.1\t0-0\t\t*\t\n" + 
                "1-2\t5-7\tis\t_\t\n" + 
                "1-3\t8-9\ta\t_\t\n" + 
                "1-4\t10-14\ttest\t_\t\n" + 
                "1-5\t15-16\t.\t_\t\n";
        // @formatter:on
        assertEquals(expectedSentence, doc.getSentences().get(0).toString());
    }

    @Test
    public void testRelation() throws Exception
    {
        // Create test document
        JCas cas = makeJCasOneSentence("This is a test .");
        List<Token> tokens = new ArrayList<>(select(cas, Token.class));
        Dependency dep = new Dependency(cas);
        dep.setGovernor(tokens.get(0));
        dep.setDependent(tokens.get(1));
        dep.setDependencyType("dep");
        dep.setBegin(dep.getDependent().getBegin());
        dep.setEnd(dep.getDependent().getEnd());
        dep.addToIndexes();

        // Set up TSV schema
        TsvSchema schema = new TsvSchema();
        Type dependencyType = cas.getCasType(Dependency.type);
        schema.addColumn(new TsvColumn(dependencyType, LayerType.RELATION, "DependencyType",
                FeatureType.PRIMITIVE));
        schema.addColumn(new TsvColumn(dependencyType, LayerType.RELATION, "Governor",
                FeatureType.RELATION_REF));

        // Convert test document content to TSV model
        TsvDocument doc = Tsv3XCasDocumentBuilder.of(schema, cas);
        doc.getSentences().get(0).getTokens().get(1).addUimaAnnotation(dep, false);

        assertEquals(join(asList("1-1\t0-4\tThis\t_\t_\t", "1-2\t5-7\tis\tdep\t1-1\t"), "\n"),
                join(asList(doc.getToken(0, 0), doc.getToken(0, 1)), "\n"));

        // @formatter:off
        String expectedSentence = 
                "#Text=This is a test .\n" + 
                "1-1\t0-4\tThis\t_\t_\t\n" + 
                "1-2\t5-7\tis\tdep\t1-1\t\n" + 
                "1-3\t8-9\ta\t_\t_\t\n" + 
                "1-4\t10-14\ttest\t_\t_\t\n" + 
                "1-5\t15-16\t.\t_\t_\t\n";
        // @formatter:on
        assertEquals(expectedSentence, doc.getSentences().get(0).toString());
    }

    private NamedEntity addNamedEntity(JCas aJCas, int aBegin, int aEnd, String aValue)
    {
        NamedEntity ne = new NamedEntity(aJCas, aBegin, aEnd);
        ne.setValue(aValue);
        ne.addToIndexes();
        return ne;
    }

    private JCas makeJCasOneSentence(String aText) throws UIMAException
    {
        TypeSystemDescription global = createTypeSystemDescription();
        TypeSystemDescription local = createTypeSystemDescriptionFromPath(
                "src/test/resources/desc/type/webannoTestTypes.xml");

        TypeSystemDescription merged = mergeTypeSystems(asList(global, local));

        JCas jcas = createJCas(merged);

        DocumentMetaData.create(jcas).setDocumentId("doc");

        TokenBuilder<Token, Sentence> tb = new TokenBuilder<>(Token.class, Sentence.class);
        tb.buildTokens(jcas, aText);

        // Remove the sentences generated by the token builder which treats the line break as a
        // sentence break
        for (Sentence s : select(jcas, Sentence.class)) {
            s.removeFromIndexes();
        }

        // Add a new sentence covering the whole text
        new Sentence(jcas, 0, jcas.getDocumentText().length()).addToIndexes();

        return jcas;
    }
}
