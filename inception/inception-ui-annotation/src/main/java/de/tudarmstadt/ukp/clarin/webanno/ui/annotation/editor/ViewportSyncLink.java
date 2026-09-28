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
package de.tudarmstadt.ukp.clarin.webanno.ui.annotation.editor;

import static de.tudarmstadt.ukp.inception.rendering.selection.FocusPosition.TOP;
import static java.lang.String.format;
import static java.util.stream.Collectors.joining;

import java.io.IOException;
import java.io.Serializable;
import java.lang.invoke.MethodHandles;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import org.danekja.java.util.function.serializable.SerializableSupplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotatorState;
import de.tudarmstadt.ukp.inception.rendering.editorstate.DiamContext;
import de.tudarmstadt.ukp.inception.rendering.editorstate.DocumentEditor;
import de.tudarmstadt.ukp.inception.rendering.paging.NoPagingStrategy;
import de.tudarmstadt.ukp.inception.rendering.selection.AnnotatorViewportChangedEvent;

/**
 * Scroll synchronization between two document editors.
 * <p>
 * Which mechanism applies depends on paging, not on which document is shown:
 * <ul>
 * <li><b>Both unpaged:</b> the viewport-sync hub on the client aligns the two editors by document
 * structure and falls back to character offsets. That works between any two documents.</li>
 * <li><b>Both paged with the same paging strategy:</b> {@link #follow} moves the other editor to
 * the window begin offset of the one that paged. Offsets only mean the same thing in the same text,
 * so this requires both editors to show the same document.</li>
 * <li><b>Mixed paging, or different paging strategies:</b> cannot sync.</li>
 * </ul>
 * The data owner never matters: both alignments depend on the text, not on whose annotations are
 * shown.
 * <p>
 * The two editors are looked up through suppliers each time, so the link follows whatever the owner
 * currently considers its two sides. A supplier may return {@code null} if that side currently has
 * no editor.
 */
public class ViewportSyncLink
    implements Serializable
{
    private static final long serialVersionUID = -2215783893170311020L;

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    /**
     * Why two editors cannot be synchronized.
     */
    public enum Obstacle
    {
        /** One of the sides has no editor. */
        NO_EDITOR,
        /** One of the editors shows no document. */
        NO_DOCUMENT,
        /** One editor is paged, the other is not. */
        MIXED_PAGING,
        /** Both editors are paged, but by different paging strategies. */
        DIFFERENT_PAGING,
        /** Both editors are paged, but show different documents. */
        DIFFERENT_DOCUMENTS
    }

    private final SerializableSupplier<? extends DocumentEditor> first;
    private final SerializableSupplier<? extends DocumentEditor> second;

    private transient boolean following = false;

    public ViewportSyncLink(SerializableSupplier<? extends DocumentEditor> aFirst,
            SerializableSupplier<? extends DocumentEditor> aSecond)
    {
        first = aFirst;
        second = aSecond;
    }

    /**
     * @return why the two editors cannot be synchronized, or {@link Optional#empty()} if they can.
     */
    public Optional<Obstacle> getObstacle()
    {
        var firstEditor = first.get();
        var secondEditor = second.get();
        if (firstEditor == null || secondEditor == null) {
            return Optional.of(Obstacle.NO_EDITOR);
        }

        var firstState = firstEditor.getAnnotatorState();
        var secondState = secondEditor.getAnnotatorState();
        if (firstState == null || secondState == null || firstState.getDocument() == null
                || secondState.getDocument() == null) {
            return Optional.of(Obstacle.NO_DOCUMENT);
        }

        var firstPaged = isPaged(firstState);
        var secondPaged = isPaged(secondState);
        if (firstPaged != secondPaged) {
            return Optional.of(Obstacle.MIXED_PAGING);
        }

        if (!firstPaged) {
            // The client aligns unpaged editors, by structure where it can - any two documents
            return Optional.empty();
        }

        if (!Objects.equals(firstState.getPagingStrategy().getClass(),
                secondState.getPagingStrategy().getClass())) {
            return Optional.of(Obstacle.DIFFERENT_PAGING);
        }

        if (!Objects.equals(firstState.getDocument(), secondState.getDocument())) {
            // Paged editors are synced by offset, which is only meaningful in the same text
            return Optional.of(Obstacle.DIFFERENT_DOCUMENTS);
        }

        return Optional.empty();
    }

    /**
     * @return whether the two editors can be synchronized.
     */
    public boolean isPossible()
    {
        return getObstacle().isEmpty();
    }

    /**
     * @param aEnabled
     *            whether the user wants the two editors synchronized.
     * @return the script linking the two editors in the client-side hub if sync is enabled and
     *         possible, or unlinking <b>both</b> editors otherwise. Empty if neither editor is
     *         registered with the hub.
     */
    public Optional<String> linkScript(boolean aEnabled)
    {
        var ids = Stream.of(first, second) //
                .map(SerializableSupplier::get) //
                .filter(Objects::nonNull) //
                .map(DiamContext::getViewportSyncClientId) //
                .flatMap(Optional::stream) //
                .toList();

        if (ids.isEmpty()) {
            return Optional.empty();
        }

        String script;
        if (aEnabled && ids.size() == 2 && isPossible()) {
            script = format("ExternalEditor.viewportSync.link('%s', '%s');", ids.get(0),
                    ids.get(1));
        }
        else {
            // Unlink both sides, not only one: either may still be linked to the other
            script = ids.stream() //
                    .map(id -> format("ExternalEditor.viewportSync.unlink('%s');", id)) //
                    .collect(joining(" "));
        }

        // The hub ships with the external-editor bundle; a page whose editors are all
        // non-external may not have it - then there is nothing to (un-)link anyway
        return Optional
                .of("if (window.ExternalEditor && ExternalEditor.viewportSync) { " + script + " }");
    }

    /**
     * If one of the two editors has paged, move the other one to the same offset. Callers only pass
     * the event on while sync is enabled. Unpaged editors are synced on the client, so events from
     * them are ignored here.
     *
     * @param aEvent
     *            the viewport change.
     */
    public void follow(AnnotatorViewportChangedEvent aEvent)
    {
        if (following) {
            return;
        }

        var target = aEvent.getRequestHandler();
        if (target == null) {
            return;
        }

        if (!isPossible()) {
            return;
        }

        var firstEditor = first.get();
        var secondEditor = second.get();

        DocumentEditor leader;
        DocumentEditor follower;
        if (aEvent.isFor(firstEditor.getAnnotatorState())) {
            leader = firstEditor;
            follower = secondEditor;
        }
        else if (aEvent.isFor(secondEditor.getAnnotatorState())) {
            leader = secondEditor;
            follower = firstEditor;
        }
        else {
            return;
        }

        var leaderState = leader.getAnnotatorState();
        if (!isPaged(leaderState)) {
            return;
        }

        try {
            following = true;

            // Explicitly the follower's CAS, not the active editor's: this moves the follower's
            // state and re-renders it, and the active editor is typically the one that paged.
            follower.getAnnotatorState().moveToOffset(follower.getEditorCas(),
                    leaderState.getWindowBeginOffset(), TOP);
            follower.actionRefreshDocument(target);

            if (follower instanceof DocumentEditorPanel panel) {
                target.add(panel.getActionBarItems(), panel.getPositionLabel());
            }
        }
        catch (IOException e) {
            LOG.error("Unable to synchronize viewport", e);
        }
        finally {
            following = false;
        }
    }

    private static boolean isPaged(AnnotatorState aState)
    {
        return !(aState.getPagingStrategy() instanceof NoPagingStrategy);
    }
}
