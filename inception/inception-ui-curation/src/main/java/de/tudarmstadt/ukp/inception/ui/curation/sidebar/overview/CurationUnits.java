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
package de.tudarmstadt.ukp.inception.ui.curation.sidebar.overview;

import static java.util.Collections.emptySet;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationSet;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.ui.curation.overview.CurationUnit;

/**
 * The computed curation units together with the document and the data owners they were computed
 * from.
 */
public class CurationUnits
    implements Serializable
{
    private static final long serialVersionUID = 5299982176936526049L;

    private List<CurationUnit> units = new ArrayList<>();
    private SourceDocument document;
    private Set<AnnotationSet> dataOwners = emptySet();

    public List<CurationUnit> getUnits()
    {
        return units;
    }

    public Set<AnnotationSet> getDataOwners()
    {
        return dataOwners;
    }

    /**
     * @param aDocument
     *            the document the units were computed from.
     * @param aDataOwners
     *            the data owners the units were computed from.
     */
    public void set(SourceDocument aDocument, Collection<AnnotationSet> aDataOwners,
            List<CurationUnit> aUnits)
    {
        units = aUnits;
        document = aDocument;
        dataOwners = Set.copyOf(aDataOwners);
    }

    public boolean isStaleFor(SourceDocument aDocument)
    {
        return aDocument != null && !aDocument.equals(document);
    }

    public boolean isStaleFor(SourceDocument aDocument, Collection<AnnotationSet> aDataOwners)
    {
        return isStaleFor(aDocument) || !dataOwners.equals(Set.copyOf(aDataOwners));
    }
}
