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
import static de.tudarmstadt.ukp.inception.annotation.feature.link.LinkFeatureMultiplicityMode.MULTIPLE_TARGETS_ONE_ROLE;
import static de.tudarmstadt.ukp.inception.annotation.feature.link.LinkFeatureMultiplicityMode.ONE_TARGET_MULTIPLE_ROLES;
import static de.tudarmstadt.ukp.inception.curation.merge.CurationTestUtils.ALT_LINKS_FEATURE;
import static de.tudarmstadt.ukp.inception.curation.merge.CurationTestUtils.HOST_TYPE;
import static de.tudarmstadt.ukp.inception.curation.merge.CurationTestUtils.LINKS_FEATURE;
import static de.tudarmstadt.ukp.inception.curation.merge.CurationTestUtils.TARGET_FEATURE;
import static de.tudarmstadt.ukp.inception.curation.merge.CurationTestUtils.linkTo;
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
import static org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Map;

import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.model.LinkMode;
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import de.tudarmstadt.ukp.inception.annotation.feature.link.LinkFeatureTraits;
import de.tudarmstadt.ukp.inception.curation.merge.strategy.ThresholdBasedMergeStrategy;
import de.tudarmstadt.ukp.inception.schema.api.feature.LinkWithRoleModel;
import de.tudarmstadt.ukp.inception.schema.api.feature.MaterializedLink;

