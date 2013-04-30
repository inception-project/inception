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

import org.apache.uima.jcas.JCas;

/**
 * UI data for {@link BratAnnotator}
 * @author Seid Muhie Yimam
 *
 */
public class BratAnnotatorUIData{
    private JCas jCas;
    private boolean isGetDocument;

    /**
     * The annotation type, span or arc value in brat annotation window
     */
    private String annotationType;
    /**
     * The id of the origin span in arc annotation
     */
    private int origin;
    /**
     * The id of target span in arc annotation
     */
    private int target;

    /**
     * The starting offset position for the current display window
     */
    private int annotationOffsetStart;
    /**
     * The end offset position for the current display window
     */
    private int annotationOffsetEnd;

    public JCas getjCas()
    {
        return jCas;
    }

    public void setjCas(JCas aJCas)
    {
        jCas = aJCas;
    }

    public boolean isGetDocument()
    {
        return isGetDocument;
    }

    public void setGetDocument(boolean aIsGetDocument)
    {
        isGetDocument = aIsGetDocument;
    }

    public String getType()
    {
        return annotationType;
    }

    public void setType(String aType)
    {
        annotationType = aType;
    }

    public int getOrigin()
    {
        return origin;
    }

    public void setOrigin(int aOrigin)
    {
        origin = aOrigin;
    }

    public int getTarget()
    {
        return target;
    }

    public void setTarget(int aTarget)
    {
        target = aTarget;
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
}