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
import static org.apache.uima.fit.util.FSUtil.getFeature;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.jcas.JCas;
import org.springframework.beans.factory.annotation.Autowired;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil;
import de.tudarmstadt.ukp.clarin.webanno.diag.CasDoctor.LogLevel;
import de.tudarmstadt.ukp.clarin.webanno.diag.CasDoctor.LogMessage;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;

public class NoMultipleIncomingRelationsCheck
    implements Check
{
    private @Autowired AnnotationSchemaService annotationService;

    @Override
    public boolean check(Project aProject, CAS aCas, List<LogMessage> aMessages)
    {
        boolean ok = true;
        if (annotationService == null) {
            return ok;
        }
        List<AnnotationLayer> allAnnoLayers = annotationService.listAnnotationLayer(aProject);
        if (allAnnoLayers != null) {
            for (AnnotationLayer layer : allAnnoLayers) {

                if (!WebAnnoConst.RELATION_TYPE.equals(layer.getType())) {
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

                    AnnotationFS source = getFeature(rel, WebAnnoConst.FEAT_REL_SOURCE,
                            AnnotationFS.class);
                    AnnotationFS target = getFeature(rel, WebAnnoConst.FEAT_REL_TARGET,
                            AnnotationFS.class);

                    AnnotationFS existingSource = incoming.get(target);
                    if (existingSource != null) {

                        // Debug output should include sentence number to make the orientation
                        // easier
                        Optional<Integer> sentenceNumber = Optional.empty();
                        try {
                            JCas jcas;
                            jcas = target.getCAS().getJCas();

                            sentenceNumber = Optional
                                    .of(WebAnnoCasUtil.getSentenceNumber(jcas, target.getBegin()));
                        }
                        catch (CASException | IndexOutOfBoundsException e) {
                            // ignore this error and don't output sentence number
                            sentenceNumber = Optional.empty();
                        }

                        if (sentenceNumber.isPresent()) {
                            aMessages.add(new LogMessage(this, LogLevel.ERROR,
                                    "Sentence %d: Relation [%s] -> [%s] points to span that already has an "
                                            + "incoming relation [%s] -> [%s].",
                                    sentenceNumber.get(), source.getCoveredText(),
                                    target.getCoveredText(), existingSource.getCoveredText(),
                                    target.getCoveredText()));
                        }
                        else {

                            aMessages.add(new LogMessage(this, LogLevel.ERROR,
                                    "Relation [%s] -> [%s] points to span that already has an "
                                            + "incoming relation [%s] -> [%s].",
                                    source.getCoveredText(), target.getCoveredText(),
                                    existingSource.getCoveredText(), target.getCoveredText()));
                        }
                        ok = false;
                    }
                    else {
                        incoming.put(target, source);
                    }
                }
            }
        }

        return ok;
    }
}
