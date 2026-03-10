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
package de.tudarmstadt.ukp.inception.annotation.layer.document.curation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.factory.TypeSystemDescriptionFactory;
import org.apache.uima.fit.util.FSCollectionFactory;
import org.apache.uima.fit.util.FSUtil;
import org.apache.uima.jcas.cas.AnnotationBase;
import org.apache.uima.resource.metadata.impl.TypeSystemDescription_impl;
import org.apache.uima.util.CasCreationUtils;
import org.junit.jupiter.api.Test;

import de.tudarmstadt.ukp.dkpro.core.api.metadata.type.DocumentMetaData;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.inception.annotation.feature.link.LinkFeatureDiffMode;
import de.tudarmstadt.ukp.inception.annotation.feature.link.LinkFeatureMultiplicityMode;
import de.tudarmstadt.ukp.inception.annotation.layer.document.api.DocumentPosition;

class DocumentMetadataDiffAdapterTest
{
    private static final String DOC_TYPE = "custom.DocumentLabel";

    @Test
    void selectAnnotationsInWindow_returnsAllAnnotations() throws Exception
    {
        var tsd = TypeSystemDescriptionFactory.createTypeSystemDescription();
        var dld = tsd.addType(DOC_TYPE, "", CAS.TYPE_NAME_ANNOTATION);
        dld.addFeature("label", "", CAS.TYPE_NAME_STRING);
        var fullTsd = CasCreationUtils.mergeTypeSystems(
                List.of(tsd, TypeSystemDescriptionFactory.createTypeSystemDescription()));

        var jcas = JCasFactory.createJCas(fullTsd);

        var cas = jcas.getCas();

        // two document annotations
        var type = cas.getTypeSystem().getType(DOC_TYPE);
        var a1 = cas.createAnnotation(type, 0, 0);
        FSUtil.setFeature(a1, "label", "A");
        cas.addFsToIndexes(a1);

        var a2 = cas.createAnnotation(type, 0, 0);
        FSUtil.setFeature(a2, "label", "B");
        cas.addFsToIndexes(a2);

        var adapter = new DocumentMetadataDiffAdapter(DOC_TYPE, "label");

        var anns = adapter.selectAnnotationsInWindow(cas, 10, 20);

        assertThat(anns).hasSize(2);
    }

    @Test
    void getPosition_buildsDocumentPosition_withDocumentIds() throws Exception
    {
        var tsd = TypeSystemDescriptionFactory.createTypeSystemDescription();
        var dld = tsd.addType(DOC_TYPE, "", CAS.TYPE_NAME_ANNOTATION);
        dld.addFeature("label", "", CAS.TYPE_NAME_STRING);
        var fullTsd = CasCreationUtils.mergeTypeSystems(
                List.of(tsd, TypeSystemDescriptionFactory.createTypeSystemDescription()));

        var jcas = JCasFactory.createJCas(fullTsd);
        var cas = jcas.getCas();

        // ensure document metadata exists and set collection/document id
        var dmd = de.tudarmstadt.ukp.inception.support.uima.WebAnnoCasUtil.getDocumentMetadata(cas);
        try {
            FSUtil.setFeature(dmd, "collectionId", "coll1");
            FSUtil.setFeature(dmd, "documentId", "doc1");
        }
        catch (IllegalArgumentException e) {
            // ignore if feature missing
        }

        var type = cas.getTypeSystem().getType(DOC_TYPE);
        var a1 = cas.createAnnotation(type, 0, 0);
        FSUtil.setFeature(a1, "label", "A");
        cas.addFsToIndexes(a1);

        var adapter = new DocumentMetadataDiffAdapter(DOC_TYPE, "label");

        var pos = adapter.getPosition((org.apache.uima.jcas.cas.AnnotationBase) a1);

        assertThat(pos).isInstanceOf(DocumentPosition.class);
        assertThat(pos.getCollectionId()).isEqualTo("coll1");
        assertThat(pos.getDocumentId()).isEqualTo("doc1");
        assertThat(pos.getType()).isEqualTo(DOC_TYPE);
    }

    @Test
    void getPosition_handlesMissingDocumentMetadataFeatures_gracefully() throws Exception
    {
        var tsd = new TypeSystemDescription_impl();
        tsd.addType(DocumentMetaData.class.getName(), "", CAS.TYPE_NAME_ANNOTATION);
        var dld = tsd.addType(DOC_TYPE, "", CAS.TYPE_NAME_ANNOTATION);
        dld.addFeature("label", "", CAS.TYPE_NAME_STRING);
        var jcas = JCasFactory.createJCas(tsd);
        var cas = jcas.getCas();

        var dmdType = cas.getTypeSystem().getType(DocumentMetaData.class.getName());
        cas.addFsToIndexes(cas.createAnnotation(dmdType, 0, 0));

        var type = cas.getTypeSystem().getType(DOC_TYPE);
        var a1 = cas.createAnnotation(type, 0, 0);
        FSUtil.setFeature(a1, "label", "A");
        cas.addFsToIndexes(a1);

        var adapter = new DocumentMetadataDiffAdapter(DOC_TYPE, "label");

        var pos = adapter.getPosition((AnnotationBase) a1);

        assertThat(pos.getCollectionId()).isNull();
        assertThat(pos.getDocumentId()).isNull();
    }

