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
package de.tudarmstadt.ukp.inception.curation.merge;

import static de.tudarmstadt.ukp.inception.curation.merge.CasMergeOperationResult.ResultState.CREATED;
import static de.tudarmstadt.ukp.inception.curation.merge.CasMergeOperationResult.ResultState.UPDATED;
import static de.tudarmstadt.ukp.inception.support.uima.FeatureStructureBuilder.buildFS;
import static de.tudarmstadt.ukp.inception.support.uima.ICasUtil.getAddr;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.apache.uima.fit.util.FSUtil.getFeature;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.apache.uima.UIMAFramework;
import org.apache.uima.cas.CAS;
import org.apache.uima.fit.factory.CasFactory;
import org.apache.uima.fit.factory.TypeSystemDescriptionFactory;
import org.apache.uima.jcas.cas.AnnotationBase;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.CasCreationUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;

import de.tudarmstadt.ukp.inception.annotation.events.FeatureValueUpdatedEvent;
import de.tudarmstadt.ukp.inception.annotation.layer.document.DocumentMetadataLayerAdapterImpl;
import de.tudarmstadt.ukp.inception.annotation.layer.document.api.DocumentMetadataLayerTraits;
import de.tudarmstadt.ukp.inception.annotation.layer.document.api.event.DocumentMetadataCreatedEvent;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotationException;
import de.tudarmstadt.ukp.inception.schema.api.adapter.IllegalFeatureValueException;
import de.tudarmstadt.ukp.inception.support.json.JSONUtil;

