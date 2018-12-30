/*
 * Copyright 2018
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
package de.tudarmstadt.ukp.clarin.webanno.api.annotation.adapter;

import static org.apache.uima.fit.util.CasUtil.selectCovered;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.util.CasUtil;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.exception.AnnotationException;

public class ChainStackingBehavior
    extends SpanStackingBehavior
{
    @Override
    public CreateSpanAnnotationRequest onCreate(TypeAdapter aAdapter,
            CreateSpanAnnotationRequest aRequest)
        throws AnnotationException
    {
        final CAS aCas = aRequest.getJcas().getCas();
        final int aBegin = aRequest.getBegin();
        final int aEnd = aRequest.getEnd();

        // If stacking is not allowed and there already is an annotation, then return the address
        // of the existing annotation.
        Type type = CasUtil.getType(aCas, aAdapter.getLayer().getName() + ChainAdapter.LINK);
        for (AnnotationFS fs : selectCovered(aCas, type, aBegin, aEnd)) {
            if (fs.getBegin() == aBegin && fs.getEnd() == aEnd) {
                if (!aAdapter.getLayer().isAllowStacking()) {
                    throw new AnnotationException("Cannot create another annotation of layer ["
                            + aAdapter.getLayer().getUiName()
                            + "] at this location - stacking is not " + "enabled for this layer.");
                }
            }
        }

        return aRequest;
    }
}
