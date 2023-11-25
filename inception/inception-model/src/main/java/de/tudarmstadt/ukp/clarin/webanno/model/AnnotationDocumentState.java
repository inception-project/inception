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
package de.tudarmstadt.ukp.clarin.webanno.model;

import static java.util.stream.Collectors.toUnmodifiableList;

import java.util.List;
import java.util.stream.Stream;

import com.fasterxml.jackson.annotation.JsonProperty;

import de.tudarmstadt.ukp.inception.support.db.PersistentEnum;
import de.tudarmstadt.ukp.inception.support.wicket.HasSymbol;

/**
 * Variables for the different states of a {@link AnnotationDocument} workflow.
 */
public enum AnnotationDocumentState
    implements PersistentEnum, HasSymbol
{
    /**
     * Indicates that the annotator has not yet started working on the document. The first time an
     * annotator opens a document, the state is immediately switched from {@link #NEW} to
     * {@link #IN_PROGRESS}. It is not necessary for the annotator to actually create an annotation.
     * If no {@link AnnotationDocument} exists yet for a given combination of {@link SourceDocument}
     * and {@code User}, then {@link #NEW} is the default state that should be assumed.
     * <p>
     * When an annotation document is set back to the {@link #NEW} state, all annotations made by
     * the annotator should be discarded. Either, the annotation CAS should be deleted entirely, or
     * it should be reset to the initial CAS of the source document.
     */
    @JsonProperty("NEW")
    NEW("NEW", "<i class=\"far fa-circle\"></i>", "black", false, false),

    /**
     * Indicates that the annotator as started working on the document. The annotator may not yet
     * actually have created any annotations.
     */
    @JsonProperty("IN_PROGRESS")
    IN_PROGRESS("INPROGRESS", "<i class=\"far fa-play-circle\"></i>", "blue", true, false),

    /**
     * Indicates that the annotation is complete. No further annotations can be added.
     */
    @JsonProperty("FINISHED")
    FINISHED("FINISHED", "<i class=\"far fa-check-circle\"></i>", "red", true, true),

    /**
     * Indicates that the given document should not be offered to an annotator. If is possible that
     * the annotator has already started working on the project. When set explicitly by an
     * annotator, it indicates that the annotator was unable to successfully complete the annotation
     * - possibly due to a problem with the document not working in the editor or such.
     */
    @JsonProperty("IGNORE")
    IGNORE("IGNORE", "<i class=\"fas fa-lock\"></i>", "black", false, true);

    private static final List<AnnotationDocumentState> TAKEN_STATES;
    private static final List<AnnotationDocumentState> TERMINAL_STATES;

    static {
        TAKEN_STATES = Stream.of(values()) //
                .filter(AnnotationDocumentState::isTaken) //
                .collect(toUnmodifiableList());
        TERMINAL_STATES = Stream.of(values()) //
                .filter(AnnotationDocumentState::isTerminal) //
                .collect(toUnmodifiableList());
    }

    private final String id;
    private final String color;
    private final String symbol;

    /**
     * An annotation document that is taken counts towards goals. E.g. if a document is taken by a
     * certain number of annotators, then the system may not assign it to other annotators anymore.
     */
    private final boolean taken;

    /**
     * This is an end state of an annotation document. An annotation document in a terminal state
     * can no longer be edited.
     */
    private final boolean terminal;

    AnnotationDocumentState(String aId, String aSymbol, String aColor, boolean aTaken,
            boolean aTerminal)
    {
        id = aId;
        symbol = aSymbol;
        color = aColor;
        taken = aTaken;
        terminal = aTerminal;
    }

    public String getName()
    {
        return getId();
    }

    @Override
    public String getId()
    {
        return id;
    }

    public String getColor()
    {
        return color;
    }

    @Override
    public String toString()
    {
        return getId();
    }

    /**
     * @return if a annotation document is considered to be taken.
     */
    public boolean isTaken()
    {
        return taken;
    }

    /**
     * @return if a state is terminal.
     */
    public boolean isTerminal()
    {
        return terminal;
    }

    public String symbol()
    {
        return symbol;
    }

    public static AnnotationDocumentState defaultState()
    {
        return NEW;
    }

    public static List<AnnotationDocumentState> takenStates()
    {
        return TAKEN_STATES;
    }

    public static List<AnnotationDocumentState> terminalStates()
    {
        return TERMINAL_STATES;
    }

    public static String symbol(AnnotationDocument aDoc)
    {
        if (aDoc == null) {
            return NEW.symbol;
        }

        if (aDoc.getAnnotatorState() != null && aDoc.getAnnotatorState() != aDoc.getState()) {
            return aDoc.getState().symbol + " (" + aDoc.getAnnotatorState().symbol + ")";
        }

        if (aDoc.getAnnotatorState() == AnnotationDocumentState.IGNORE) {
            return aDoc.getState().symbol + " (<i class=\"fas fa-exclamation-triangle\"></i>)";
        }

        return aDoc.getState().symbol;
    }

    public static AnnotationDocumentState oneClickTransition(AnnotationDocument aDoc)
    {
        if (aDoc.getAnnotatorState() != null) {
            switch (aDoc.getAnnotatorState()) {
            case FINISHED: // fall-through
            case IN_PROGRESS: // fall-through
            case IGNORE:
                switch (aDoc.getState()) {
                case IN_PROGRESS:
                    return FINISHED;
                case FINISHED:
                    return IGNORE;
                case IGNORE:
                    return IN_PROGRESS;
                case NEW: // fall-through
                default:
                    // These combinations of effective and annotator state should not be reachable.
                    // We we reach them, better do nothing.
                    return aDoc.getState();
                }
            case NEW: // fall-through
            default:
                // These states should normally not be settable by the annotator. We should not do
                // anything if we encounter these...
                return aDoc.getState();
            }
        }

        switch (aDoc.getState()) {
        case NEW:
            return IGNORE;
        case IN_PROGRESS:
            return FINISHED;
        case FINISHED:
            return IN_PROGRESS;
        case IGNORE:
            return NEW;
        default:
            return aDoc.getState();
        }
    }
}
