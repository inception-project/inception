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

import static de.tudarmstadt.ukp.clarin.webanno.diag.checks.UnreachableAnnotationsCheck.countFeatureStructures;
import static de.tudarmstadt.ukp.inception.support.uima.WebAnnoCasUtil.getRealCas;
import static org.apache.uima.cas.impl.Serialization.serializeCASComplete;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.List;

import org.apache.uima.UIMAException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.impl.CASCompleteSerializer;
import org.apache.uima.cas.impl.CASImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.diag.repairs.Repair.Safe;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.support.logging.LogMessage;
import de.tudarmstadt.ukp.inception.support.uima.WebAnnoCasUtil;

/**
 * Ensures that the CAS is up-to-date with the project type system. It performs the same operation
 * which is regularly performed when a user opens a document for annotation/curation.
 */
@Safe(true)
public class UpgradeCasRepair
    implements Repair
{
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final AnnotationSchemaService annotationService;

    public UpgradeCasRepair(AnnotationSchemaService aAnnotationService)
    {
        annotationService = aAnnotationService;
    }

    @Override
    public void repair(SourceDocument aDocument, String aDataOwner, CAS aCas,
            List<LogMessage> aMessages)
    {
        try {
            var casImpl = (CASImpl) getRealCas(aCas);

            var annotationCountsBefore = countFeatureStructures(casImpl);
            var bytesBefore = size(serializeCASComplete(casImpl));
            int bytesAfter;

            annotationService.upgradeCas(aCas, aDocument.getProject());
            aMessages.add(LogMessage.info(this, "CAS upgraded."));

            var annotationCountsAfter = countFeatureStructures(casImpl);
            bytesAfter = size(serializeCASComplete(casImpl));

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
        catch (UIMAException | IOException e) {
            LOG.error("Unabled to access CAS", e);
            aMessages.add(LogMessage.error(this, "Unabled to access CAS: %s", e.getMessage()));
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
