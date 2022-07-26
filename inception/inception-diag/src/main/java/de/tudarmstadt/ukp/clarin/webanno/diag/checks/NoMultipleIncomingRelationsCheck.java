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

import static de.tudarmstadt.ukp.clarin.webanno.support.WebAnnoConst.FEAT_REL_SOURCE;
import static de.tudarmstadt.ukp.clarin.webanno.support.WebAnnoConst.FEAT_REL_TARGET;
import static de.tudarmstadt.ukp.clarin.webanno.support.WebAnnoConst.RELATION_TYPE;
import static org.apache.uima.fit.util.CasUtil.getType;
import static org.apache.uima.fit.util.CasUtil.select;
import static org.apache.uima.fit.util.FSUtil.getFeature;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.support.logging.LogMessage;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.Dependency;
import de.tudarmstadt.ukp.inception.schema.AnnotationSchemaService;

public class NoMultipleIncomingRelationsCheck
    implements Check
{
    private final AnnotationSchemaService annotationService;

    public NoMultipleIncomingRelationsCheck(AnnotationSchemaService aAnnotationService)
    {
        annotationService = aAnnotationService;
    }

    @Override
    public boolean check(Project aProject, CAS aCas, List<LogMessage> aMessages)
    {
        if (annotationService == null) {
            return true;
        }

        List<AnnotationLayer> allAnnoLayers = annotationService.listAnnotationLayer(aProject);
        if (CollectionUtils.isEmpty(allAnnoLayers)) {
            return true;
        }

        boolean ok = true;
        for (AnnotationLayer layer : allAnnoLayers) {

            if (!RELATION_TYPE.equals(layer.getType())) {
                continue;
            }

            if (!Dependency.class.getName().equals(layer.getName())) {
                continue;
            }

            Type type;
            try {
                type = getType(aCas, layer.getName());
            }
            catch (IllegalArgumentException e) {
                // If the type does not exist, the CAS has not been upgraded. In this case, we
                // can skip checking the layer because there will be no annotations anyway.
                continue;
            }

            // Remember all nodes that already have a known incoming relation.
            // Map from the target to the existing source, so the source can be used
            // to provide a better debugging output.
            Map<AnnotationFS, AnnotationFS> incoming = new HashMap<>();

            for (AnnotationFS rel : select(aCas, type)) {

                AnnotationFS source = getFeature(rel, FEAT_REL_SOURCE, AnnotationFS.class);
                AnnotationFS target = getFeature(rel, FEAT_REL_TARGET, AnnotationFS.class);

                AnnotationFS existingSource = incoming.get(target);
                if (existingSource != null) {

                    // Debug output should include sentence number to make the orientation
                    // easier
                    Optional<Integer> sentenceNumber = Optional.empty();
                    try {
                        sentenceNumber = Optional.of(WebAnnoCasUtil
                                .getSentenceNumber(target.getCAS(), target.getBegin()));
                    }
                    catch (IndexOutOfBoundsException e) {
                        // ignore this error and don't output sentence number
                        sentenceNumber = Optional.empty();
                    }

                    if (sentenceNumber.isPresent()) {
                        aMessages.add(LogMessage.warn(this,
                                "Sentence %d: Relation [%s] -> [%s] points to span that already has an "
                                        + "incoming relation [%s] -> [%s].",
                                sentenceNumber.get(), source.getCoveredText(),
                                target.getCoveredText(), existingSource.getCoveredText(),
                                target.getCoveredText()));
                    }
                    else {

                        aMessages.add(LogMessage.warn(this,
                                "Relation [%s] -> [%s] points to span that already has an "
                                        + "incoming relation [%s] -> [%s].",
                                source.getCoveredText(), target.getCoveredText(),
                                existingSource.getCoveredText(), target.getCoveredText()));
                    }

                    // This check only logs warnings - it should not fail. Having multiple
                    // incoming edges is not a serious problem.
                    // ok = false;
                }
                else {
                    incoming.put(target, source);
                }
            }
        }
        return ok;
    }
}
