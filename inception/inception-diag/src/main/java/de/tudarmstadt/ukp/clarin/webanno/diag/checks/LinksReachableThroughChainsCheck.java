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

import static org.apache.uima.fit.util.CasUtil.getType;
import static org.apache.uima.fit.util.CasUtil.select;

import java.util.ArrayList;
import java.util.List;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.util.FSUtil;

import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.support.WebAnnoConst;
import de.tudarmstadt.ukp.inception.support.logging.LogLevel;
import de.tudarmstadt.ukp.inception.support.logging.LogMessage;

public class LinksReachableThroughChainsCheck
    implements Check
{
    private final AnnotationSchemaService annotationService;

    public LinksReachableThroughChainsCheck(AnnotationSchemaService aAnnotationService)
    {
        annotationService = aAnnotationService;
    }

    @Override
    public boolean check(SourceDocument aDocument, String aDataOwner, CAS aCas,
            List<LogMessage> aMessages)
    {
        boolean ok = true;
        for (var layer : annotationService.listAnnotationLayer(aDocument.getProject())) {
            if (!WebAnnoConst.CHAIN_TYPE.equals(layer.getType())) {
                continue;
            }

            Type chainType;
            Type linkType;

            try {
                chainType = getType(aCas, layer.getName() + "Chain");
                linkType = getType(aCas, layer.getName() + "Link");
            }
            catch (IllegalArgumentException e) {
                // This happens if the types do not (yet) exist in the CAS because the types are
                // new and the CAS has not been upgraded yet. In this case, we can just ignore the
                // check
                continue;
            }

            var chains = aCas.select(chainType).asList();
            var links = new ArrayList<>(select(aCas, linkType));

            for (var chain : chains) {
                var link = FSUtil.getFeature(chain, "first", AnnotationFS.class);

                while (link != null) {
                    links.remove(link);
                    link = FSUtil.getFeature(link, "next", AnnotationFS.class);
                }
            }

            if (!links.isEmpty()) {
                ok = false;

                aMessages.add(new LogMessage(this, LogLevel.ERROR,
                        "CoreferenceLinks not reachable through chains: %d", links.size()));
                for (AnnotationFS link : links) {
                    aMessages.add(new LogMessage(this, LogLevel.ERROR,
                            "Unreachable CoreferenceLink [%s]", link));
                }
            }
        }

        return ok;
    }
}
