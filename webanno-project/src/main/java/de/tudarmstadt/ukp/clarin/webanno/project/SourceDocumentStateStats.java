/*
 * Copyright 2018
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
 */
package de.tudarmstadt.ukp.clarin.webanno.project;

import de.tudarmstadt.ukp.clarin.webanno.model.ProjectState;

public final class SourceDocumentStateStats
{
    private final long total;
    private final long an;
    private final long aip;
    private final long af;
    private final long cip;
    private final long cf;
    
    public SourceDocumentStateStats(Long aTotal, Long aAn, Long aAip, Long aAf, Long aCip,
            Long aCf)
    {
        super();
        total = aTotal != null ? aTotal : 0l;
        an = aAn != null ? aAn : 0l;
        aip = aAip != null ? aAip : 0l;
        af = aAf != null ? aAf : 0l;
        cip = aCip != null ? aCip : 0l;
        cf = aCf != null ? aCf : 0l;
    }
    
    public long getTotal()
    {
        return total;
    }
    
    public long getNewAnnotations()
    {
        return an;
    }
    
    public long getAnnotationsInProgress()
    {
        return aip;
    }
    
    public long getFinishedAnnotations()
    {
        return af;
    }
    
    public long getCurationsInProgress()
    {
        return cip;
    }
    
    public long getCurationsFinished()
    {
        return cf;
    }
    
    public ProjectState getProjectState()
    {
        if (total == cf) {
            return ProjectState.CURATION_FINISHED;
        }
        else if (total == af) {
            return ProjectState.ANNOTATION_FINISHED;
        }
        else if (total == an) {
            return ProjectState.NEW;
        }
        else if (cip > 0 || cf > 0) {
            return ProjectState.CURATION_IN_PROGRESS;
        }
        else if (aip > 0 || af > 0) {
            return ProjectState.ANNOTATION_IN_PROGRESS;
        }
        else {
            // This should actually never happen...
            throw new IllegalStateException("Unable to determine project state from " + toString());
        }
    }

    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder();
        builder.append("SourceDocumentStateStats [total=");
        builder.append(total);
        builder.append(", an=");
        builder.append(an);
        builder.append(", aip=");
        builder.append(aip);
        builder.append(", af=");
        builder.append(af);
        builder.append(", cip=");
        builder.append(cip);
        builder.append(", cf=");
        builder.append(cf);
        builder.append("]");
        return builder.toString();
    }
    
    
}
