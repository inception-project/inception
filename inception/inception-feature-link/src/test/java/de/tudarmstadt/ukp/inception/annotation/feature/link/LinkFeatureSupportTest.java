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
package de.tudarmstadt.ukp.inception.annotation.feature.link;

import static java.util.Arrays.asList;
import static org.apache.uima.fit.factory.JCasFactory.createJCasFromPath;
import static org.apache.uima.fit.util.FSUtil.setFeature;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;

import org.apache.uima.cas.Type;
import org.apache.uima.jcas.JCas;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.LinkMode;
import de.tudarmstadt.ukp.clarin.webanno.model.MultiValueMode;
import de.tudarmstadt.ukp.clarin.webanno.model.Tag;
import de.tudarmstadt.ukp.clarin.webanno.model.TagSet;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.schema.api.feature.LinkWithRoleModel;
import de.tudarmstadt.ukp.inception.support.uima.ICasUtil;

@ExtendWith(MockitoExtension.class)
public class LinkFeatureSupportTest
{
    private @Mock AnnotationSchemaService schemaService;

    private LinkFeatureSupport sut;

    private AnnotationFeature slotFeature;

    private JCas jcas;
    private Type hostType;
    private Type targetType;
    private Type linkType;

    @BeforeEach
    public void setUp() throws Exception
    {
        sut = new LinkFeatureSupport(schemaService);

        slotFeature = new AnnotationFeature("links", "webanno.custom.SimpleSpan");
        slotFeature.setLinkTypeName("webanno.custom.LinkType");
        slotFeature.setLinkMode(LinkMode.WITH_ROLE);
        slotFeature.setLinkTypeRoleFeatureName("role");
        slotFeature.setLinkTypeTargetFeatureName("target");
        slotFeature.setMode(MultiValueMode.ARRAY);

        jcas = createJCasFromPath("src/test/resources/desc/type/webannoTestTypes.xml");
        jcas.setDocumentText("label");
        hostType = jcas.getCas().getTypeSystem().getType("webanno.custom.SimpleLinkHost");
        targetType = jcas.getCas().getTypeSystem().getType(slotFeature.getType());
        linkType = jcas.getCas().getTypeSystem().getType(slotFeature.getLinkTypeName());
    }

    @Test
    public void testAccepts()
    {
        var feat1 = new AnnotationFeature("string", "LinkType");
        feat1.setMode(MultiValueMode.ARRAY);
        feat1.setLinkMode(LinkMode.WITH_ROLE);

        var feat2 = new AnnotationFeature("Dummy feature", "someType");

        assertThat(sut.accepts(feat1)).isTrue();
        assertThat(sut.accepts(feat2)).isFalse();
    }

    @Test
    public void testWrapUnwrap() throws Exception
    {
        var cas = jcas.getCas();

        var links = new ArrayList<LinkWithRoleModel>();
        links.add(new LinkWithRoleModel("role", "label", 3));

        var targetFS = cas.createAnnotation(targetType, 0, cas.getDocumentText().length());

        var array = cas.createArrayFS(1);
        var linkFS = cas.createFS(linkType);
        setFeature(linkFS, slotFeature.getLinkTypeRoleFeatureName(), "role");
        setFeature(linkFS, slotFeature.getLinkTypeTargetFeatureName(), targetFS);
        array.set(0, linkFS);

        assertThat(sut.wrapFeatureValue(slotFeature, cas, array)).isEqualTo(links);
        assertThat(sut.wrapFeatureValue(slotFeature, cas, null)).isEmpty();
        assertThatThrownBy(() -> sut.wrapFeatureValue(slotFeature, cas, new Object()))
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(sut.unwrapFeatureValue(slotFeature, links)).isSameAs(links);
        assertThat(sut.unwrapFeatureValue(slotFeature, null)).isNull();
        assertThatThrownBy(() -> sut.unwrapFeatureValue(slotFeature, new Object()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void thatUsingOutOfTagsetValueInClosedTagsetProducesException() throws Exception
    {
        final String role = "TAG-NOT-IN-LIST";

        var cas = jcas.getCas();

        var slotFeatureTagset = new TagSet();
        slotFeatureTagset.setCreateTag(false);

        slotFeature.setTagset(slotFeatureTagset);

        var hostFS = cas.createAnnotation(hostType, 0, cas.getDocumentText().length());
        var targetFS = cas.createAnnotation(targetType, 0, cas.getDocumentText().length());

        when(schemaService.existsTag(role, slotFeatureTagset)).thenReturn(false);

        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> sut.setFeatureValue(jcas.getCas(), slotFeature,
                        ICasUtil.getAddr(hostFS),
                        asList(new LinkWithRoleModel(role, "dummy", ICasUtil.getAddr(targetFS)))))
                .withMessageContaining("is not in the tag list");
    }

    @Test
    public void thatUsingOutOfTagsetValueInOpenTagsetAddsNewValue() throws Exception
    {
        final String role = "TAG-NOT-IN-LIST";

        var cas = jcas.getCas();

        var slotFeatureTagset = new TagSet();
        slotFeatureTagset.setCreateTag(true);

        slotFeature.setTagset(slotFeatureTagset);

        var hostFS = cas.createAnnotation(hostType, 0, cas.getDocumentText().length());
        var targetFS = cas.createAnnotation(targetType, 0, cas.getDocumentText().length());

        when(schemaService.existsTag(role, slotFeatureTagset)).thenReturn(false);

        sut.setFeatureValue(jcas.getCas(), slotFeature, ICasUtil.getAddr(hostFS),
                asList(new LinkWithRoleModel(role, "dummy", ICasUtil.getAddr(targetFS))));

        verify(schemaService).createTag(new Tag(slotFeatureTagset, role));
    }
}
