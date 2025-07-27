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
package de.tudarmstadt.ukp.inception.search.index.mtas;

import static de.tudarmstadt.ukp.clarin.webanno.model.AnchoringMode.SINGLE_TOKEN;
import static de.tudarmstadt.ukp.inception.support.WebAnnoConst.FEAT_REL_SOURCE;
import static de.tudarmstadt.ukp.inception.support.WebAnnoConst.FEAT_REL_TARGET;
import static de.tudarmstadt.ukp.inception.support.WebAnnoConst.RELATION_TYPE;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.apache.uima.cas.CAS.TYPE_NAME_BOOLEAN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.apache.uima.cas.CAS;
import org.apache.uima.fit.factory.JCasBuilder;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.testing.factory.TokenBuilder;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.StringArray;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import de.tudarmstadt.ukp.clarin.webanno.constraints.ConstraintsService;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.Dependency;
import de.tudarmstadt.ukp.inception.annotation.feature.bool.BooleanFeatureSupport;
import de.tudarmstadt.ukp.inception.annotation.feature.multistring.MultiValueStringFeatureSupport;
import de.tudarmstadt.ukp.inception.annotation.feature.number.NumberFeatureSupport;
import de.tudarmstadt.ukp.inception.annotation.feature.string.StringFeatureSupport;
import de.tudarmstadt.ukp.inception.annotation.feature.string.StringFeatureSupportPropertiesImpl;
import de.tudarmstadt.ukp.inception.annotation.layer.relation.RelationAdapterImpl;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureSupportRegistryImpl;
import de.tudarmstadt.ukp.inception.schema.api.layer.LayerSupportRegistryImpl;
import de.tudarmstadt.ukp.inception.search.FeatureIndexingSupportRegistryImpl;
import de.tudarmstadt.ukp.inception.search.PrimitiveUimaIndexingSupport;
import de.tudarmstadt.ukp.inception.search.model.AnnotationSearchState;
import inception.test.MultiValueHost;
import inception.test.PrimitiveHost;
import mtas.analysis.token.MtasToken;
import mtas.analysis.token.MtasTokenCollection;
import mtas.analysis.util.MtasParserException;

@ExtendWith(MockitoExtension.class)
public class MtasUimaParserTest
{
    protected @Mock ConstraintsService constraintsService;
    private @Mock AnnotationSchemaService annotationSchemaService;

    private LayerSupportRegistryImpl layerSupportRegistry;
    private FeatureSupportRegistryImpl featureSupportRegistry;
    private FeatureIndexingSupportRegistryImpl featureIndexingSupportRegistry;

    private AnnotationSearchState prefs;
    private Project project;
    private JCas jcas;

    private AnnotationLayer namedEntityLayer;
    private AnnotationFeature namedEntityValueFeature;
    private AnnotationFeature namedEntityIdentifierFeature;

    private AnnotationLayer tokenLayer;
    private AnnotationFeature tokenLayerPos;

    private AnnotationLayer posLayer;
    private AnnotationFeature posLayerValue;

    private AnnotationLayer depLayer;
    private AnnotationFeature dependencyLayerGovernor;
    private AnnotationFeature dependencyLayerDependent;

    private AnnotationLayer multiValueHostLayer;
    private AnnotationFeature multiValueHostStringArrayFeature;

    private AnnotationLayer primitiveHostLayer;
    private AnnotationFeature primitiveHostBooleanFeature;

