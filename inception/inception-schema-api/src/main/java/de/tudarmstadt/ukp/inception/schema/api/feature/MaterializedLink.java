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
package de.tudarmstadt.ukp.inception.schema.api.feature;

import static de.tudarmstadt.ukp.inception.support.uima.ICasUtil.selectAnnotationByAddr;
import static java.util.Collections.emptyList;

import java.util.ArrayList;
import java.util.List;

import org.apache.uima.cas.ArrayFS;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.jcas.tcas.Annotation;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;

public record MaterializedLink(String feature, String role, String targetType, int targetBegin,
        int targetEnd)
{
    public static List<MaterializedLink> toMaterializedLinks(FeatureStructure aLinkHost,
            String aSlotFeature, String aRoleFeature, String aTargetFeature)
    {
        var slotFeature = aLinkHost.getType().getFeatureByBaseName(aSlotFeature);
        if (slotFeature == null) {
            return emptyList();
        }

        var values = aLinkHost.getFeatureValue(slotFeature);
        if (values instanceof ArrayFS array) {
            var links = new ArrayList<MaterializedLink>();
            for (var link : array.toArray()) {
                var roleFeature = link.getType().getFeatureByBaseName(aRoleFeature);
                var targetFeature = link.getType().getFeatureByBaseName(aTargetFeature);
                if (link.getFeatureValue(targetFeature) instanceof Annotation target) {
                    var role = link.getStringValue(roleFeature);
                    var m = new MaterializedLink(aSlotFeature, role, target.getType().getName(),
                            target.getBegin(), target.getEnd());
                    links.add(m);
                }
            }
            return links;
        }

        return emptyList();
    }

    @Override
    public final String toString()
    {
        return "Link [" + feature + ", " + role + ", " + targetType + ", " + targetBegin + ", "
                + targetEnd + "]";
    }

    public static MaterializedLink toMaterializedLink(FeatureStructure aLinkHost,
            AnnotationFeature aSlotFeature, LinkWithRoleModel aLink)
    {
        var cas = aLinkHost.getCAS();
        var linkTarget = selectAnnotationByAddr(cas, aLink.targetAddr);
        return new MaterializedLink(aSlotFeature.getName(), aLink.role,
                linkTarget.getType().getName(), linkTarget.getBegin(), linkTarget.getEnd());
    }
}
