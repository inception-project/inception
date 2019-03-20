/*
 * Copyright 2018
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
package de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature;

import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.getAddr;
import static java.util.Arrays.asList;
import static org.apache.uima.fit.factory.JCasFactory.createJCasFromPath;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.ArrayList;
import java.util.List;

import org.apache.uima.cas.ArrayFS;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.util.FSUtil;
import org.apache.uima.jcas.JCas;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.LinkWithRoleModel;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.LinkMode;
import de.tudarmstadt.ukp.clarin.webanno.model.MultiValueMode;
import de.tudarmstadt.ukp.clarin.webanno.model.Tag;
import de.tudarmstadt.ukp.clarin.webanno.model.TagSet;

public class SlotFeatureSupportTest
{
    private @Mock AnnotationSchemaService schemaService;

    private SlotFeatureSupport sut;
    
    private AnnotationFeature slotFeature;
    
    private JCas jcas;
    private Type hostType;
    private Type targetType;
    private Type linkType;
    
    @Before
    public void setUp() throws Exception
    {
        initMocks(this);
        
        sut = new SlotFeatureSupport(schemaService);
        
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
        AnnotationFeature feat1 = new AnnotationFeature("string", "LinkType");
        feat1.setMode(MultiValueMode.ARRAY);
        feat1.setLinkMode(LinkMode.WITH_ROLE);
        
        AnnotationFeature feat2 = new AnnotationFeature("Dummy feature", "someType");
        
        assertThat(sut.accepts(feat1)).isTrue();
        assertThat(sut.accepts(feat2)).isFalse();
    }
    
    @Test
    public void testWrapUnwrap() throws Exception
    {
        CAS cas = jcas.getCas();
        
        List<LinkWithRoleModel> links = new ArrayList<>();
        links.add(new LinkWithRoleModel("role", "label", 13));
                
        AnnotationFS targetFS = cas.createAnnotation(targetType, 0, cas.getDocumentText().length());
        
        ArrayFS array = cas.createArrayFS(1);
        FeatureStructure linkFS = cas.createFS(linkType);
        FSUtil.setFeature(linkFS, slotFeature.getLinkTypeRoleFeatureName(), "role");
        FSUtil.setFeature(linkFS, slotFeature.getLinkTypeTargetFeatureName(), targetFS);
        array.set(0, linkFS);
        
        assertThat(sut.wrapFeatureValue(slotFeature, cas, array)).isEqualTo(links);
        assertThat(sut.wrapFeatureValue(slotFeature, cas, null)).isEmpty();
        assertThatThrownBy(() -> sut.wrapFeatureValue(slotFeature, cas, new Object()))
                .isInstanceOf(IllegalArgumentException.class);
        
        assertThat(sut.unwrapFeatureValue(slotFeature, cas, links)).isSameAs(links);
        assertThat(sut.unwrapFeatureValue(slotFeature, cas, null)).isNull();
        assertThatThrownBy(() -> sut.unwrapFeatureValue(slotFeature, cas, new Object()))
                .isInstanceOf(IllegalArgumentException.class);
    }
    
    @Test
    public void thatUsingOutOfTagsetValueInClosedTagsetProducesException() throws Exception
    {
        final String role = "TAG-NOT-IN-LIST";
        
        CAS cas = jcas.getCas();

        TagSet slotFeatureTagset = new TagSet();
        slotFeatureTagset.setCreateTag(false);
        
        slotFeature.setTagset(slotFeatureTagset);
        
        AnnotationFS hostFS = cas.createAnnotation(hostType, 0, cas.getDocumentText().length());
        AnnotationFS targetFS = cas.createAnnotation(targetType, 0, cas.getDocumentText().length());
        
        when(schemaService.existsTag(role, slotFeatureTagset)).thenReturn(false);
        
        assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> sut.setFeatureValue(jcas, slotFeature, getAddr(hostFS), 
                    asList(new LinkWithRoleModel(role, "dummy", getAddr(targetFS)))))
            .withMessageContaining("is not in the tag list");
    }
    
    @Test
    public void thatUsingOutOfTagsetValueInOpenTagsetAddsNewValue() throws Exception
    {
        final String role = "TAG-NOT-IN-LIST";
        
        CAS cas = jcas.getCas();

        TagSet slotFeatureTagset = new TagSet();
        slotFeatureTagset.setCreateTag(true);
        
        slotFeature.setTagset(slotFeatureTagset);
        
        AnnotationFS hostFS = cas.createAnnotation(hostType, 0, cas.getDocumentText().length());
        AnnotationFS targetFS = cas.createAnnotation(targetType, 0, cas.getDocumentText().length());
        
        when(schemaService.existsTag(role, slotFeatureTagset)).thenReturn(false);
        
        sut.setFeatureValue(jcas, slotFeature, getAddr(hostFS),
                asList(new LinkWithRoleModel(role, "dummy", getAddr(targetFS))));
        
        verify(schemaService).createTag(new Tag(slotFeatureTagset, role));
    }
}
