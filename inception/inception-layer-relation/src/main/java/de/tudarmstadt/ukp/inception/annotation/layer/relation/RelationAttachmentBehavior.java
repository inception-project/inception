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
package de.tudarmstadt.ukp.inception.annotation.layer.relation;

import static org.apache.uima.fit.util.CasUtil.getType;
import static org.apache.uima.fit.util.CasUtil.selectCovered;

import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.text.AnnotationFS;
import org.springframework.core.annotation.Order;

import de.tudarmstadt.ukp.inception.annotation.layer.relation.api.CreateRelationAnnotationRequest;
import de.tudarmstadt.ukp.inception.annotation.layer.relation.api.RelationAdapter;
import de.tudarmstadt.ukp.inception.annotation.layer.relation.behavior.RelationLayerBehavior;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@code AnnotationServiceAutoConfiguration#relationAttachmentBehavior}.
 * </p>
 */
@Order(10)
public class RelationAttachmentBehavior
    extends RelationLayerBehavior
{
    @Override
    public CreateRelationAnnotationRequest onCreate(RelationAdapter aAdapter,
            CreateRelationAnnotationRequest aRequest)
    {
        if (aAdapter.getLayer().getAttachFeature() == null) {
            return aRequest;
        }

        // FIXME The code below appears to be broken - it basically only works because the only
        // case were we use the attach-feature is with the Dependency/Token layers and it works
        // because tokens cannot stack. "selectCovered" below would need to be replaced with
        // an actual lookup of the annotation pointed to by the attach-feature.
        var cas = aRequest.getCas();
        var attachType = getType(cas, aAdapter.getLayer().getAttachType().getName());
        var originFS = aRequest.getOriginFs();
        var targetFS = aRequest.getTargetFs();
        targetFS = selectCovered(cas, attachType, targetFS.getBegin(), targetFS.getEnd()).get(0);
        originFS = selectCovered(cas, attachType, originFS.getBegin(), originFS.getEnd()).get(0);
        return aRequest.changeRelation(originFS, targetFS);
    }

    public static FeatureStructure[] resolve(RelationAdapter aAdapter, AnnotationFS aRelation)
    {
        var type = aRelation.getType();
        var targetFeature = type.getFeatureByBaseName(aAdapter.getTargetFeatureName());
        var sourceFeature = type.getFeatureByBaseName(aAdapter.getSourceFeatureName());

        FeatureStructure targetFs;
        FeatureStructure sourceFs;

        if (aAdapter.getAttachFeatureName() != null) {
            var spanType = getType(aRelation.getCAS(), aAdapter.getAttachTypeName());
            var arcSpanFeature = spanType.getFeatureByBaseName(aAdapter.getAttachFeatureName());
            targetFs = aRelation.getFeatureValue(targetFeature).getFeatureValue(arcSpanFeature);
            sourceFs = aRelation.getFeatureValue(sourceFeature).getFeatureValue(arcSpanFeature);
        }
        else {
            targetFs = aRelation.getFeatureValue(targetFeature);
            sourceFs = aRelation.getFeatureValue(sourceFeature);
        }

        return new FeatureStructure[] { sourceFs, targetFs };
    }
}
