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

import static de.tudarmstadt.ukp.inception.support.WebAnnoConst.RELATION_TYPE;
import static org.apache.uima.fit.util.CasUtil.getType;
import static org.apache.uima.fit.util.CasUtil.select;
import static org.apache.uima.fit.util.FSUtil.getFeature;

import java.util.List;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;

import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.support.WebAnnoConst;
import de.tudarmstadt.ukp.inception.support.logging.LogLevel;
import de.tudarmstadt.ukp.inception.support.logging.LogMessage;

/**
 * Checks that the offsets of relations match the target of the relation. This mirrors the DKPro
 * Core convention that the offsets of a dependency relation must match the offsets of the
 * dependent.
 */
public class RelationOffsetsCheck
    implements Check
{
    private final AnnotationSchemaService annotationService;

    public RelationOffsetsCheck(AnnotationSchemaService aAnnotationService)
    {
        annotationService = aAnnotationService;
    }

    @Override
    public boolean check(SourceDocument aDocument, String aDataOwner, CAS aCas,
            List<LogMessage> aMessages)
    {
        boolean ok = true;

        for (var layer : annotationService.listAnnotationLayer(aDocument.getProject())) {
            if (!RELATION_TYPE.equals(layer.getType())) {
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

            for (var rel : select(aCas, type)) {
                var target = getFeature(rel, WebAnnoConst.FEAT_REL_TARGET, AnnotationFS.class);
                if ((rel.getBegin() != target.getBegin()) || (rel.getEnd() != target.getEnd())) {
                    aMessages.add(new LogMessage(this, LogLevel.ERROR,
                            "Relation offsets [%d,%d] to not match target offsets [%d,%d]",
                            rel.getBegin(), rel.getEnd(), target.getBegin(), target.getEnd()));
                    ok = false;
                }
            }
        }

        return ok;
    }
}
