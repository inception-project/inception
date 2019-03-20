/*
 * Copyright 2016
 * Ubiquitous Knowledge Processing (UKP) Lab and FG Language Technology
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
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
import static org.apache.uima.fit.util.CasUtil.selectFS;

import java.util.ArrayList;
import java.util.List;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.util.FSUtil;
import org.springframework.beans.factory.annotation.Autowired;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.support.logging.LogLevel;
import de.tudarmstadt.ukp.clarin.webanno.support.logging.LogMessage;

public class LinksReachableThroughChainsCheck
    implements Check
{
    private @Autowired AnnotationSchemaService annotationService;

    @Override
    public boolean check(Project aProject, CAS aCas, List<LogMessage> aMessages)
    {
        boolean ok = true;
        for (AnnotationLayer layer : annotationService.listAnnotationLayer(aProject)) {
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
            
            List<FeatureStructure> chains = new ArrayList<>(selectFS(aCas, chainType));
            List<AnnotationFS> links = new ArrayList<>(select(aCas, linkType));

            for (FeatureStructure chain : chains) {
                AnnotationFS link = FSUtil.getFeature(chain, "first", AnnotationFS.class);
                
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
