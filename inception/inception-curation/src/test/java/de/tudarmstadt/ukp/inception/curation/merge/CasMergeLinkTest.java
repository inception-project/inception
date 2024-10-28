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
import static de.tudarmstadt.ukp.clarin.webanno.model.OverlapMode.ANY_OVERLAP;
import static de.tudarmstadt.ukp.inception.annotation.feature.link.LinkFeatureDiffMode.INCLUDE;
import static de.tudarmstadt.ukp.inception.annotation.feature.link.LinkFeatureMultiplicityMode.ONE_TARGET_MULTIPLE_ROLES;
import static de.tudarmstadt.ukp.inception.curation.merge.CurationTestUtils.HOST_TYPE;
import static de.tudarmstadt.ukp.inception.curation.merge.CurationTestUtils.LINKS_FEATURE;
import static de.tudarmstadt.ukp.inception.curation.merge.CurationTestUtils.TARGET_FEATURE;
import static de.tudarmstadt.ukp.inception.curation.merge.CurationTestUtils.makeLinkFS;
import static de.tudarmstadt.ukp.inception.curation.merge.CurationTestUtils.makeLinkHostFS;
import static de.tudarmstadt.ukp.inception.schema.api.feature.MaterializedLink.toMaterializedLinks;
import static de.tudarmstadt.ukp.inception.support.json.JSONUtil.toJsonString;
import static de.tudarmstadt.ukp.inception.support.uima.AnnotationBuilder.buildAnnotation;
import static java.util.Arrays.asList;
import static org.apache.uima.fit.factory.JCasFactory.createJCas;
import static org.apache.uima.fit.util.FSUtil.getFeature;
import static org.apache.uima.fit.util.FSUtil.setFeature;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Map;

import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.inception.annotation.feature.link.LinkFeatureTraits;
import de.tudarmstadt.ukp.inception.curation.merge.strategy.ThresholdBasedMergeStrategy;
import de.tudarmstadt.ukp.inception.schema.api.feature.LinkWithRoleModel;
import de.tudarmstadt.ukp.inception.schema.api.feature.MaterializedLink;

