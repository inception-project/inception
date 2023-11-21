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

import static de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiff.doDiff;
import static de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.LinkCompareBehavior.LINK_TARGET_AS_LABEL;
import static de.tudarmstadt.ukp.inception.curation.merge.CurationTestUtils.HOST_TYPE;
import static de.tudarmstadt.ukp.inception.curation.merge.CurationTestUtils.createMultiLinkWithRoleTestTypeSystem;
import static de.tudarmstadt.ukp.inception.curation.merge.CurationTestUtils.makeLinkFS;
import static de.tudarmstadt.ukp.inception.curation.merge.CurationTestUtils.makeLinkHostFS;
import static de.tudarmstadt.ukp.inception.schema.api.feature.FeatureUtil.setLinkFeatureValue;
import static java.util.Arrays.asList;
import static org.apache.uima.fit.factory.JCasFactory.createJCas;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.jcas.JCas;
import org.junit.jupiter.api.Test;

import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiff.DiffResult;

public class CasDiffLinkFeaturesTest
    extends CasMergeTestBase
{
    @Test
    public void copyLinkToEmptyTest() throws Exception
    {
        // Set up target CAS
        JCas targetCas = createJCas(createMultiLinkWithRoleTestTypeSystem("f1"));
        Type type = targetCas.getTypeSystem().getType(HOST_TYPE);
        Feature feature = type.getFeatureByBaseName("f1");
        AnnotationFS mergeFs = makeLinkHostFS(targetCas, 0, 0, feature, "A");
        FeatureStructure linkFs = makeLinkFS(targetCas, "slot1", 0, 0);
        setLinkFeatureValue(mergeFs, type.getFeatureByBaseName("links"), asList(linkFs));

        // Set up source CAS
        JCas sourceCas = createJCas(createMultiLinkWithRoleTestTypeSystem("f1"));
        makeLinkHostFS(sourceCas, 0, 0, feature, "A", makeLinkFS(sourceCas, "slot1", 0, 0));

        // Perform diff
        Map<String, List<CAS>> casByUser = new LinkedHashMap<>();
        casByUser.put("user1", asList(targetCas.getCas()));
        casByUser.put("user2", asList(sourceCas.getCas()));
        DiffResult diff = doDiff(diffAdapters, LINK_TARGET_AS_LABEL, casByUser).toResult();

        assertThat(diff.getDifferingConfigurationSets()).isEmpty();
        assertThat(diff.getIncompleteConfigurationSets()).isEmpty();
    }

    @Test
    public void copyLinkToExistingButDiffLinkTest() throws Exception
    {
        // Set up target CAS
        JCas targetCas = createJCas(createMultiLinkWithRoleTestTypeSystem("f1"));
        Type type = targetCas.getTypeSystem().getType(HOST_TYPE);
        Feature feature = type.getFeatureByBaseName("f1");
        AnnotationFS mergeFs = makeLinkHostFS(targetCas, 0, 0, feature, "A",
                makeLinkFS(targetCas, "slot1", 0, 0));
        FeatureStructure linkFs = makeLinkFS(targetCas, "slot2", 0, 0);
        setLinkFeatureValue(mergeFs, type.getFeatureByBaseName("links"), asList(linkFs));

        // Set up source CAS
        JCas sourceCas = createJCas(createMultiLinkWithRoleTestTypeSystem("f1"));
        makeLinkHostFS(sourceCas, 0, 0, feature, "A", makeLinkFS(sourceCas, "slot1", 0, 0));

        // Perform diff
        Map<String, List<CAS>> casByUser = new LinkedHashMap<>();
        casByUser.put("user1", asList(targetCas.getCas()));
        casByUser.put("user2", asList(sourceCas.getCas()));
        DiffResult diff = doDiff(diffAdapters, LINK_TARGET_AS_LABEL, casByUser).toResult();

        assertThat(diff.getDifferingConfigurationSets()).isEmpty();
        assertThat(diff.getIncompleteConfigurationSets()).hasSize(2);
    }
}