    @BeforeEach
    public void setup() throws Exception
    {
        project = new Project();
        project.setId(1l);
        project.setName("test project");

        namedEntityLayer = AnnotationLayer.builder() //
                .forJCasClass(NamedEntity.class) //
                .withProject(project) //
                .build();

        namedEntityValueFeature = AnnotationFeature.builder() //
                .withLayer(namedEntityLayer) //
                .withName(NamedEntity._FeatName_value) //
                .withUiName(NamedEntity._FeatName_value) //
                .withType(CAS.TYPE_NAME_STRING) //
                .build();

        namedEntityIdentifierFeature = AnnotationFeature.builder() //
                .withLayer(namedEntityLayer) //
                .withName(NamedEntity._FeatName_identifier) //
                .withUiName(NamedEntity._FeatName_identifier) //
                .withRange(CAS.TYPE_NAME_STRING) //
                .build();

        tokenLayer = AnnotationLayer.builder() //
                .forJCasClass(Token.class) //
                .withProject(project) //
                .withAnchoringMode(SINGLE_TOKEN) //
                .build();

        tokenLayerPos = AnnotationFeature.builder() //
                .withLayer(tokenLayer) //
                .withName(Token._FeatName_pos) //
                .withRange(POS.class) //
                .build();

        posLayer = AnnotationLayer.builder() //
                .forJCasClass(POS.class) //
                .withProject(project) //
                .withAnchoringMode(SINGLE_TOKEN) //
                .build();

        posLayerValue = AnnotationFeature.builder() //
                .withLayer(posLayer) //
                .withName(POS._FeatName_PosValue) //
                .withUiName(POS._FeatName_PosValue) //
                .withRange(CAS.TYPE_NAME_STRING) //
                .build();

        depLayer = AnnotationLayer.builder() //
                .forJCasClass(Dependency.class) //
                .withType(RELATION_TYPE) //
                .withProject(project) //
                .withAnchoringMode(SINGLE_TOKEN) //
                .withAttachFeature(tokenLayerPos) //
                .build();

        dependencyLayerGovernor = AnnotationFeature.builder() //
                .withLayer(depLayer) //
                .withName(FEAT_REL_SOURCE) //
                .withUiName(FEAT_REL_SOURCE) //
                .withRange(Token.class) //
                .build();

        dependencyLayerDependent = AnnotationFeature.builder() //
                .withLayer(depLayer) //
                .withName(FEAT_REL_TARGET) //
                .withUiName(FEAT_REL_TARGET) //
                .withRange(Token.class) //
                .build();

        multiValueHostLayer = AnnotationLayer.builder() //
                .forJCasClass(MultiValueHost.class) //
                .withProject(project) //
                .build();

        multiValueHostStringArrayFeature = AnnotationFeature.builder() //
                .withLayer(multiValueHostLayer) //
                .withName(MultiValueHost._FeatName_stringArray) //
                .withUiName(MultiValueHost._FeatName_stringArray) //
                .withRange(StringArray.class) //
                .build();

        primitiveHostLayer = AnnotationLayer.builder() //
                .forJCasClass(PrimitiveHost.class) //
                .withProject(project) //
                .build();

        primitiveHostBooleanFeature = AnnotationFeature.builder() //
                .withLayer(primitiveHostLayer) //
                .withName(PrimitiveHost._FeatName_booleanFeature) //
                .withUiName(PrimitiveHost._FeatName_booleanFeature) //
                .withRange(TYPE_NAME_BOOLEAN) //
                .build();

        prefs = new AnnotationSearchState();

        layerSupportRegistry = new LayerSupportRegistryImpl(emptyList());

        featureSupportRegistry = new FeatureSupportRegistryImpl(asList( //
                new StringFeatureSupport(), //
                new BooleanFeatureSupport(), //
                new NumberFeatureSupport(), //
                new MultiValueStringFeatureSupport(new StringFeatureSupportPropertiesImpl(),
                        null)));
        featureSupportRegistry.init();

        featureIndexingSupportRegistry = new FeatureIndexingSupportRegistryImpl(asList( //
                new PrimitiveUimaIndexingSupport(featureSupportRegistry)));
        featureIndexingSupportRegistry.init();

        // Resetting the JCas is faster than re-creating it
        if (jcas == null) {
            jcas = JCasFactory.createJCas();
        }
        else {
            jcas.reset();
        }
    }

