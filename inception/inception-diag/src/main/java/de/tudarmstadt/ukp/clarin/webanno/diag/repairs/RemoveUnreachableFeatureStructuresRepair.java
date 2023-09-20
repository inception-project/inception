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
package de.tudarmstadt.ukp.clarin.webanno.diag.repairs;

import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.getRealCas;
import static de.tudarmstadt.ukp.clarin.webanno.diag.checks.UnreachableAnnotationsCheck.countFeatureStructures;
import static de.tudarmstadt.ukp.clarin.webanno.diag.checks.UnreachableAnnotationsCheck.makeDummyCas;
import static org.apache.uima.cas.impl.Serialization.deserializeCASComplete;
import static org.apache.uima.cas.impl.Serialization.serializeCASComplete;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.List;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.impl.CASCompleteSerializer;
import org.apache.uima.cas.impl.CASImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.support.logging.LogMessage;

public class RemoveUnreachableFeatureStructuresRepair
    implements Repair
{
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Override
    public void repair(Project aProject, CAS aCas, List<LogMessage> aMessages)
    {
        var casImpl = (CASImpl) getRealCas(aCas);

        var annotationCountsBefore = countFeatureStructures(casImpl);

        // Disable forced retaining of all assigned annotations so that during serialization,
        // any temporary annotations that got potentially stuck in the CAS can be released.
        var dummy = makeDummyCas();
        var bytesBefore = size(serializeCASComplete(casImpl));
        int bytesAfter;
        try (var ctx = casImpl.ll_enableV2IdRefs(false);
                var ctx1 = dummy.ll_enableV2IdRefs(false)) {
            var data = serializeCASComplete(casImpl);
            deserializeCASComplete(data, dummy);
            bytesAfter = size(serializeCASComplete(dummy));
        }

        var annotationCountsAfter = countFeatureStructures(dummy);

        var diffTypes = 0;
        var totalDiff = 0;
        var totalBefore = 0;
        var totalAfter = 0;
        for (var typeName : annotationCountsBefore.keySet().stream().sorted()
                .toArray(String[]::new)) {
            var before = annotationCountsBefore.getOrDefault(typeName, 0l);
            var after = annotationCountsAfter.getOrDefault(typeName, 0l);
            var diff = before - after;
            totalDiff += diff;
            totalBefore += before;
            totalAfter += after;
            if (diff > 0) {
                diffTypes++;
                aMessages.add(LogMessage.info(this,
                        "Type [%s] had [%d] unreachable instances that were removed (before: [%d], after: [%d])",
                        typeName, diff, before, after));
            }
        }

        if (totalDiff > 0) {
            if (diffTypes > 1) {
                aMessages.add(LogMessage.info(this,
                        "A total of [%d] unreachable instances that were removed (before: [%d], after: [%d])",
                        totalDiff, totalBefore, totalAfter));
            }
            aMessages.add(LogMessage.info(this,
                    "Saved [%d] uncompressed bytes (before: [%d], after: [%d])",
                    bytesBefore - bytesAfter, bytesBefore, bytesAfter));
        }
    }

    private int size(CASCompleteSerializer aSer)
    {
        try {
            return WebAnnoCasUtil.casToByteArray(aSer).length;
        }
        catch (IOException e) {
            LOG.error("Unable to calculate size of CAS", e);
            return -1;
        }
    }
}
