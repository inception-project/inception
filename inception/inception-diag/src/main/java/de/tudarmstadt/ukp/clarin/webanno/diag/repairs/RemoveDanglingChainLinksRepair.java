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

import static org.apache.uima.fit.util.CasUtil.getType;

import java.util.ArrayList;
import java.util.List;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.util.FSUtil;
import org.apache.uima.jcas.tcas.Annotation;

import de.tudarmstadt.ukp.clarin.webanno.diag.repairs.Repair.Safe;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.support.WebAnnoConst;
import de.tudarmstadt.ukp.inception.support.logging.LogLevel;
import de.tudarmstadt.ukp.inception.support.logging.LogMessage;

@Safe(false)
public class RemoveDanglingChainLinksRepair
    implements Repair
{
    private final AnnotationSchemaService annotationService;

    public RemoveDanglingChainLinksRepair(AnnotationSchemaService aAnnotationService)
    {
        annotationService = aAnnotationService;
    }

    @Override
    public void repair(SourceDocument aDocument, String aDataOwner, CAS aCas,
            List<LogMessage> aMessages)
    {
        for (var layer : annotationService.listAnnotationLayer(aDocument.getProject())) {
            if (!WebAnnoConst.CHAIN_TYPE.equals(layer.getType())) {
                continue;
            }

            var chains = aCas.select(getType(aCas, layer.getName() + "Chain"));
            var links = new ArrayList<>(
                    aCas.<Annotation> select(getType(aCas, layer.getName() + "Link")).asList());

            for (var chain : chains) {
                var link = FSUtil.getFeature(chain, "first", AnnotationFS.class);

                while (link != null) {
                    links.remove(link);
                    link = FSUtil.getFeature(link, "next", AnnotationFS.class);
                }
            }

            // Delete those relations that pointed to deleted spans
            if (!links.isEmpty()) {
                links.forEach(aCas::removeFsFromIndexes);
                aMessages.add(new LogMessage(this, LogLevel.INFO,
                        "Removed [%d] dangling links in layer [" + layer.getName() + "].",
                        links.size()));
            }
        }
    }
}
