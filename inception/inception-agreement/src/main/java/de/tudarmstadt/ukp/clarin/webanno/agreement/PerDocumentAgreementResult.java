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
package de.tudarmstadt.ukp.clarin.webanno.agreement;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;

import de.tudarmstadt.ukp.clarin.webanno.agreement.measures.DefaultAgreementTraits;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;

public class PerDocumentAgreementResult
    extends AgreementResult_ImplBase
{
    private static final long serialVersionUID = -5103322733512045313L;

    private final Set<SourceDocument> documents = new HashSet<>();
    private final Map<SourceDocument, AgreementSummary> results = new HashMap<>();

    private SummaryStatistics agreementScoreStats = new SummaryStatistics();

    public PerDocumentAgreementResult(AnnotationFeature aFeature, DefaultAgreementTraits aTraits)
    {
        super(aFeature, aTraits);
    }

    public SummaryStatistics getAgreementScoreStats()
    {
        return agreementScoreStats;
    }

    public Set<SourceDocument> getDocuments()
    {
        return documents;
    }

    public AgreementSummary getResult(SourceDocument aDocument)
    {
        return results.get(aDocument);
    }

    public void mergeResult(SourceDocument aDocument, AgreementSummary aRes)
    {
        agreementScoreStats.addValue(aRes.getAgreement());
        documents.add(aDocument);

        var existingRes = getResult(aDocument);
        if (existingRes != null) {
            existingRes.merge(aRes);
        }
        else {
            results.put(aDocument, aRes);
        }
    }
}
