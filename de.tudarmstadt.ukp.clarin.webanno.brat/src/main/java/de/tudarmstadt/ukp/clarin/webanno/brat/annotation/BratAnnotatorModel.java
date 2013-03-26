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

import org.apache.uima.jcas.JCas;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.TagSet;
import de.tudarmstadt.ukp.clarin.webanno.model.User;

public class BratAnnotatorModel implements Serializable
{
    private static final long serialVersionUID = 1078613192789450714L;

    private Project project;
    private SourceDocument document;
    private String fileName;
    private User user;
    private int sentenceAddress = -1;
    private int lastSentenceAddress;
    private int firstSentenceAddress;

    // Annotation preferences
    private HashSet<TagSet> annotationLayers = new HashSet<TagSet>();
    private int windowSize;
    private boolean isDisplayLemmaSelected;
    private boolean scrollPage;

    private transient JCas jCas;
    private int annotationOffsetStart;
    private int annotationOffsetEnd;
    private String type;
    private String origin;
    private String target;

    // If Brat action is getdocument, no aut-scroll at all
    private boolean isGetDocument;

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
        return sentenceAddress;
    }

    public void setSentenceAddress(int aSentenceAddress)
    {
        sentenceAddress = aSentenceAddress;
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
        return isDisplayLemmaSelected;
    }

    public void setDisplayLemmaSelected(boolean aIsDisplayLemmaSelected)
    {
        isDisplayLemmaSelected = aIsDisplayLemmaSelected;
    }

    public boolean isScrollPage()
    {
        return scrollPage;
    }

    public void setScrollPage(boolean aScrollPage)
    {
        scrollPage = aScrollPage;
    }

    public JCas getjCas()
    {
        return jCas;
    }

    public void setjCas(JCas aJCas)
    {
        jCas = aJCas;
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
        return type;
    }

    public void setType(String aType)
    {
        type = aType;
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

    public boolean isGetDocument()
    {
        return isGetDocument;
    }

    public void setGetDocument(boolean aIsGetDocument)
    {
        isGetDocument = aIsGetDocument;
    }

    public String getFileName()
    {
        return fileName;
    }

    public void setFileName(String aFileName)
    {
        fileName = aFileName;
    }



}