    @Test
    public void testSentencesAndTokens() throws Exception
    {
        var builder = TokenBuilder.create(Token.class, Sentence.class);
        builder.buildTokens(jcas, "This is a test . \n This is sentence two .");

        var sut = new MtasUimaParser(asList(), annotationSchemaService,
                featureIndexingSupportRegistry, prefs);
        MtasTokenCollection tc = sut.createTokenCollection(jcas.getCas());

        // MtasUtils.print(tc);

        List<MtasToken> tokens = toList(tc);

        assertThat(tokens) //
                .filteredOn(t -> "Token".equals(t.getPrefix())) //
                .extracting(MtasToken::getPostfix) //
                .containsExactly( //
                        "this", "is", "a", "test", ".", "this", "is", "sentence", "two", ".");

        assertThat(tokens) //
                .filteredOn(t -> "s".equals(t.getPrefix())) //
                .extracting(MtasToken::getPostfix)
                .containsExactly("this is a test .", "this is sentence two .");
    }

    @Test
    public void testNamedEnity() throws Exception
    {
        var builder = new JCasBuilder(jcas);
        builder.add("I", Token.class);
        builder.add(" ");
        builder.add("am", Token.class);
        builder.add(" ");
        int begin = builder.getPosition();
        builder.add("John", Token.class);
        builder.add(" ");
        builder.add("Smith", Token.class);
        NamedEntity ne = new NamedEntity(jcas, begin, builder.getPosition());
        ne.setValue("PER");
        ne.addToIndexes();
        builder.add(" ");
        builder.add(".", Token.class);

        MtasUimaParser sut = new MtasUimaParser(
                asList(namedEntityValueFeature, namedEntityIdentifierFeature),
                annotationSchemaService, featureIndexingSupportRegistry, prefs);

        MtasTokenCollection tc = sut.createTokenCollection(jcas.getCas());
        // MtasUtils.print(tc);

        List<MtasToken> tokens = new ArrayList<>();
        tc.iterator().forEachRemaining(tokens::add);

        assertThat(tokens) //
                .filteredOn(t -> t.getPrefix().startsWith(namedEntityLayer.getUiName()))
                .extracting(MtasToken::getPrefix, MtasToken::getPostfix) //
                .containsExactly( //
                        tuple("NamedEntity", ""), //
                        tuple("NamedEntity.value", "PER"));
    }

    @Test
    void testMultiValueStringFeature() throws Exception
    {
        TokenBuilder<Token, Sentence> builder = TokenBuilder.create(Token.class, Sentence.class);
        builder.buildTokens(jcas, "test");

        var mvh = new MultiValueHost(jcas, 0, 4);
        mvh.setStringArray(StringArray.create(jcas, new String[] { "a", "b" }));
        mvh.addToIndexes();

        MtasUimaParser sut = new MtasUimaParser(asList(multiValueHostStringArrayFeature),
                annotationSchemaService, featureIndexingSupportRegistry, prefs);
        MtasTokenCollection result = sut.createTokenCollection(jcas.getCas());

        // MtasUtils.print(result);

        List<MtasToken> tokens = toList(result);

        assertThat(tokens) //
                .filteredOn(t -> t.getPrefix().startsWith(multiValueHostLayer.getUiName())) //
                .extracting(MtasToken::getPrefix, MtasToken::getPostfix) //
                .containsExactly( //
                        tuple("MultiValueHost", "test"), //
                        tuple("MultiValueHost.stringArray", "a"), //
                        tuple("MultiValueHost.stringArray", "b"));
    }

    @Test
    void testBooleanFeature() throws Exception
    {
        TokenBuilder<Token, Sentence> builder = TokenBuilder.create(Token.class, Sentence.class);
        builder.buildTokens(jcas, "test");

        var ph1 = new PrimitiveHost(jcas, 0, 4);
        ph1.setBooleanFeature(true);
        ph1.addToIndexes();

        var ph2 = new PrimitiveHost(jcas, 0, 4);
        ph2.setBooleanFeature(false);
        ph2.addToIndexes();

        MtasUimaParser sut = new MtasUimaParser(asList(primitiveHostBooleanFeature),
                annotationSchemaService, featureIndexingSupportRegistry, prefs);
        MtasTokenCollection result = sut.createTokenCollection(jcas.getCas());

        // MtasUtils.print(result);

        List<MtasToken> tokens = toList(result);

        assertThat(tokens) //
                .filteredOn(t -> t.getPrefix().startsWith(primitiveHostLayer.getUiName())) //
                .extracting(MtasToken::getPrefix, MtasToken::getPostfix) //
                .containsExactly( //
                        tuple("PrimitiveHost", "test"), //
                        tuple("PrimitiveHost", "test"), //
                        tuple("PrimitiveHost.booleanFeature", "false"), //
                        tuple("PrimitiveHost.booleanFeature", "true"));
    }

