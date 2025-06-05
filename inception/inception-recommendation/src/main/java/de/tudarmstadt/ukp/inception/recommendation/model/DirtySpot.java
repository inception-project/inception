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
package de.tudarmstadt.ukp.inception.recommendation.model;

import static java.util.Collections.unmodifiableMap;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.inception.annotation.events.AnnotationEvent;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.TrainingInstance;
import de.tudarmstadt.ukp.inception.rendering.model.Range;

public class DirtySpot
{
    private final SourceDocument document;
    private final String user;
    private final Range affectedRange;
    private final Map<Recommender, List<TrainingInstance>> incrementalTrainingData;

    public DirtySpot(AnnotationEvent aEvent,
            Map<Recommender, List<TrainingInstance>> aIncrementalTrainingData)
    {
        document = aEvent.getDocument();
        user = aEvent.getDocumentOwner();
        affectedRange = aEvent.getAffectedRange();
        incrementalTrainingData = unmodifiableMap(new LinkedHashMap<>(aIncrementalTrainingData));
    }

    public Range getAffectedRange()
    {
        return affectedRange;
    }

    public boolean affectsDocument(long aDocumentId, String aUser)
    {
        return Objects.equals(document.getId(), aDocumentId) && user.equals(aUser);
    }

    public String getUser()
    {
        return user;
    }

    public SourceDocument getDocument()
    {
        return document;
    }

    public Project getProject()
    {
        return document.getProject();
    }

    public Map<Recommender, List<TrainingInstance>> getIncrementalTrainingData()
    {
        return incrementalTrainingData;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(affectedRange, document, user);
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        DirtySpot other = (DirtySpot) obj;
        return Objects.equals(affectedRange, other.affectedRange)
                && Objects.equals(document, other.document) && Objects.equals(user, other.user);
    }
}
