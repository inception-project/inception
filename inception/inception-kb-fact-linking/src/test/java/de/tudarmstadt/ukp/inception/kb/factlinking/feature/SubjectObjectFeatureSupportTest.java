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
package de.tudarmstadt.ukp.inception.kb.factlinking.feature;

import static org.apache.uima.fit.factory.JCasFactory.createJCasFromPath;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;

import org.apache.uima.cas.ArrayFS;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.util.FSUtil;
import org.junit.jupiter.api.Test;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.LinkMode;
import de.tudarmstadt.ukp.clarin.webanno.model.MultiValueMode;
import de.tudarmstadt.ukp.inception.schema.feature.LinkWithRoleModel;

@Deprecated
public class SubjectObjectFeatureSupportTest
{
    @Test
    public void testAccepts()
    {
        SubjectObjectFeatureSupport sut = new SubjectObjectFeatureSupport();

        AnnotationFeature feat1 = new AnnotationFeature("slot", "webanno.custom.SimpleSpan");
        feat1.setLinkTypeName(FactLinkingConstants.SUBJECT_LINK);
        feat1.setLinkMode(LinkMode.WITH_ROLE);
        feat1.setLinkTypeRoleFeatureName("role");
        feat1.setLinkTypeTargetFeatureName("target");
        feat1.setMode(MultiValueMode.ARRAY);

        AnnotationFeature feat2 = new AnnotationFeature("Dummy feature", "someType");

        assertThat(sut.accepts(feat1)).isTrue();
        assertThat(sut.accepts(feat2)).isFalse();
    }

    @Test
    public void testWrapUnwrap() throws Exception
    {
        CAS cas = createJCasFromPath("src/test/resources/desc/type/webannoTestTypes.xml").getCas();

        SubjectObjectFeatureSupport sut = new SubjectObjectFeatureSupport();

        var feat1 = new AnnotationFeature("slot", "webanno.custom.SimpleSpan");
        feat1.setLinkTypeName("webanno.custom.LinkType");
        feat1.setLinkMode(LinkMode.WITH_ROLE);
        feat1.setLinkTypeRoleFeatureName("role");
        feat1.setLinkTypeTargetFeatureName("target");
        feat1.setMode(MultiValueMode.ARRAY);

        List<LinkWithRoleModel> links = new ArrayList<>();
        links.add(new LinkWithRoleModel("role", "label", 3));

        cas.setDocumentText("label");
        Type targetType = cas.getTypeSystem().getType(feat1.getType());
        Type linkType = cas.getTypeSystem().getType(feat1.getLinkTypeName());

        AnnotationFS targetFS = cas.createAnnotation(targetType, 0, cas.getDocumentText().length());

        @SuppressWarnings("unchecked")
        ArrayFS<FeatureStructure> array = cas.createArrayFS(1);
        FeatureStructure linkFS = cas.createFS(linkType);
        FSUtil.setFeature(linkFS, feat1.getLinkTypeRoleFeatureName(), "role");
        FSUtil.setFeature(linkFS, feat1.getLinkTypeTargetFeatureName(), targetFS);
        array.set(0, linkFS);

        assertThat(sut.wrapFeatureValue(feat1, cas, array)).isEqualTo(links);
        assertThat(sut.wrapFeatureValue(feat1, cas, null)).isEmpty();
        assertThatThrownBy(() -> sut.wrapFeatureValue(feat1, cas, new Object()))
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(sut.unwrapFeatureValue(feat1, cas, links)).isSameAs(links);
        assertThat(sut.unwrapFeatureValue(feat1, cas, null)).isNull();
        assertThatThrownBy(() -> sut.unwrapFeatureValue(feat1, cas, new Object()))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