public class CasMergeLinkTest
    extends CasMergeTestBase
{
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final String DUMMY_USER = "dummyTargetUser";

    private JCas sourceCas;
    private JCas targetCas;

    @Override
    @BeforeEach
    public void setup() throws Exception
    {
        super.setup();
        sourceCas = createJCas();
        targetCas = createJCas();
    }

    @Test
    public void thatLinkIsCopiedFromSourceToTarget() throws Exception
    {
        // Set up source CAS
        var role = "slot1";
        var sourceFs = makeLinkHostFS(sourceCas, 0, 0, makeLinkFS(sourceCas, role, 0, 0));

        // Set up target CAS
        var target = makeLinkHostFS(targetCas, 0, 0);
        var targetFiller = new Token(targetCas, 0, 0);
        targetFiller.addToIndexes();

        // Perform merge
        sut.mergeSlotFeature(document, DUMMY_USER, slotLayer, targetCas.getCas(), sourceFs,
                slotFeature.getName(), 0);

        var adapter = schemaService.getAdapter(slotLayer);
        List<LinkWithRoleModel> mergedLinks = adapter.getFeatureValue(slotFeature, target);
        assertThat(mergedLinks) //
                .as("Link has been copied from source to target")
                .containsExactly(new LinkWithRoleModel(role, null, targetFiller.getAddress()));
    }

    @Test
    public void thatLinkIsAttachedToCorrectStackedTargetWithoutLabel() throws Exception
    {
        var adapter = schemaService.getAdapter(slotLayer);

        // Set up source CAS
        var role = "slot1";
        var sourceFs1 = makeLinkHostFS(sourceCas, 0, 0, makeLinkFS(sourceCas, role, 0, 0));
        var sourceFs2 = makeLinkHostFS(sourceCas, 0, 0, makeLinkFS(sourceCas, role, 1, 1));

        // Set up target CAS
        var target1 = makeLinkHostFS(targetCas, 0, 0);
        var targetFiller1 = new Token(targetCas, 0, 0);
        targetFiller1.addToIndexes();

        // Perform merge
        LOG.trace("Merge 1");
        sut.mergeSlotFeature(document, DUMMY_USER, slotLayer, targetCas.getCas(), sourceFs1,
                slotFeature.getName(), 0);

        List<LinkWithRoleModel> mergedLinks1 = adapter.getFeatureValue(slotFeature, target1);
        assertThat(mergedLinks1) //
                .as("Link has been copied from source to target 1")
                .containsExactly(new LinkWithRoleModel(role, null, targetFiller1.getAddress()));

        // Add stacked target to target CAS
        var target2 = makeLinkHostFS(targetCas, 0, 0);
        var targetFiller2 = new Token(targetCas, 1, 1);
        targetFiller2.addToIndexes();

        // Perform another merge
        LOG.trace("Merge 2");
        sut.mergeSlotFeature(document, DUMMY_USER, slotLayer, targetCas.getCas(), sourceFs2,
                slotFeature.getName(), 0);

        List<LinkWithRoleModel> mergedLinks2 = adapter.getFeatureValue(slotFeature, target2);
        assertThat(mergedLinks2) //
                .as("Link has been copied from source to target 2")
                .containsExactly(new LinkWithRoleModel(role, null, targetFiller2.getAddress()));
    }

    @Test
    public void thatLinkIsAttachedToCorrectStackedTargetWithLabel() throws Exception
    {
        var adapter = schemaService.getAdapter(slotLayer);

        // Set up source CAS
        var role = "slot1";
        var sourceFs1 = makeLinkHostFS(sourceCas, 0, 0, makeLinkFS(sourceCas, role, 0, 0));
        setFeature(sourceFs1, "f1", "foo");
        var sourceFs2 = makeLinkHostFS(sourceCas, 0, 0, makeLinkFS(sourceCas, role, 1, 1));
        setFeature(sourceFs2, "f1", "bar");

        // Set up target CAS
        var target1 = makeLinkHostFS(targetCas, 0, 0);
        setFeature(target1, "f1", "foo");
        var targetFiller1 = new Token(targetCas, 0, 0);
        targetFiller1.addToIndexes();

        // Perform merge
        LOG.trace("Merge 1");
        sut.mergeSlotFeature(document, DUMMY_USER, slotLayer, targetCas.getCas(), sourceFs1,
                slotFeature.getName(), 0);

        List<LinkWithRoleModel> mergedLinks1 = adapter.getFeatureValue(slotFeature, target1);
        assertThat(mergedLinks1) //
                .as("Link has been copied from source to target 1")
                .containsExactly(new LinkWithRoleModel(role, null, targetFiller1.getAddress()));

        // Add stacked target to target CAS
        var target2 = makeLinkHostFS(targetCas, 0, 0);
        setFeature(target2, "f1", "bar");
        var targetFiller2 = new Token(targetCas, 1, 1);
        targetFiller2.addToIndexes();

        // Perform another merge
        LOG.trace("Merge 2");
        sut.mergeSlotFeature(document, DUMMY_USER, slotLayer, targetCas.getCas(), sourceFs2,
                slotFeature.getName(), 0);

        List<LinkWithRoleModel> mergedLinks2 = adapter.getFeatureValue(slotFeature, target2);
        assertThat(mergedLinks2) //
                .as("Link has been copied from source to target 2")
                .containsExactly(new LinkWithRoleModel(role, null, targetFiller2.getAddress()));
    }

    @Test
    public void thatSecondLinkWithSameTargetIsRejectedWhenRolesAreDisabled() throws Exception
    {
        var traits = new LinkFeatureTraits();
        traits.setEnableRoleLabels(false);
        slotFeature.setTraits(toJsonString(traits));

        // Set up source CAS
        var sourceFs = buildAnnotation(sourceCas.getCas(), HOST_TYPE) //
                .at(0, 0) //
                .withFeature(LINKS_FEATURE, asList(makeLinkFS(sourceCas, null, 0, 0)))
                .buildAndAddToIndexes();

        // Set up target CAS
        var targetLink = makeLinkFS(targetCas, null, 0, 0);
        var targetFiller = getFeature(targetLink, TARGET_FEATURE, Annotation.class);
        var targetFs = buildAnnotation(targetCas.getCas(), HOST_TYPE) //
                .at(sourceFs) //
                .withFeature(LINKS_FEATURE, asList(targetLink)) //
                .buildAndAddToIndexes();

        // Perform merge
        assertThatExceptionOfType(AlreadyMergedException.class)
                .isThrownBy(() -> sut.mergeSlotFeature(document, DUMMY_USER, slotLayer,
                        targetCas.getCas(), sourceFs, slotFeature.getName(), 0));

        var adapter = schemaService.getAdapter(slotLayer);
        List<LinkWithRoleModel> mergedLinks = adapter.getFeatureValue(slotFeature, targetFs);
        assertThat(mergedLinks) //
                .as("There is still only a single link from the host to the filler")
                .containsExactly(new LinkWithRoleModel(null, null, targetFiller.getAddress()));
    }

    @Test
    public void thatSecondLinkWithSameTargetButDifferentRoleIsAddedWhenRolesAreEnabled()
        throws Exception
    {
        // Set up source CAS
        var sourceFs = buildAnnotation(sourceCas.getCas(), HOST_TYPE) //
                .at(0, 0) //
                .withFeature(LINKS_FEATURE, asList(makeLinkFS(sourceCas, "role1", 0, 0)))
                .buildAndAddToIndexes();

        // Set up target CAS
        var targetLink = makeLinkFS(targetCas, "role2", 0, 0);
        var targetFiller = getFeature(targetLink, TARGET_FEATURE, Annotation.class);
        var targetFs = buildAnnotation(targetCas.getCas(), HOST_TYPE) //
                .at(sourceFs) //
                .withFeature(LINKS_FEATURE, asList(targetLink)) //
                .buildAndAddToIndexes();

        // Perform merge
        sut.mergeSlotFeature(document, DUMMY_USER, slotLayer, targetCas.getCas(), sourceFs,
                slotFeature.getName(), 0);

        var adapter = schemaService.getAdapter(slotLayer);
        List<LinkWithRoleModel> mergedLinks = adapter.getFeatureValue(slotFeature, targetFs);
        assertThat(mergedLinks) //
                .as("There is still only a single link from the host to the filler")
                .containsExactly( //
                        new LinkWithRoleModel("role1", null, targetFiller.getAddress()),
                        new LinkWithRoleModel("role2", null, targetFiller.getAddress()));
    }

    @Test
    public void thatSecondLinkWithSameTargetAndSameRoleIsRejectedWhenRolesAreEnabled()
        throws Exception
    {
        // Set up source CAS
        var sourceFs = buildAnnotation(sourceCas.getCas(), HOST_TYPE) //
                .at(0, 0) //
                .withFeature(LINKS_FEATURE, asList(makeLinkFS(sourceCas, "role1", 0, 0)))
                .buildAndAddToIndexes();

        // Set up target CAS
        var targetLink = makeLinkFS(targetCas, "role1", 0, 0);
        var targetFiller = getFeature(targetLink, TARGET_FEATURE, Annotation.class);
        var targetFs = buildAnnotation(targetCas.getCas(), HOST_TYPE) //
                .at(sourceFs) //
                .withFeature(LINKS_FEATURE, asList(targetLink)) //
                .buildAndAddToIndexes();

        // Perform merge
        assertThatExceptionOfType(AlreadyMergedException.class)
                .isThrownBy(() -> sut.mergeSlotFeature(document, DUMMY_USER, slotLayer,
                        targetCas.getCas(), sourceFs, slotFeature.getName(), 0));

        var adapter = schemaService.getAdapter(slotLayer);
        List<LinkWithRoleModel> mergedLinks = adapter.getFeatureValue(slotFeature, targetFs);
        assertThat(mergedLinks) //
                .as("There is still only a single link from the host to the filler")
                .containsExactly( //
                        new LinkWithRoleModel("role1", null, targetFiller.getAddress()));
    }

    @Test
    public void thatStackedLinkHostsWithDifferentTargetsAreMerged() throws Exception
    {
        slotLayer.setOverlapMode(ANY_OVERLAP);
        var traits = new LinkFeatureTraits();
        traits.setDiffMode(INCLUDE);
        slotHostDiffAdapter.addLinkFeature("links", "role", "target", ONE_TARGET_MULTIPLE_ROLES,
                INCLUDE);

        slotFeature.setTraits(toJsonString(traits));
        sut.setMergeStrategy(ThresholdBasedMergeStrategy.builder() //
                .withUserThreshold(1) //
                .withConfidenceThreshold(0) //
                .withTopRanks(Integer.MAX_VALUE) //
                .build());

        // Set up source CAS
        buildAnnotation(sourceCas.getCas(), HOST_TYPE) //
                .at(0, 0) //
                .withFeature(LINKS_FEATURE, asList(makeLinkFS(sourceCas, "role1", 1, 1)))
                .buildAndAddToIndexes();

        buildAnnotation(sourceCas.getCas(), HOST_TYPE) //
                .at(0, 0) //
                .withFeature(LINKS_FEATURE, asList(makeLinkFS(sourceCas, "role2", 1, 1)))
                .buildAndAddToIndexes();

        var casMap = Map.of("source", sourceCas.getCas());

        var diff = doDiff(diffAdapters, casMap).toResult();
        diff.print(System.out);

        sut.mergeCas(diff, document, DUMMY_USER, targetCas.getCas(), casMap);

        var targetHosts = targetCas.select(HOST_TYPE).asList();
        assertThat(targetHosts) //
                .as("Links by host in target CAS") //
                .hasSize(2) //
                .extracting(host -> toMaterializedLinks(host, LINKS_FEATURE, "role", "target")) //
                .containsExactlyInAnyOrder( //
                        asList(new MaterializedLink(LINKS_FEATURE, "role1", Token._TypeName, 1, 1)), //
                        asList(new MaterializedLink(LINKS_FEATURE, "role2", Token._TypeName, 1,
                                1)));
    }
}