@Execution(CONCURRENT)
class CasMergeDocumentTest
    extends CasMergeTestBase
{
    private static final String DUMMY_USER = "dummyTargetUser";

    @Test
    void simpleCopyToEmptyTest() throws Exception
    {
        var sourceCas = createCas();
        var sourceFS = createDocumentLabel(sourceCas, "NN");

        var targetCas = createCas();

        sut.mergeDocumentAnnotation(document, DUMMY_USER, documentLabelLayer, targetCas, sourceFS);

        assertThat(targetCas.select(documentLabelLayer.getName()).asList()) //
                .hasSize(1);
    }

    @Test
    void simpleCopyToSameExistingAnnoTest() throws Exception
    {
        var sourceCas = createCas();
        var sourceFS = createDocumentLabel(sourceCas, "NN");

        var targetCas = createCas();
        buildFS(targetCas, DOCUMENT_LABEL_TYPE) //
                .withFeature("label", getFeature(sourceFS, "label", String.class)) //
                .buildAndAddToIndexes();

        assertThatExceptionOfType(AnnotationException.class) //
                .isThrownBy(() -> sut.mergeDocumentAnnotation(document, DUMMY_USER,
                        documentLabelLayer, targetCas, sourceFS))
                .withMessageContaining("annotation already exists");
    }

    @Test
    void simpleCopyToDiffExistingAnnoWithNoStackingTest() throws Exception
    {
        var sourceCas = createCas();
        var sourceFS = createDocumentLabel(sourceCas, "NN");

        var traits = new DocumentMetadataLayerTraits();
        traits.setSingleton(true);
        documentLabelLayer.setTraits(JSONUtil.toJsonString(traits));

        var targetCas = createCas();
        buildFS(targetCas, DOCUMENT_LABEL_TYPE) //
                .withFeature("label", "NE") //
                .buildAndAddToIndexes();

        sut.mergeDocumentAnnotation(document, DUMMY_USER, documentLabelLayer, targetCas, sourceFS);

        assertThat(targetCas.select(documentLabelLayer.getName()).asList()) //
                .as("Target feature value should be overwritten by source feature value")
                .extracting( //
                        a -> getFeature(a, "label", String.class))
                .containsExactly( //
                        getFeature(sourceFS, "label", String.class));
    }

    @Test
    void simpleCopyToDiffExistingAnnoWithStackingTest() throws Exception
    {
        var sourceCas = createCas();
        var sourceFS = createDocumentLabel(sourceCas, "NN");

        var targetCas = createCas();
        var existingFs = buildFS(targetCas, DOCUMENT_LABEL_TYPE) //
                .withFeature("label", "NE") //
                .buildAndAddToIndexes();

        sut.mergeDocumentAnnotation(document, DUMMY_USER, documentLabelLayer, targetCas, sourceFS);

        assertThat(targetCas.select(documentLabelLayer.getName()).asList()) //
                .as("Source FS should be added alongside existing FS in target") //
                .extracting( //
                        a -> getFeature(a, "label", String.class))
                .containsExactlyInAnyOrder( //
                        getFeature(existingFs, "label", String.class),
                        getFeature(sourceFS, "label", String.class));
    }

    @Test
    void thatFailedMergePublishesNoEvents() throws Exception
    {
        var events = recordEvents();
        doThrow(new IllegalFeatureValueException("Not in the tagset")) //
                .when(schemaService).createMissingTag(any(), eq("NOT-IN-TAGSET"));

        var sourceCas = createCas();
        var sourceFS = createDocumentLabel(sourceCas, "NOT-IN-TAGSET");

        var targetCas = createCas();

        assertThatExceptionOfType(IllegalFeatureValueException.class) //
                .isThrownBy(() -> sut.mergeDocumentAnnotation(document, DUMMY_USER,
                        documentLabelLayer, targetCas, sourceFS))
                .withMessage("Not in the tagset");

        assertThat(targetCas.select(documentLabelLayer.getName()).asList()) //
                .as("Annotation created by the failed merge should be removed again") //
                .isEmpty();
        assertThat(events) //
                .as("Events of the failed merge should not be published") //
                .isEmpty();
    }

    @Test
    void thatSuccessfulMergePublishesEvents() throws Exception
    {
        var events = recordEvents();

        var sourceCas = createCas();
        var sourceFS = createDocumentLabel(sourceCas, "NN");

        var targetCas = createCas();

        sut.mergeDocumentAnnotation(document, DUMMY_USER, documentLabelLayer, targetCas, sourceFS);

        assertThat(events) //
                .hasExactlyElementsOfTypes(DocumentMetadataCreatedEvent.class,
                        FeatureValueUpdatedEvent.class);
    }

    /**
     * Makes the merge use a document label adapter that records the events it publishes.
     */
    private List<Object> recordEvents()
    {
        var events = new ArrayList<Object>();
        var adapter = new DocumentMetadataLayerAdapterImpl(layerSupportRegistry,
                featureSupportRegistry, events::add, documentLabelLayer,
                () -> schemaService.listAnnotationFeature(documentLabelLayer), constraintsService,
                emptyList());
        when(schemaService.getAdapter(documentLabelLayer)).thenReturn(adapter);
        return events;
    }

    @Test
    void thatFeatureMissingFromSourceDoesNotFailMerge() throws Exception
    {
        // The source CAS was last saved before the label feature was added to the layer
        var sourceCas = createCasWithoutLabelFeature();
        var sourceFS = (AnnotationBase) buildFS(sourceCas, documentLabelLayer.getName()) //
                .buildAndAddToIndexes();

        var targetCas = createCas();

        var result = sut.mergeDocumentAnnotation(document, DUMMY_USER, documentLabelLayer,
                targetCas, sourceFS);

        assertThat(result.state()).isEqualTo(CREATED);
        assertThat(targetCas.select(documentLabelLayer.getName()).asList()) //
                .extracting(a -> getFeature(a, "label", String.class)) //
                .containsExactly((String) null);
    }

    @Test
    void thatFeatureMissingFromSourceOverwritesExistingValueWithUnset() throws Exception
    {
        var traits = new DocumentMetadataLayerTraits();
        traits.setSingleton(true);
        documentLabelLayer.setTraits(JSONUtil.toJsonString(traits));

        // The source CAS was last saved before the label feature was added to the layer
        var sourceCas = createCasWithoutLabelFeature();
        var sourceFS = (AnnotationBase) buildFS(sourceCas, documentLabelLayer.getName()) //
                .buildAndAddToIndexes();

        var targetCas = createCas();
        var existing = createDocumentLabel(targetCas, "NE");

        var result = sut.mergeDocumentAnnotation(document, DUMMY_USER, documentLabelLayer,
                targetCas, sourceFS);

        assertThat(result.state()).isEqualTo(UPDATED);
        assertThat(result.targetAddress()).isEqualTo(getAddr(existing));
        assertThat(targetCas.select(documentLabelLayer.getName()).asList()) //
                .as("The existing annotation should be updated, not replaced") //
                .containsExactly(existing);
        assertThat(getFeature(existing, "label", String.class)) //
                .as("Like an upgraded source CAS, the source has no label, so the merge unsets it")
                .isNull();
    }

    private CAS createCas() throws ResourceInitializationException
    {
        return createCas(true);
    }

    private CAS createCasWithoutLabelFeature() throws ResourceInitializationException
    {
        return createCas(false);
    }

    private CAS createCas(boolean aWithLabelFeature) throws ResourceInitializationException
    {
        var tsd = UIMAFramework.getResourceSpecifierFactory().createTypeSystemDescription();
        var dld = tsd.addType(documentLabelLayer.getName(), "", CAS.TYPE_NAME_ANNOTATION_BASE);
        if (aWithLabelFeature) {
            dld.addFeature("label", "", CAS.TYPE_NAME_STRING);
        }
        dld.addFeature("i7n_uiOrder", "", CAS.TYPE_NAME_INTEGER);
        var fullTsd = CasCreationUtils.mergeTypeSystems( //
                asList(tsd, TypeSystemDescriptionFactory.createTypeSystemDescription()));
        return CasFactory.createCas(fullTsd);
    }

    private AnnotationBase createDocumentLabel(CAS aCas, String aLabel)
    {
        return (AnnotationBase) buildFS(aCas, documentLabelLayer.getName()) //
                .withFeature("label", aLabel) //
                .buildAndAddToIndexes();
    }
}
