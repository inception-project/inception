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
package de.tudarmstadt.ukp.inception.externalsearch.pubannotation.format;

import static de.tudarmstadt.ukp.inception.annotation.layer.relation.api.RelationLayerSupport.FEAT_REL_SOURCE;
import static de.tudarmstadt.ukp.inception.annotation.layer.relation.api.RelationLayerSupport.FEAT_REL_TARGET;
import static de.tudarmstadt.ukp.inception.externalsearch.pubannotation.format.PubAnnotationToCasConverter.LABEL_FEATURE;
import static de.tudarmstadt.ukp.inception.project.initializers.basic.BasicRelationLayerInitializer.BASIC_RELATION_LAYER_NAME;
import static de.tudarmstadt.ukp.inception.project.initializers.basic.BasicSpanLayerInitializer.BASIC_SPAN_LAYER_NAME;
import static java.util.Arrays.asList;
import static org.apache.uima.cas.CAS.TYPE_NAME_ANNOTATION;
import static org.apache.uima.cas.CAS.TYPE_NAME_STRING;
import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngine;
import static org.apache.uima.fit.factory.TypeSystemDescriptionFactory.createTypeSystemDescription;
import static org.apache.uima.util.CasCreationUtils.mergeTypeSystems;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;

import org.apache.uima.UIMAFramework;
import org.apache.uima.fit.factory.JCasFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import de.tudarmstadt.ukp.dkpro.core.api.metadata.type.DocumentMetaData;
import de.tudarmstadt.ukp.inception.externalsearch.pubannotation.model.PubAnnotationDocument;
import tools.jackson.databind.ObjectMapper;

public class PubAnnotationWriterTest
{
    private static final String CUSTOM_PROTEIN = "custom.Protein";
    private static final String CUSTOM_INTERACT = "custom.InteractWith";

    @Test
    public void thatWriterEmitsAnnotationsJson(@TempDir File aTmp) throws Exception
    {
        var customTsd = UIMAFramework.getResourceSpecifierFactory().createTypeSystemDescription();
        var protein = customTsd.addType(CUSTOM_PROTEIN, null, TYPE_NAME_ANNOTATION);
        protein.addFeature("uniprot", null, TYPE_NAME_STRING);

        var interact = customTsd.addType(CUSTOM_INTERACT, null, TYPE_NAME_ANNOTATION);
        interact.addFeature(FEAT_REL_SOURCE, null, TYPE_NAME_ANNOTATION);
        interact.addFeature(FEAT_REL_TARGET, null, TYPE_NAME_ANNOTATION);

        var basicSpan = customTsd.addType(BASIC_SPAN_LAYER_NAME, null, TYPE_NAME_ANNOTATION);
        basicSpan.addFeature(LABEL_FEATURE, null, TYPE_NAME_STRING);

        var basicRelation = customTsd.addType(BASIC_RELATION_LAYER_NAME, null,
                TYPE_NAME_ANNOTATION);
        basicRelation.addFeature(LABEL_FEATURE, null, TYPE_NAME_STRING);
        basicRelation.addFeature(FEAT_REL_SOURCE, null, TYPE_NAME_ANNOTATION);
        basicRelation.addFeature(FEAT_REL_TARGET, null, TYPE_NAME_ANNOTATION);

        var merged = mergeTypeSystems(asList(createTypeSystemDescription(), customTsd));
        var jcas = JCasFactory.createJCas(merged);
        jcas.setDocumentText("alpha bravo charlie");

        var dmd = DocumentMetaData.create(jcas);
        dmd.setDocumentId("doc-1");

        var ts = jcas.getTypeSystem();
        var proteinType = ts.getType(CUSTOM_PROTEIN);

        var p1 = jcas.getCas().createAnnotation(proteinType, 0, 5);
        p1.setStringValue(proteinType.getFeatureByBaseName("uniprot"), "Q15306");
        jcas.getCas().addFsToIndexes(p1);

        var p2 = jcas.getCas().createAnnotation(proteinType, 6, 11);
        jcas.getCas().addFsToIndexes(p2);

        var interactType = ts.getType(CUSTOM_INTERACT);
        var rel = jcas.getCas().createAnnotation(interactType, p2.getBegin(), p2.getEnd());
        rel.setFeatureValue(interactType.getFeatureByBaseName(FEAT_REL_SOURCE), p1);
        rel.setFeatureValue(interactType.getFeatureByBaseName(FEAT_REL_TARGET), p2);
        jcas.getCas().addFsToIndexes(rel);

        var writer = createEngine(PubAnnotationWriter.class, //
                PubAnnotationWriter.PARAM_TARGET_LOCATION, aTmp, //
                PubAnnotationWriter.PARAM_SPAN_TYPES, new String[] { CUSTOM_PROTEIN }, //
                PubAnnotationWriter.PARAM_RELATION_TYPES, new String[] { CUSTOM_INTERACT }, //
                PubAnnotationWriter.PARAM_SOURCEDB, "PMC", //
                PubAnnotationWriter.PARAM_SOURCEID, "1234");
        writer.process(jcas);

        var out = new File(aTmp, "doc-1.json");
        assertThat(out).exists();

        var doc = new ObjectMapper().readValue(out, PubAnnotationDocument.class);
        assertThat(doc.getSourceDb()).isEqualTo("PMC");
        assertThat(doc.getSourceId()).isEqualTo("1234");
        assertThat(doc.getText()).isEqualTo("alpha bravo charlie");

        // Short name "Protein" is unique in this CAS — emitted instead of FQN.
        assertThat(doc.getDenotations()).hasSize(2);
        assertThat(doc.getDenotations().get(0).getObject()).isEqualTo("Protein");

        // uniprot=Q15306 attribute present on T1.
        assertThat(doc.getAttributes()).hasSize(1);
        var attr = doc.getAttributes().get(0);
        assertThat(attr.getSubject()).isEqualTo("T1");
        assertThat(attr.getPredicate()).isEqualTo("uniprot");
        assertThat(attr.getObject()).isEqualTo("Q15306");

        // InteractWith short name unique → emitted as "InteractWith".
        assertThat(doc.getRelations()).hasSize(1);
        var r = doc.getRelations().get(0);
        assertThat(r.getPredicate()).isEqualTo("InteractWith");
        assertThat(r.getSubject()).isEqualTo("T1");
        assertThat(r.getObject()).isEqualTo("T2");
    }
}
