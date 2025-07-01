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
package de.tudarmstadt.ukp.inception.documents.api;

import java.io.Serializable;

import de.tudarmstadt.ukp.clarin.webanno.model.ProjectState;

public final class SourceDocumentStateStats
    implements Serializable
{
    private static final long serialVersionUID = -8098769134446827697L;

    private final long total;
    private final long annotationsNew;
    private final long annotationsInProgress;
    private final long annotationsFinished;
    private final long curationsInProgress;
    private final long curationsFinished;

    public SourceDocumentStateStats(Long aTotal, Long aAn, Long aAip, Long aAf, Long aCip, Long aCf)
    {
        total = aTotal != null ? aTotal : 0l;
        annotationsNew = aAn != null ? aAn : 0l;
        annotationsInProgress = aAip != null ? aAip : 0l;
        annotationsFinished = aAf != null ? aAf : 0l;
        curationsInProgress = aCip != null ? aCip : 0l;
        curationsFinished = aCf != null ? aCf : 0l;
    }

    public long getTotal()
    {
        return total;
    }

    public long getNewAnnotations()
    {
        return annotationsNew;
    }

    public long getAnnotationsInProgress()
    {
        return annotationsInProgress;
    }

    public long getFinishedAnnotations()
    {
        return annotationsFinished;
    }

    public long getCurationsInProgress()
    {
        return curationsInProgress;
    }

    public long getCurationsFinished()
    {
        return curationsFinished;
    }

    public ProjectState getProjectState()
    {
        if (total == 0) {
            return ProjectState.NEW;
        }

        if (total == curationsFinished) {
            return ProjectState.CURATION_FINISHED;
        }

        if (total == annotationsFinished) {
            return ProjectState.ANNOTATION_FINISHED;
        }

        if (total == annotationsNew) {
            return ProjectState.NEW;
        }

        if (annotationsInProgress > 0 || annotationsNew > 0) {
            return ProjectState.ANNOTATION_IN_PROGRESS;
        }

        if (annotationsFinished > 0 || curationsInProgress > 0) {
            return ProjectState.CURATION_IN_PROGRESS;
        }

        // This should actually never happen...
        throw new IllegalStateException("Unable to determine project state from " + toString());
    }

    @Override
    public String toString()
    {
        var builder = new StringBuilder();
        builder.append("SourceDocumentStateStats [total=");
        builder.append(total);
        builder.append(", an=");
        builder.append(annotationsNew);
        builder.append(", aip=");
        builder.append(annotationsInProgress);
        builder.append(", af=");
        builder.append(annotationsFinished);
        builder.append(", cip=");
        builder.append(curationsInProgress);
        builder.append(", cf=");
        builder.append(curationsFinished);
        builder.append("]");
        return builder.toString();
    }
}