    @Test
    public void testZeroWidthSpanNotIndexed() throws Exception
    {
        TokenBuilder<Token, Sentence> builder = TokenBuilder.create(Token.class, Sentence.class);
        builder.buildTokens(jcas, "This is a test . \n This is sentence two .");

        NamedEntity zeroWidthNe = new NamedEntity(jcas, 4, 4);
        zeroWidthNe.setValue("OTH");
        zeroWidthNe.addToIndexes();

        MtasUimaParser sut = new MtasUimaParser(
                asList(namedEntityValueFeature, namedEntityIdentifierFeature),
                annotationSchemaService, featureIndexingSupportRegistry, prefs);
        MtasTokenCollection result = sut.createTokenCollection(jcas.getCas());

        // MtasUtils.print(result);

        List<MtasToken> tokens = new ArrayList<>();
        result.iterator().forEachRemaining(tokens::add);

        assertThat(tokens) //
                .filteredOn(t -> t.getPrefix().startsWith(namedEntityLayer.getUiName())) //
                .isEmpty();
    }

    @Test
    public void testDependencyRelation() throws Exception
    {
        // Set up document with a dummy dependency relation
        jcas.setDocumentText("a b");
        Token t1 = new Token(jcas, 0, 1);
        t1.addToIndexes();

        POS p1 = new POS(jcas, t1.getBegin(), t1.getEnd());
        p1.setPosValue("A");
        t1.setPos(p1);
        p1.addToIndexes();

        Token t2 = new Token(jcas, 2, 3);
        t2.addToIndexes();

        POS p2 = new POS(jcas, t2.getBegin(), t2.getEnd());
        p2.setPosValue("B");
        t2.setPos(p2);
        p2.addToIndexes();

        Dependency d1 = new Dependency(jcas, t2.getBegin(), t2.getEnd());
        d1.setDependent(t2);
        d1.setGovernor(t1);
        d1.addToIndexes();

        when(annotationSchemaService.getAdapter(depLayer)) //
                .thenReturn(new RelationAdapterImpl(layerSupportRegistry, featureSupportRegistry,
                        null, depLayer, FEAT_REL_TARGET, FEAT_REL_SOURCE,
                        () -> asList(dependencyLayerGovernor, dependencyLayerDependent),
                        emptyList(), constraintsService));

        var sut = new MtasUimaParser(
                asList(tokenLayerPos, posLayerValue, dependencyLayerGovernor,
                        dependencyLayerDependent),
                annotationSchemaService, featureIndexingSupportRegistry, prefs);
        var tc = sut.createTokenCollection(jcas.getCas());

        // MtasUtils.print(tc);

        var tokens = new ArrayList<MtasToken>();
        tc.iterator().forEachRemaining(tokens::add);

        assertThat(tokens) //
                .filteredOn(t -> t.getPrefix().startsWith("Dependency"))
                .extracting(t -> t.getPrefix() + "=" + t.getPostfix()) //
                .containsExactly( //
                        "Dependency=b", //
                        "Dependency-source=a", //
                        "Dependency-source.PosValue=A", //
                        "Dependency-target=b", //
                        "Dependency-target.PosValue=B");
    }

    private List<MtasToken> toList(MtasTokenCollection result) throws MtasParserException
    {
        var tokens = new ArrayList<MtasToken>();
        result.iterator().forEachRemaining(tokens::add);
        return tokens;
    }
}
