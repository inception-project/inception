/*******************************************************************************
 * Copyright 2012
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
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

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
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
     * The starting offset position for the current display window
     */
    private int annotationOffsetStart;
    /**
     * The end offset position for the current display window
     */
    private int annotationOffsetEnd;
    /**
     * The annotation type, span or arc value in brat annotation window
     */
    private String annotationType;
    /**
     * The id of the origin span in arc annotation
     */
    private String origin;
    /**
     * The id of target span in arc annotation
     */
    private String target;

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
     * Used to enable/disable the display of lemma layers
     */
    private boolean displayLemmaSelected;
    /**
     * Used to enable/disable auto-scrolling while annotation
     */
    private boolean scrollPage;

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

    public boolean isDisplayLemmaSelected()
    {
        return displayLemmaSelected;
    }

    public void setDisplayLemmaSelected(boolean aIsDisplayLemmaSelected)
    {
        displayLemmaSelected = aIsDisplayLemmaSelected;
    }

    public boolean isScrollPage()
    {
        return scrollPage;
    }

    public void setScrollPage(boolean aScrollPage)
    {
        scrollPage = aScrollPage;
    }

    public int getAnnotationOffsetStart()
    {
        return annotationOffsetStart;
    }

    public void setAnnotationOffsetStart(int aAnnotationOffsetStart)
    {
        annotationOffsetStart = aAnnotationOffsetStart;
    }

    public int getAnnotationOffsetEnd()
    {
        return annotationOffsetEnd;
    }

    public void setAnnotationOffsetEnd(int aAnnotationOffsetEnd)
    {
        annotationOffsetEnd = aAnnotationOffsetEnd;
    }

    public String getType()
    {
        return annotationType;
    }

    public void setType(String aType)
    {
        annotationType = aType;
    }

    public String getOrigin()
    {
        return origin;
    }

    public void setOrigin(String aOrigin)
    {
        origin = aOrigin;
    }

    public String getTarget()
    {
        return target;
    }

    public void setTarget(String aTarget)
    {
        target = aTarget;
    }

}
