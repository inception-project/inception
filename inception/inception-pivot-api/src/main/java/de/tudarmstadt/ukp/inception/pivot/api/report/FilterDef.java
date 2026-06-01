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
package de.tudarmstadt.ukp.inception.pivot.api.report;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState;

@JsonInclude(NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonPropertyOrder({ "annotators", "documents", "states" })
public class FilterDef
    implements Serializable
{
    private static final long serialVersionUID = 1L;

    private List<String> annotators = new ArrayList<>();
    private List<String> documents = new ArrayList<>();
    private List<AnnotationDocumentState> states = new ArrayList<>();

    public List<String> getAnnotators()
    {
        return annotators;
    }

    public void setAnnotators(List<String> aAnnotators)
    {
        annotators = aAnnotators != null ? aAnnotators : new ArrayList<>();
    }

    public List<String> getDocuments()
    {
        return documents;
    }

    public void setDocuments(List<String> aDocuments)
    {
        documents = aDocuments != null ? aDocuments : new ArrayList<>();
    }

    public List<AnnotationDocumentState> getStates()
    {
        return states;
    }

    public void setStates(List<AnnotationDocumentState> aStates)
    {
        states = aStates != null ? aStates : new ArrayList<>();
    }

    @Override
    public boolean equals(Object aOther)
    {
        if (this == aOther) {
            return true;
        }
        if (!(aOther instanceof FilterDef that)) {
            return false;
        }
        return Objects.equals(annotators, that.annotators) //
                && Objects.equals(documents, that.documents) //
                && Objects.equals(states, that.states);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(annotators, documents, states);
    }
}
