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
package de.tudarmstadt.ukp.inception.pivot.report;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.ProjectUserPermissions;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;

public class ReportDecl
    implements Serializable
{
    private static final long serialVersionUID = 3617963902948560054L;

    private AggregatorDecl aggregator;

    private List<ProjectUserPermissions> annotators = new ArrayList<>();
    private List<SourceDocument> documents = new ArrayList<>();
    private List<AnnotationDocumentState> states = new ArrayList<>();
    private List<ExtractorDecl> rowExtractors = new ArrayList<>();
    private List<ExtractorDecl> colExtractors = new ArrayList<>();
    private List<ExtractorDecl> cellExtractors = new ArrayList<>();

    public AggregatorDecl getAggregator()
    {
        return aggregator;
    }

    public void setAggregator(AggregatorDecl aAggregator)
    {
        aggregator = aAggregator;
    }

    public List<ProjectUserPermissions> getAnnotators()
    {
        return annotators;
    }

    public void setAnnotators(List<ProjectUserPermissions> aAnnotators)
    {
        annotators = aAnnotators != null ? aAnnotators : new ArrayList<>();
    }

    public List<SourceDocument> getDocuments()
    {
        return documents;
    }

    public void setDocuments(List<SourceDocument> aDocuments)
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

    public List<ExtractorDecl> getRowExtractors()
    {
        return rowExtractors;
    }

    public void setRowExtractors(List<ExtractorDecl> aRowExtractors)
    {
        rowExtractors = aRowExtractors != null ? aRowExtractors : new ArrayList<>();
    }

    public List<ExtractorDecl> getColExtractors()
    {
        return colExtractors;
    }

    public void setColExtractors(List<ExtractorDecl> aColExtractors)
    {
        colExtractors = aColExtractors != null ? aColExtractors : new ArrayList<>();
    }

    public List<ExtractorDecl> getCellExtractors()
    {
        return cellExtractors;
    }

    public void setCellExtractors(List<ExtractorDecl> aCellExtractors)
    {
        cellExtractors = aCellExtractors != null ? aCellExtractors : new ArrayList<>();
    }
}
