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
package de.tudarmstadt.ukp.inception.diam.editing;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.apache.uima.cas.CAS.TYPE_NAME_ANNOTATION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.factory.CasFactory;
import org.apache.uima.resource.metadata.impl.TypeSystemDescription_impl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.annotation.layer.span.api.SpanAdapter;
import de.tudarmstadt.ukp.inception.editor.state.AnnotatorStateImpl;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotationException;
import de.tudarmstadt.ukp.inception.rendering.editorstate.FeatureState;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VID;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.schema.api.feature.LinkWithRoleModel;
import de.tudarmstadt.ukp.inception.support.logging.LogMessage;
import de.tudarmstadt.ukp.inception.support.uima.ICasUtil;

/**
 * Tests the slot cleanup performed while deleting an annotation, and in particular the
 * {@link LogMessage}s it accumulates.
 * <p>
 * The messages are the interesting part here: they used to be emitted inline during the recursion
 * and are now collected into a list and reported once at the end. Asserting on the returned list is
 * exact in a way that looking at the feedback panel is not - two cleared slots that produce
 * identical message text are indistinguishable by eye but trivially countable here.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class AnnotationEditingServiceImplSlotCleanupTest
{
    private static final String HOST_TYPE = "webanno.custom.SlotHost";

    private @Mock AnnotationSchemaService schemaService;
    private @Mock SpanAdapter targetAdapter;

    private AnnotationEditingServiceImpl sut;

    private CAS cas;
    private MockAnnotatorState state;
    private Project project;
    private SourceDocument document;
    private AnnotationLayer targetLayer;
    private AnnotationLayer hostLayer;

    private List<LinkWithRoleModel> hostOneLinks;
    private List<LinkWithRoleModel> hostTwoLinks;

    private AnnotationFS target;
    private AnnotationFS hostOne;
    private AnnotationFS hostTwo;

    private AnnotationFeature linkFeature;

    @BeforeEach
    void setup() throws Exception
    {
        project = new Project("test");
        project.setId(1l);
        document = new SourceDocument("doc.txt", project, "text");
        document.setId(1l);

        targetLayer = new AnnotationLayer();
        targetLayer.setId(1l);
        targetLayer.setName(TYPE_NAME_ANNOTATION);
        targetLayer.setUiName("Target");
        targetLayer.setProject(project);

        hostLayer = new AnnotationLayer();
        hostLayer.setId(2l);
        hostLayer.setName(HOST_TYPE);
        hostLayer.setUiName("Host");
        hostLayer.setProject(project);

        linkFeature = new AnnotationFeature();
        linkFeature.setId(1l);
        linkFeature.setName("slots");
        linkFeature.setUiName("Slots");
        linkFeature.setLayer(hostLayer);

        var tsd = new TypeSystemDescription_impl();
        tsd.addType(HOST_TYPE, "", TYPE_NAME_ANNOTATION);
        cas = CasFactory.createCas(tsd);
        cas.setDocumentText("one two three four");

        state = new MockAnnotatorState(project, document, "annotator");

        sut = new AnnotationEditingServiceImpl(schemaService);

        when(schemaService.listAttachedSpanFeatures(any())).thenReturn(emptyList());
        when(schemaService.getAttachedRels(any(), any())).thenReturn(emptyList());
        when(schemaService.listAttachedLinkFeatures(hostLayer)).thenReturn(asList(linkFeature));
        when(schemaService.listAttachedLinkFeatures(targetLayer)).thenReturn(asList(linkFeature));

        when(targetAdapter.getLayer()).thenReturn(targetLayer);
        when(targetAdapter.getFeatureValue(eq(linkFeature), any())).thenAnswer(call -> {
            var fs = call.getArgument(1, FeatureStructure.class);
            return linksOf(ICasUtil.getAddr(fs));
        });
    }

    private List<LinkWithRoleModel> linksOf(int aAddr)
    {
        if (hostOne != null && aAddr == ICasUtil.getAddr(hostOne)) {
            return hostOneLinks;
        }
        if (hostTwo != null && aAddr == ICasUtil.getAddr(hostTwo)) {
            return hostTwoLinks;
        }
        return new ArrayList<>();
    }

    /**
     * Creates the target annotation plus the given number of hosts, all of type Annotation so that
     * the CAS-wide scan in the service finds them.
     */
    private void givenTargetAndHosts(int aHostCount) throws Exception
    {
        var annotationType = cas.getTypeSystem().getType(TYPE_NAME_ANNOTATION);
        var hostType = cas.getTypeSystem().getType(HOST_TYPE);

        target = cas.createAnnotation(annotationType, 0, 3);
        cas.addFsToIndexes(target);

        if (aHostCount >= 1) {
            hostOne = cas.createAnnotation(hostType, 4, 7);
            cas.addFsToIndexes(hostOne);
            hostOneLinks = new ArrayList<>();
        }

        if (aHostCount >= 2) {
            hostTwo = cas.createAnnotation(hostType, 8, 13);
            cas.addFsToIndexes(hostTwo);
            hostTwoLinks = new ArrayList<>();
        }
    }

    private LinkWithRoleModel link(String aRole, AnnotationFS aTarget)
    {
        var link = new LinkWithRoleModel();
        link.role = aRole;
        link.targetAddr = ICasUtil.getAddr(aTarget);
        link.label = aTarget.getCoveredText();
        return link;
    }

    @Test
    void thatClearingASingleSlotYieldsOneMessage() throws Exception
    {
        givenTargetAndHosts(1);
        hostOneLinks.add(link("agent", target));

        var messages = sut.deleteAnnotation(cas, state, VID.of(target), targetLayer, targetAdapter);

        assertThat(messages).hasSize(1);
        assertThat(messages.get(0).getMessage())
                .isEqualTo("Cleared slot [agent] in feature [Slots] on [Host]");
        assertThat(messages.get(0).getLevel().toString()).isEqualTo("INFO");
        assertThat(hostOneLinks).isEmpty();
    }

    /**
     * The accumulation case: two slots on two different hosts, with distinct roles, both pointing
     * at the deleted annotation.
     */
    @Test
    void thatClearingSlotsOnTwoHostsYieldsOneMessageEach() throws Exception
    {
        givenTargetAndHosts(2);
        hostOneLinks.add(link("agent", target));
        hostTwoLinks.add(link("patient", target));

        var messages = sut.deleteAnnotation(cas, state, VID.of(target), targetLayer, targetAdapter);

        assertThat(messages).extracting(LogMessage::getMessage).containsExactlyInAnyOrder(
                "Cleared slot [agent] in feature [Slots] on [Host]",
                "Cleared slot [patient] in feature [Slots] on [Host]");
        assertThat(hostOneLinks).isEmpty();
        assertThat(hostTwoLinks).isEmpty();
    }

    /**
     * The case that cannot be checked by eye in the UI: two cleared slots whose message text is
     * identical. Exactly two messages must come back - not one deduplicated, not three.
     */
    @Test
    void thatTwoIdenticallyNamedClearedSlotsStillYieldTwoMessages() throws Exception
    {
        givenTargetAndHosts(2);
        hostOneLinks.add(link("agent", target));
        hostTwoLinks.add(link("agent", target));

        var messages = sut.deleteAnnotation(cas, state, VID.of(target), targetLayer, targetAdapter);

        assertThat(messages).hasSize(2);
        assertThat(messages).extracting(LogMessage::getMessage)
                .containsOnly("Cleared slot [agent] in feature [Slots] on [Host]");
    }

    /**
     * Two slots on the <b>same</b> host - exercises accumulation inside the inner loop, where the
     * link list is written back once but two messages must still be recorded.
     */
    @Test
    void thatTwoSlotsOnTheSameHostYieldTwoMessages() throws Exception
    {
        givenTargetAndHosts(1);
        hostOneLinks.add(link("agent", target));
        hostOneLinks.add(link("patient", target));

        var messages = sut.deleteAnnotation(cas, state, VID.of(target), targetLayer, targetAdapter);

        assertThat(messages).extracting(LogMessage::getMessage).containsExactlyInAnyOrder(
                "Cleared slot [agent] in feature [Slots] on [Host]",
                "Cleared slot [patient] in feature [Slots] on [Host]");
        assertThat(hostOneLinks).isEmpty();
    }

    /**
     * Slots pointing at other annotations must survive, and must not produce messages.
     */
    @Test
    void thatUnrelatedSlotsAreLeftAloneAndProduceNoMessages() throws Exception
    {
        givenTargetAndHosts(2);
        var unrelated = link("agent", hostTwo);
        hostOneLinks.add(unrelated);

        var messages = sut.deleteAnnotation(cas, state, VID.of(target), targetLayer, targetAdapter);

        assertThat(messages).isEmpty();
        assertThat(hostOneLinks).containsExactly(unrelated);
        verify(targetAdapter, never()).setFeatureValue(any(), any(), any(), anyInt(),
                eq(linkFeature), any());
    }

    /**
     * When writing the cleaned link list back fails, the service records an error message and
     * carries on.
     */
    @Test
    void thatAFailingWriteBackIsReportedAsAnError() throws Exception
    {
        givenTargetAndHosts(1);
        hostOneLinks.add(link("agent", target));

        doThrow(new AnnotationException("nope")).when(targetAdapter).setFeatureValue(any(), any(),
                any(), anyInt(), eq(linkFeature), any());

        var messages = sut.deleteAnnotation(cas, state, VID.of(target), targetLayer, targetAdapter);

        assertThat(messages).extracting(LogMessage::getMessage).contains(
                "Cleared slot [agent] in feature [Slots] on [Host]",
                "Unable to clean slots in feature [Slots] on [Host]");
    }

    /**
     * An armed slot on the host whose link is being cleared must be disarmed, so the next click
     * does not land in a slot the user no longer sees.
     */
    @Test
    void thatAnArmedSlotOnTheAffectedHostIsDisarmed() throws Exception
    {
        givenTargetAndHosts(1);
        hostOneLinks.add(link("agent", target));

        var armed = new FeatureState(VID.of(hostOne), linkFeature, null);
        state.armedFeature = armed;

        sut.deleteAnnotation(cas, state, VID.of(target), targetLayer, targetAdapter);

        assertThat(state.armedFeature).as("armed slot on the modified host was cleared").isNull();
    }

    /**
     * An armed slot on an unrelated annotation must be left armed.
     */
    @Test
    void thatAnArmedSlotOnAnotherAnnotationSurvives() throws Exception
    {
        givenTargetAndHosts(2);
        hostOneLinks.add(link("agent", target));

        var armed = new FeatureState(VID.of(hostTwo), linkFeature, null);
        state.armedFeature = armed;

        sut.deleteAnnotation(cas, state, VID.of(target), targetLayer, targetAdapter);

        assertThat(state.armedFeature).isSameAs(armed);
    }

    /**
     * If the delete itself fails after slot cleanup already modified the CAS, the service must
     * surface a {@link PartialDeleteException} carrying the messages collected so far. This is hard
     * to trigger through the UI - it is not hard here.
     */
    @Test
    void thatAFailureAfterCleanupYieldsPartialDeleteExceptionWithMessages() throws Exception
    {
        givenTargetAndHosts(1);
        hostOneLinks.add(link("agent", target));

        doThrow(new AnnotationException("delete refused")).when(targetAdapter).delete(any(), any(),
                any(), any(VID.class));

        assertThatExceptionOfType(PartialDeleteException.class).isThrownBy(
                () -> sut.deleteAnnotation(cas, state, VID.of(target), targetLayer, targetAdapter))
                .satisfies(e -> assertThat(e.getMessages()).extracting(LogMessage::getMessage)
                        .contains("Cleared slot [agent] in feature [Slots] on [Host]"));
    }

    /**
     * If the delete fails <b>before</b> anything was modified, the original exception must come
     * through rather than a {@link PartialDeleteException}.
     */
    @Test
    void thatAFailureWithoutModificationRethrowsTheOriginalException() throws Exception
    {
        givenTargetAndHosts(1);
        // no links -> no cleanup -> nothing modified before the delete is attempted

        doThrow(new AnnotationException("delete refused")).when(targetAdapter).delete(any(), any(),
                any(), any(VID.class));

        assertThatExceptionOfType(AnnotationException.class).isThrownBy(
                () -> sut.deleteAnnotation(cas, state, VID.of(target), targetLayer, targetAdapter))
                .isNotInstanceOf(PartialDeleteException.class);
    }

    private static class MockAnnotatorState
        extends AnnotatorStateImpl
    {
        private static final long serialVersionUID = 1L;

        FeatureState armedFeature;

        MockAnnotatorState(Project aProject, SourceDocument aDocument, String aUser)
        {
            setProject(aProject);
            setDocument(aDocument, emptyList());
            setUser(new User(aUser));
        }

        @Override
        public FeatureState getArmedFeature()
        {
            return armedFeature;
        }

        @Override
        public void clearArmedSlot()
        {
            armedFeature = null;
        }
    }
}