@Execution(CONCURRENT)
public class CasMergeLinkTest
    extends CasMergeTestBase
{
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final String DUMMY_USER = "dummyTargetUser";

    private JCas sourceCas1;
    private JCas sourceCas2;
    private JCas targetCas;

    @Override
    @BeforeEach
    public void setup() throws Exception
    {
        super.setup();
        sourceCas1 = createJCas();
        sourceCas2 = createJCas();
        targetCas = createJCas();
    }

    @Test
    public void thatLinkIsCopiedFromSourceToTarget() throws Exception
    {
        // Set up source CAS
        var role = "slot1";
        var sourceFs = makeLinkHostFS(sourceCas1, 0, 0, makeLinkFS(sourceCas1, role, 0, 0));

        // Set up target CAS
        var target = makeLinkHostFS(targetCas, 0, 0);
        var targetFiller = new NamedEntity(targetCas, 0, 0);
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
        var sourceFs1 = makeLinkHostFS(sourceCas1, 0, 0, makeLinkFS(sourceCas1, role, 0, 0));
        var sourceFs2 = makeLinkHostFS(sourceCas1, 0, 0, makeLinkFS(sourceCas1, role, 1, 1));

        // Set up target CAS
        var target1 = makeLinkHostFS(targetCas, 0, 0);
        var targetFiller1 = new NamedEntity(targetCas, 0, 0);
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
        var targetFiller2 = new NamedEntity(targetCas, 1, 1);
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
        var sourceFs1 = makeLinkHostFS(sourceCas1, 0, 0, makeLinkFS(sourceCas1, role, 0, 0));
        setFeature(sourceFs1, "f1", "foo");
        var sourceFs2 = makeLinkHostFS(sourceCas1, 0, 0, makeLinkFS(sourceCas1, role, 1, 1));
        setFeature(sourceFs2, "f1", "bar");

        // Set up target CAS
        var target1 = makeLinkHostFS(targetCas, 0, 0);
        setFeature(target1, "f1", "foo");
        var targetFiller1 = new NamedEntity(targetCas, 0, 0);
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
        var targetFiller2 = new NamedEntity(targetCas, 1, 1);
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
        var sourceFs = buildAnnotation(sourceCas1, HOST_TYPE) //
                .at(0, 0) //
                .withFeature(LINKS_FEATURE, makeLinkFS(sourceCas1, null, 0, 0))
                .buildAndAddToIndexes();

        // Set up target CAS
        var targetLink = makeLinkFS(targetCas, null, 0, 0);
        var targetFiller = getFeature(targetLink, TARGET_FEATURE, Annotation.class);
        var targetFs = buildAnnotation(targetCas, HOST_TYPE) //
                .at(sourceFs) //
                .withFeature(LINKS_FEATURE, targetLink) //
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
        var sourceFs = buildAnnotation(sourceCas1, HOST_TYPE) //
                .at(0, 0) //
                .withFeature(LINKS_FEATURE, makeLinkFS(sourceCas1, "role1", 0, 0))
                .buildAndAddToIndexes();

        // Set up target CAS
        var targetLink = makeLinkFS(targetCas, "role2", 0, 0);
        var targetFiller = getFeature(targetLink, TARGET_FEATURE, Annotation.class);
        var targetFs = buildAnnotation(targetCas, HOST_TYPE) //
                .at(sourceFs) //
                .withFeature(LINKS_FEATURE, targetLink) //
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
        var sourceFs = buildAnnotation(sourceCas1, HOST_TYPE) //
                .at(0, 0) //
                .withFeature(LINKS_FEATURE, makeLinkFS(sourceCas1, "role1", 0, 0))
                .buildAndAddToIndexes();

        // Set up target CAS
        var targetLink = makeLinkFS(targetCas, "role1", 0, 0);
        var targetFiller = getFeature(targetLink, TARGET_FEATURE, Annotation.class);
        var targetFs = buildAnnotation(targetCas.getCas(), HOST_TYPE) //
                .at(sourceFs) //
                .withFeature(LINKS_FEATURE, targetLink) //
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
    public void thatLinkMergesToRightTargets() throws Exception
    {
        // Source
        var sourceFiller1 = buildAnnotation(sourceCas1, NamedEntity.class) //
                .at(1, 1) //
                .withFeature(NamedEntity._FeatName_value, "foo") //
                .buildAndAddToIndexes();

        buildAnnotation(sourceCas1, NamedEntity.class) //
                .at(1, 1) //
                .withFeature(NamedEntity._FeatName_value, "bar") //
                .buildAndAddToIndexes();

        var sourceFs = buildAnnotation(sourceCas1, HOST_TYPE) //
                .at(0, 0) //
                .withFeature(LINKS_FEATURE, linkTo("role1", sourceFiller1)) //
                .withFeature("f1", "fai") //
                .buildAndAddToIndexes();

        buildAnnotation(sourceCas1, HOST_TYPE) //
                .at(0, 0) //
                .withFeature("f1", "fum") //
                .buildAndAddToIndexes();

        // Target (link not merged yet)
        buildAnnotation(targetCas, NamedEntity.class) //
                .at(1, 1) //
                .withFeature(NamedEntity._FeatName_value, "foo") //
                .buildAndAddToIndexes();

        buildAnnotation(targetCas, NamedEntity.class) //
                .at(1, 1) //
                .withFeature(NamedEntity._FeatName_value, "bar") //
                .buildAndAddToIndexes();

        var targetCandidateFs1 = buildAnnotation(targetCas, HOST_TYPE) //
                .at(0, 0) //
                .withFeature("f1", "fai") //
                .buildAndAddToIndexes();

        var targetCandidateFs2 = buildAnnotation(targetCas, HOST_TYPE) //
                .at(0, 0) //
                .withFeature("f1", "fum") //
                .buildAndAddToIndexes();

        // Perform merge
        sut.mergeSlotFeature(document, DUMMY_USER, slotLayer, targetCas.getCas(), sourceFs,
                LINKS_FEATURE, 0);

        var adapter = schemaService.getAdapter(slotLayer);
        var linksFeature = adapter.getFeature(LINKS_FEATURE).get();

        List<LinkWithRoleModel> mergedLinks2 = adapter.getFeatureValue(linksFeature,
                targetCandidateFs1);
        assertThat(mergedLinks2) //
                .as("Link has been merged to the second candidate").isNotEmpty();

        List<LinkWithRoleModel> mergedLinks1 = adapter.getFeatureValue(linksFeature,
                targetCandidateFs2);
        assertThat(mergedLinks1) //
                .as("Link has NOT been merged to the first candidate").isEmpty();
    }

    @Test
    public void thatLinkIsAttachedToCorrectStackedTargetWhenOtherLinkFeatureDiffers()
        throws Exception
    {
        // Set up source CAS
        var sourceFs = buildAnnotation(sourceCas1, HOST_TYPE) //
                .at(0, 0) //
                .withFeature(LINKS_FEATURE, makeLinkFS(sourceCas1, "role1", 1, 1)) //
                .withFeature(ALT_LINKS_FEATURE, makeLinkFS(sourceCas1, "role1", 2, 2)) //
                .buildAndAddToIndexes();

        // Set up target CAS
        new NamedEntity(targetCas, 2, 2).addToIndexes();
        var targetCandidateFs1 = buildAnnotation(targetCas, HOST_TYPE) //
                .at(sourceFs) //
                .withFeature(LINKS_FEATURE, makeLinkFS(targetCas, "role1", 3, 3)) //
                .buildAndAddToIndexes();
        var targetCandidateFs2 = buildAnnotation(targetCas, HOST_TYPE) //
                .at(sourceFs) //
                .withFeature(LINKS_FEATURE, makeLinkFS(targetCas, "role1", 1, 1)) //
                .buildAndAddToIndexes();

        // Perform merge
        sut.mergeSlotFeature(document, DUMMY_USER, slotLayer, targetCas.getCas(), sourceFs,
                ALT_LINKS_FEATURE, 0);

        var adapter = schemaService.getAdapter(slotLayer);
        var altLinksFeature = adapter.getFeature(ALT_LINKS_FEATURE).get();
        List<LinkWithRoleModel> mergedLinks1 = adapter.getFeatureValue(altLinksFeature,
                targetCandidateFs1);
        assertThat(mergedLinks1) //
                .as("Link has NOT been merged to the first candidate").isEmpty();
        List<LinkWithRoleModel> mergedLinks2 = adapter.getFeatureValue(altLinksFeature,
                targetCandidateFs2);
        assertThat(mergedLinks2) //
                .as("Link has been merged to the second candidate").isNotEmpty();
    }

    @Nested
    class SingleUserThesholdBasedMergeStrategyTests
    {
        @BeforeEach
        void setup() throws Exception
        {
            slotLayer.setOverlapMode(ANY_OVERLAP);

            var traits = new LinkFeatureTraits();
            traits.setDiffMode(INCLUDE);
            slotFeature.setTraits(toJsonString(traits));

            slotHostDiffAdapter.addLinkFeature("links", "role", "target", ONE_TARGET_MULTIPLE_ROLES,
                    INCLUDE);

            sut.setMergeStrategy(ThresholdBasedMergeStrategy.builder() //
                    .withUserThreshold(1) //
                    .withConfidenceThreshold(0) //
                    .withTopRanks(0) //
                    .build());
        }

        @Test
        public void thatStackedLinkHostsWithDifferentTargetsAreMerged() throws Exception
        {
            buildAnnotation(sourceCas1, HOST_TYPE) //
                    .at(0, 0) //
                    .withFeature(LINKS_FEATURE, makeLinkFS(sourceCas1, "role1", 1, 1))
                    .buildAndAddToIndexes();

            buildAnnotation(sourceCas1, HOST_TYPE) //
                    .at(0, 0) //
                    .withFeature(LINKS_FEATURE, makeLinkFS(sourceCas1, "role2", 1, 1))
                    .buildAndAddToIndexes();

            var casMap = Map.of("source", sourceCas1.getCas());
            var diff = doDiff(diffAdapters, casMap).toResult();
            sut.mergeCas(diff, document, DUMMY_USER, targetCas.getCas(), casMap);

            var targetHosts = targetCas.select(HOST_TYPE).asList();
            assertThat(targetHosts) //
                    .as("Links by host in target CAS") //
                    .extracting(host -> toMaterializedLinks(host, LINKS_FEATURE, "role", "target")) //
                    .containsExactlyInAnyOrder( //
                            asList(matLink(LINKS_FEATURE, "role1", NamedEntity.class, 1, 1)), //
                            asList(matLink(LINKS_FEATURE, "role2", NamedEntity.class, 1, 1)));
        }

        @Test
        public void thatMultipleMatchingStackedLinksWithRoleAreMerged() throws Exception
        {
            slotHostDiffAdapter.addLinkFeature("links", "role", "target", MULTIPLE_TARGETS_ONE_ROLE,
                    INCLUDE);

            buildAnnotation(sourceCas1, HOST_TYPE) //
                    .at(0, 0) //
                    .withFeature(LINKS_FEATURE, //
                            makeLinkFS(sourceCas1, "role1", 1, 1), //
                            makeLinkFS(sourceCas1, "role1", 2, 2), //
                            makeLinkFS(sourceCas1, "role1", 3, 3))
                    .buildAndAddToIndexes();

            buildAnnotation(sourceCas1, HOST_TYPE) //
                    .at(0, 0) //
                    .withFeature(LINKS_FEATURE, makeLinkFS(sourceCas1, "role1", 1, 1))
                    .buildAndAddToIndexes();

            buildAnnotation(sourceCas1, HOST_TYPE) //
                    .at(0, 0) //
                    .withFeature(LINKS_FEATURE, //
                            makeLinkFS(sourceCas1, "role1", 1, 1), //
                            makeLinkFS(sourceCas1, "role1", 2, 2))
                    .buildAndAddToIndexes();

            buildAnnotation(sourceCas1, HOST_TYPE) //
                    .at(0, 0) //
                    .withFeature(LINKS_FEATURE, //
                            makeLinkFS(sourceCas1, "role1", 1, 1), //
                            makeLinkFS(sourceCas1, "role1", 3, 3))
                    .buildAndAddToIndexes();

            var casMap = Map.of("source1", sourceCas1.getCas());
            var diff = doDiff(diffAdapters, casMap).toResult();
            sut.mergeCas(diff, document, DUMMY_USER, targetCas.getCas(), casMap);

            assertThat(targetCas.select(HOST_TYPE).asList()) //
                    .as("Links by host in target CAS") //
                    .extracting(host -> toMaterializedLinks(host, LINKS_FEATURE, "role", "target")) //
                    .containsExactlyInAnyOrder( //
                            asList( //
                                    matLink(LINKS_FEATURE, "role1", NamedEntity.class, 1, 1)),
                            asList( //
                                    matLink(LINKS_FEATURE, "role1", NamedEntity.class, 2, 2), //
                                    matLink(LINKS_FEATURE, "role1", NamedEntity.class, 1, 1)), //
                            asList( //
                                    matLink(LINKS_FEATURE, "role1", NamedEntity.class, 3, 3), //
                                    matLink(LINKS_FEATURE, "role1", NamedEntity.class, 1, 1)),
                            asList( //
                                    matLink(LINKS_FEATURE, "role1", NamedEntity.class, 3, 3), //
                                    matLink(LINKS_FEATURE, "role1", NamedEntity.class, 2, 2), //
                                    matLink(LINKS_FEATURE, "role1", NamedEntity.class, 1, 1)));
        }

        @Test
        public void thatMultipleMatchingStackedLinksWithoutRoleAreMerged() throws Exception
        {
            var traits = new LinkFeatureTraits();
            traits.setDiffMode(INCLUDE);
            traits.setEnableRoleLabels(false);
            slotFeature.setTraits(toJsonString(traits));

            slotFeature.setLinkMode(LinkMode.WITH_ROLE);
            slotHostDiffAdapter.addLinkFeature("links", "role", "target",
                    traits.getMultiplicityMode(), INCLUDE);

            buildAnnotation(sourceCas1, HOST_TYPE) //
                    .at(0, 0) //
                    .withFeature(LINKS_FEATURE, //
                            makeLinkFS(sourceCas1, 1, 1), //
                            makeLinkFS(sourceCas1, 2, 2))
                    .buildAndAddToIndexes();

            buildAnnotation(sourceCas1.getCas(), HOST_TYPE) //
                    .at(0, 0) //
                    .withFeature(LINKS_FEATURE, //
                            makeLinkFS(sourceCas1, 1, 1), //
                            makeLinkFS(sourceCas1, 2, 2), //
                            makeLinkFS(sourceCas1, 3, 3))
                    .buildAndAddToIndexes();

            buildAnnotation(sourceCas1, HOST_TYPE) //
                    .at(0, 0) //
                    .withFeature(LINKS_FEATURE, //
                            makeLinkFS(sourceCas1, 1, 1), //
                            makeLinkFS(sourceCas1, 3, 3))
                    .buildAndAddToIndexes();

            buildAnnotation(sourceCas1, HOST_TYPE) //
                    .at(0, 0) //
                    .withFeature(LINKS_FEATURE, makeLinkFS(sourceCas1, null, 1, 1))
                    .buildAndAddToIndexes();

            var casMap = Map.of("source1", sourceCas1.getCas());
            var diff = doDiff(diffAdapters, casMap).toResult();
            sut.mergeCas(diff, document, DUMMY_USER, targetCas.getCas(), casMap);

            assertThat(targetCas.select(HOST_TYPE).asList()) //
                    .as("Links by host in target CAS") //
                    .extracting(host -> toMaterializedLinks(host, LINKS_FEATURE, "role", "target")) //
                    .containsExactlyInAnyOrder( //
                            asList( //
                                    matLink(LINKS_FEATURE, NamedEntity.class, 1, 1)),
                            asList( //
                                    matLink(LINKS_FEATURE, NamedEntity.class, 1, 1), //
                                    matLink(LINKS_FEATURE, NamedEntity.class, 2, 2)), //
                            asList( //
                                    matLink(LINKS_FEATURE, NamedEntity.class, 1, 1), //
                                    matLink(LINKS_FEATURE, NamedEntity.class, 3, 3)),
                            asList( //
                                    matLink(LINKS_FEATURE, NamedEntity.class, 1, 1), //
                                    matLink(LINKS_FEATURE, NamedEntity.class, 2, 2), //
                                    matLink(LINKS_FEATURE, NamedEntity.class, 3, 3)));
        }
    }

    @Nested
    class DualUserThesholdBasedMergeStrategyTests
    {
        @BeforeEach
        void setup() throws Exception
        {
            slotLayer.setOverlapMode(ANY_OVERLAP);

            var traits = new LinkFeatureTraits();
            traits.setDiffMode(INCLUDE);
            slotFeature.setTraits(toJsonString(traits));

            slotHostDiffAdapter.addLinkFeature("links", "role", "target", ONE_TARGET_MULTIPLE_ROLES,
                    INCLUDE);

            sut.setMergeStrategy(ThresholdBasedMergeStrategy.builder() //
                    .withUserThreshold(2) //
                    .withConfidenceThreshold(0) //
                    .withTopRanks(0) //
                    .build());
        }

        @Test
        public void thatMatchingStackedLinksAreMerged() throws Exception
        {
            buildAnnotation(sourceCas1, HOST_TYPE) //
                    .at(0, 0) //
                    .withFeature(LINKS_FEATURE, makeLinkFS(sourceCas1, "role1", 1, 1))
                    .buildAndAddToIndexes();

            buildAnnotation(sourceCas1, HOST_TYPE) //
                    .at(0, 0) //
                    .withFeature(LINKS_FEATURE, makeLinkFS(sourceCas1, "role2", 1, 1))
                    .buildAndAddToIndexes();

            buildAnnotation(sourceCas2, HOST_TYPE) //
                    .at(0, 0) //
                    .withFeature(LINKS_FEATURE, makeLinkFS(sourceCas2, "role1", 1, 1))
                    .buildAndAddToIndexes();

            var casMap = Map.of("source1", sourceCas1.getCas(), "source2", sourceCas2.getCas());
            var diff = doDiff(diffAdapters, casMap).toResult();
            sut.mergeCas(diff, document, DUMMY_USER, targetCas.getCas(), casMap);

            var targetHosts = targetCas.select(HOST_TYPE).asList();
            assertThat(targetHosts) //
                    .as("Links by host in target CAS") //
                    .extracting(host -> toMaterializedLinks(host, LINKS_FEATURE, "role", "target")) //
                    .containsExactlyInAnyOrder( //
                            asList(matLink(LINKS_FEATURE, "role1", NamedEntity.class, 1, 1)));
        }

        @Test
        public void thatMultipleMatchingStackedLinksAreMerged() throws Exception
        {
            slotHostDiffAdapter.addLinkFeature("links", "role", "target", MULTIPLE_TARGETS_ONE_ROLE,
                    INCLUDE);

            buildAnnotation(sourceCas1, HOST_TYPE) //
                    .at(0, 0) //
                    .withFeature(LINKS_FEATURE, //
                            makeLinkFS(sourceCas1, "role1", 1, 1))
                    .buildAndAddToIndexes();

            buildAnnotation(sourceCas1, HOST_TYPE) //
                    .at(0, 0) //
                    .withFeature(LINKS_FEATURE, //
                            makeLinkFS(sourceCas1, "role1", 1, 1), //
                            makeLinkFS(sourceCas1, "role1", 2, 2))
                    .buildAndAddToIndexes();

            buildAnnotation(sourceCas1, HOST_TYPE) //
                    .at(0, 0) //
                    .withFeature(LINKS_FEATURE, //
                            makeLinkFS(sourceCas1, "role1", 1, 1), //
                            makeLinkFS(sourceCas1, "role1", 3, 3))
                    .buildAndAddToIndexes();

            buildAnnotation(sourceCas1, HOST_TYPE) //
                    .at(0, 0) //
                    .withFeature(LINKS_FEATURE, //
                            makeLinkFS(sourceCas1, "role1", 1, 1), //
                            makeLinkFS(sourceCas1, "role1", 2, 2), //
                            makeLinkFS(sourceCas1, "role1", 3, 3))
                    .buildAndAddToIndexes();

            buildAnnotation(sourceCas2, HOST_TYPE) //
                    .at(0, 0) //
                    .withFeature(LINKS_FEATURE, //
                            makeLinkFS(sourceCas2, "role1", 2, 2), //
                            makeLinkFS(sourceCas2, "role1", 3, 3), //
                            makeLinkFS(sourceCas2, "role1", 1, 1))
                    .buildAndAddToIndexes();

            buildAnnotation(sourceCas2, HOST_TYPE) //
                    .at(0, 0) //
                    .withFeature(LINKS_FEATURE, //
                            makeLinkFS(sourceCas2, "role1", 1, 1))
                    .buildAndAddToIndexes();

            buildAnnotation(sourceCas2, HOST_TYPE) //
                    .at(0, 0) //
                    .withFeature(LINKS_FEATURE, //
                            makeLinkFS(sourceCas2, "role1", 2, 2), //
                            makeLinkFS(sourceCas2, "role1", 1, 1))
                    .buildAndAddToIndexes();

            buildAnnotation(sourceCas2, HOST_TYPE) //
                    .at(0, 0) //
                    .withFeature(LINKS_FEATURE, //
                            makeLinkFS(sourceCas2, "role1", 1, 1), //
                            makeLinkFS(sourceCas2, "role1", 3, 3))
                    .buildAndAddToIndexes();

            var casMap = Map.of("source1", sourceCas1.getCas(), "source2", sourceCas2.getCas());
            var diff = doDiff(diffAdapters, casMap).toResult();
            sut.mergeCas(diff, document, DUMMY_USER, targetCas.getCas(), casMap);

            assertThat(targetCas.select(HOST_TYPE).asList()) //
                    .as("Links by host in target CAS") //
                    .extracting(host -> toMaterializedLinks(host, LINKS_FEATURE, "role", "target")) //
                    .containsExactlyInAnyOrder( //
                            asList( //
                                    matLink(LINKS_FEATURE, "role1", NamedEntity.class, 1, 1)),
                            asList( //
                                    matLink(LINKS_FEATURE, "role1", NamedEntity.class, 2, 2), //
                                    matLink(LINKS_FEATURE, "role1", NamedEntity.class, 1, 1)), //
                            asList( //
                                    matLink(LINKS_FEATURE, "role1", NamedEntity.class, 3, 3), //
                                    matLink(LINKS_FEATURE, "role1", NamedEntity.class, 1, 1)),
                            asList( //
                                    matLink(LINKS_FEATURE, "role1", NamedEntity.class, 3, 3), //
                                    matLink(LINKS_FEATURE, "role1", NamedEntity.class, 2, 2), //
                                    matLink(LINKS_FEATURE, "role1", NamedEntity.class, 1, 1)));
        }
    }

    MaterializedLink matLink(String feature, Class<?> targetType, int targetBegin, int targetEnd)
    {
        return new MaterializedLink(feature, null, targetType.getName(), targetBegin, targetEnd);
    }

    MaterializedLink matLink(String feature, String role, Class<?> targetType, int targetBegin,
            int targetEnd)
    {
        return new MaterializedLink(feature, role, targetType.getName(), targetBegin, targetEnd);
    }
}