    @Test
    public void generateSubPositions_returnsEmpty_whenNoLinkFeaturesDeclared() throws Exception
    {
        var tsd = TypeSystemDescriptionFactory.createTypeSystemDescription();
        var dld = tsd.addType(DOC_TYPE, "", CAS.TYPE_NAME_ANNOTATION);
        dld.addFeature("label", "", CAS.TYPE_NAME_STRING);
        var fullTsd = CasCreationUtils.mergeTypeSystems(
                List.of(tsd, TypeSystemDescriptionFactory.createTypeSystemDescription()));

        var jcas = JCasFactory.createJCas(fullTsd);
        var cas = jcas.getCas();

        // create a host with links but adapter has no link features declared
        var hostType = cas.getTypeSystem().getType(DOC_TYPE);
        AnnotationFS host = cas.createAnnotation(hostType, 0, 0);
        cas.addFsToIndexes(host);

        var adapter = new DocumentMetadataDiffAdapter(DOC_TYPE, "label");

        var subs = adapter.generateSubPositions((org.apache.uima.jcas.cas.AnnotationBase) host);

        assertThat(subs).isEmpty();
    }

    @Test
    void generateSubPositions_skipsNullArrayFeature_and_buildsPositions_forEachLinkEntry()
        throws Exception
    {
        // Build a small type system: LinkType + Document label host with links FSArray
        var tsd = new TypeSystemDescription_impl();

        var linkTD = tsd.addType("LinkType", "", CAS.TYPE_NAME_TOP);
        linkTD.addFeature("role", "", CAS.TYPE_NAME_STRING);
        linkTD.addFeature("target", "", Token.class.getName());

        var hostTD = tsd.addType(DOC_TYPE, "", CAS.TYPE_NAME_ANNOTATION);
        hostTD.addFeature("links", "", CAS.TYPE_NAME_FS_ARRAY, linkTD.getName(), false);
        hostTD.addFeature("label", "", CAS.TYPE_NAME_STRING);

        var fullTsd = CasCreationUtils.mergeTypeSystems(
                List.of(tsd, TypeSystemDescriptionFactory.createTypeSystemDescription()));

        var jcas = JCasFactory.createJCas(fullTsd);
        var cas = jcas.getCas();

        // create token target
        var t1 = new Token(jcas, 0, 1);
        t1.addToIndexes();
        var t2 = new Token(jcas, 2, 3);
        t2.addToIndexes();

        // create two link FS entries
        var lt = cas.getTypeSystem().getType("LinkType");
        var roleF = lt.getFeatureByBaseName("role");
        var targetF = lt.getFeatureByBaseName("target");

        var link1 = cas.createFS(lt);
        link1.setStringValue(roleF, "r1");
        link1.setFeatureValue(targetF, t1);
        cas.addFsToIndexes(link1);

        var link2 = cas.createFS(lt);
        link2.setStringValue(roleF, "r2");
        link2.setFeatureValue(targetF, t2);
        cas.addFsToIndexes(link2);

        // attach to host
        var hostType = cas.getTypeSystem().getType(DOC_TYPE);
        var host = cas.createAnnotation(hostType, 0, 0);
        host.setFeatureValue(hostType.getFeatureByBaseName("links"),
                FSCollectionFactory.createFSArray(jcas, List.of(link1, link2)));
        cas.addFsToIndexes(host);

        var adapter = new DocumentMetadataDiffAdapter(DOC_TYPE, "label");
        adapter.addLinkFeature("links", "role", "target",
                LinkFeatureMultiplicityMode.MULTIPLE_TARGETS_MULTIPLE_ROLES,
                LinkFeatureDiffMode.INCLUDE);

        var subs = adapter.generateSubPositions((org.apache.uima.jcas.cas.AnnotationBase) host);

        assertThat(subs).hasSize(2);

        var p1 = subs.get(0);
        assertThat(p1.getLinkFeature()).isEqualTo("links");
        assertThat(p1.getLinkRole()).isIn("r1", "r2");
        assertThat(p1.getLinkTargetBegin()).isIn(0, 2);
        assertThat(p1.getLinkTargetEnd()).isIn(1, 3);
        assertThat(p1.getLinkFeatureMultiplicityMode())
                .isEqualTo(LinkFeatureMultiplicityMode.MULTIPLE_TARGETS_MULTIPLE_ROLES);
    }
}
