/*******************************************************************************
 * Copyright 2012
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
 ******************************************************************************/
package de.tudarmstadt.ukp.clarin.webanno.brat.annotation;

import java.io.Serializable;
import java.util.HashSet;

import org.apache.wicket.markup.html.panel.FeedbackPanel;

import de.tudarmstadt.ukp.clarin.webanno.model.Mode;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.Tag;
import de.tudarmstadt.ukp.clarin.webanno.model.TagSet;
import de.tudarmstadt.ukp.clarin.webanno.model.User;

/**
 * Data model for the {@link BratAnnotator}
 *
 * @author Seid Muhie Yimam
 *
 */
public class BratAnnotatorModel
    implements Serializable
{
    private static final long serialVersionUID = 1078613192789450714L;

    /**
     * The Project the annotator working on
     */
    private Project project;
    /**
     * The source document the to be annotated
     */
    private SourceDocument document;

    /**
     * The current user annotating the document
     */
    private User user;
    /**
     * The sentence address where the display window starts with, in its UIMA annotation
     */
    private int displayWindowStartSentenceAddress = -1;
    /**
     * The very last sentence address in its UIMA annotation
     */
    private int lastSentenceAddress;
    /**
     * The very first sentence address in its UIMA annotation
     */
    private int firstSentenceAddress;

    /**
     * The begin offset of a sentence
     */
    private int sentenceBeginOffset;
    /**
     * The end offset of a sentence
     */
    private int sentenceEndOffset;
    // Annotation preferences, to be saved in a file system
    /**
     * The annotation layers available in the current project.
     */
    private HashSet<TagSet> annotationLayers = new HashSet<TagSet>();
    /**
     * The number of sentences to be dispalyed at atime
     */
    private int windowSize = 10;

    /**
     * Used to enable/disable auto-scrolling while annotation
     */
    private boolean scrollPage;
    /**
     * If the document is opened through the next/previous buttons on the annotation page, not with
     * the open dialog method, used to change {@link #document}
     */
    private String documentName;
    /**
     * The Mode of the current operations as either {@link Mode#ANNOTATION} or as
     * {@link Mode#CURATION}
     */
    private Mode mode;

    /**
     * The previously selected {@link TagSet} and {@link Tag} for a span/Arc annotation so as toz
     * pre-fill the type in the span/arc annotation dialog (only for new span/arc annotations)
     *
     * @return
     */
    private TagSet rememberedSpanTagSet;
    private TagSet rememberedArcTagSet;
    private Tag rememberedSpanTag;
    private Tag rememberedArcTag;
    /**
     * Specific message to be sent from the annotation dialog to the {@link BratAnnotator} so that
     * it can be displayed in the {@link FeedbackPanel}
     */
    private String message = "";

    private boolean annotationCleared = false;

    // determine if static color for annotations will be used or we shall dynamically generate one
    private boolean staticColor = true;

    // if it is annotation or delete operation
    private boolean isAnnotate;

    public Project getProject()
    {
        return project;
    }

    public void setProject(Project aProject)
    {
        project = aProject;
    }

    public SourceDocument getDocument()
    {
        return document;
    }

    public void setDocument(SourceDocument aDocument)
    {
        document = aDocument;
    }

    public User getUser()
    {
        return user;
    }

    public void setUser(User aUser)
    {
        user = aUser;
    }

    public int getSentenceAddress()
    {
        return displayWindowStartSentenceAddress;
    }

    public void setSentenceAddress(int aSentenceAddress)
    {
        displayWindowStartSentenceAddress = aSentenceAddress;
    }

    public int getLastSentenceAddress()
    {
        return lastSentenceAddress;
    }

    public void setLastSentenceAddress(int aLastSentenceAddress)
    {
        lastSentenceAddress = aLastSentenceAddress;
    }

    public int getFirstSentenceAddress()
    {
        return firstSentenceAddress;
    }

    public void setFirstSentenceAddress(int aFirstSentenceAddress)
    {
        firstSentenceAddress = aFirstSentenceAddress;
    }

    public HashSet<TagSet> getAnnotationLayers()
    {
        return annotationLayers;
    }

    public void setAnnotationLayers(HashSet<TagSet> aAnnotationLayers)
    {
        annotationLayers = aAnnotationLayers;
    }

    public int getWindowSize()
    {
        return windowSize;
    }

    public void setWindowSize(int aWindowSize)
    {
        windowSize = aWindowSize;
    }

    public boolean isScrollPage()
    {
        return scrollPage;
    }

    public void setScrollPage(boolean aScrollPage)
    {
        scrollPage = aScrollPage;
    }

    public String getDocumentName()
    {
        return documentName;
    }

    public void setDocumentName(String documentName)
    {
        this.documentName = documentName;
    }

    public Mode getMode()
    {
        return mode;
    }

    public void setMode(Mode mode)
    {
        this.mode = mode;
    }

    public TagSet getRememberedSpanTagSet()
    {
        return rememberedSpanTagSet;
    }

    public void setRememberedSpanTagSet(TagSet rememberedTagSet)
    {
        this.rememberedSpanTagSet = rememberedTagSet;
    }

    public Tag getRememberedSpanTag()
    {
        return rememberedSpanTag;
    }

    public void setRememberedSpanTag(Tag rememberedTag)
    {
        this.rememberedSpanTag = rememberedTag;
    }

    public TagSet getRememberedArcTagSet()
    {
        return rememberedArcTagSet;
    }

    public void setRememberedArcTagSet(TagSet rememberedArcTagSet)
    {
        this.rememberedArcTagSet = rememberedArcTagSet;
    }

    public Tag getRememberedArcTag()
    {
        return rememberedArcTag;
    }

    public void setRememberedArcTag(Tag rememberedArcTag)
    {
        this.rememberedArcTag = rememberedArcTag;
    }

    public int getSentenceBeginOffset()
    {
        return sentenceBeginOffset;
    }

    public void setSentenceBeginOffset(int sentenceBeginOffset)
    {
        this.sentenceBeginOffset = sentenceBeginOffset;
    }

    public int getSentenceEndOffset()
    {
        return sentenceEndOffset;
    }

    public void setSentenceEndOffset(int sentenceEndOffset)
    {
        this.sentenceEndOffset = sentenceEndOffset;
    }

    public String getMessage()
    {
        return message;
    }

    public void setMessage(String message)
    {
        this.message = message;
    }

    public boolean isAnnotationCleared()
    {
        return annotationCleared;
    }

    public void setAnnotationCleared(boolean annotationCleared)
    {
        this.annotationCleared = annotationCleared;
    }

    public boolean isStaticColor()
    {
        return staticColor;
    }

    public void setStaticColor(boolean staticColor)
    {
        this.staticColor = staticColor;
    }

    public boolean isAnnotate()
    {
        return isAnnotate;
    }

    public void setAnnotate(boolean isAnnotate)
    {
        this.isAnnotate = isAnnotate;
    }


}
