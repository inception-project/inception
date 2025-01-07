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
package de.tudarmstadt.ukp.clarin.webanno.diag.checks;

import static de.tudarmstadt.ukp.inception.support.uima.WebAnnoCasUtil.getRealCas;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.groupingBy;
import static org.apache.uima.cas.impl.Serialization.deserializeCASComplete;
import static org.apache.uima.cas.impl.Serialization.serializeCASComplete;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.resource.ResourceInitializationException;

import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.inception.support.logging.LogMessage;
import de.tudarmstadt.ukp.inception.support.uima.WebAnnoCasUtil;

public class UnreachableAnnotationsCheck
    implements Check
{
    @Override
    public boolean check(SourceDocument aDocument, String aDataOwner, CAS aCas,
            List<LogMessage> aMessages)
    {
        var casImpl = (CASImpl) getRealCas(aCas);

        var annotationCountsBefore = countFeatureStructures(casImpl);
        annotationCountsBefore.remove(CAS.TYPE_NAME_DOCUMENT_ANNOTATION);

        // Disable forced retaining of all assigned annotations so that during serialization,
        // any temporary annotations that got potentially stuck in the CAS can be released.
        var dummy = makeDummyCas();
        try (var ctx = casImpl.ll_enableV2IdRefs(false);
                var ctx1 = dummy.ll_enableV2IdRefs(false)) {
            var data = serializeCASComplete(casImpl);
            deserializeCASComplete(data, dummy);
        }

        var annotationCountsAfter = countFeatureStructures(dummy);
        annotationCountsAfter.remove(CAS.TYPE_NAME_DOCUMENT_ANNOTATION);

        var diffTypes = 0;
        var totalDiff = 0;
        for (var typeName : annotationCountsBefore.keySet().stream().sorted()
                .toArray(String[]::new)) {
            var before = annotationCountsBefore.getOrDefault(typeName, 0l);
            var after = annotationCountsAfter.getOrDefault(typeName, 0l);
            var diff = before - after;
            totalDiff += diff;
            if (diff > 0) {
                diffTypes++;
                aMessages.add(LogMessage.info(this, "Type [%s] has [%d] unreachable instances",
                        typeName, diff));
            }
        }

        if (totalDiff > 0) {
            if (diffTypes > 1) {
                aMessages.add(LogMessage.info(this,
                        "A total of [%d] unreachable instances that were found", totalDiff));
            }
        }

        return true;
    }

    public static CASImpl makeDummyCas()
    {
        try {
            return (CASImpl) WebAnnoCasUtil.getRealCas(WebAnnoCasUtil.createCas());
        }
        catch (ResourceInitializationException e) {
            throw new IllegalStateException(e);
        }
    }

    public static Map<String, Long> countFeatureStructures(CASImpl casImpl)
    {
        return findAllFeatureStructures(casImpl).stream() //
                .map(fs -> fs.getType().getName()) //
                .collect(groupingBy(identity(), counting()));
    }

    public static Set<FeatureStructure> findAllFeatureStructures(CAS aCas)
    {
        Set<FeatureStructure> allFSes = new LinkedHashSet<>();
        ((CASImpl) aCas).walkReachablePlusFSsSorted(allFSes::add, null, null, null);
        return allFSes;
    }
}
