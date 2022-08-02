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
package de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.uima.cas.ArrayFS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.util.FSUtil;

import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.LinkCompareBehavior;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.LinkFeatureDecl;

public abstract class DiffAdapter_ImplBase
    implements DiffAdapter
{
    private final String type;

    private final Set<String> labelFeatures;

    private final List<LinkFeatureDecl> linkFeatures = new ArrayList<>();

    public DiffAdapter_ImplBase(String aType, Set<String> aLabelFeatures)
    {
        type = aType;
        labelFeatures = Collections.unmodifiableSet(new HashSet<>(aLabelFeatures));
    }

    public void addLinkFeature(String aName, String aRoleFeature, String aTargetFeature)
    {
        linkFeatures.add(new LinkFeatureDecl(aName, aRoleFeature, aTargetFeature));
    }

    @Override
    public String getType()
    {
        return type;
    }

    @Override
    public Set<String> getLabelFeatures()
    {
        return labelFeatures;
    }

    @Override
    public LinkFeatureDecl getLinkFeature(String aFeature)
    {
        for (LinkFeatureDecl decl : linkFeatures) {
            if (decl.getName().equals(aFeature)) {
                return decl;
            }
        }
        return null;
    }

    @Override
    public Position getPosition(int aCasId, FeatureStructure aFS)
    {
        return getPosition(aCasId, aFS, null, null, -1, -1, null);
    }

    @Override
    public List<? extends Position> generateSubPositions(int aCasId, AnnotationFS aFs,
            LinkCompareBehavior aLinkCompareBehavior)
    {
        List<Position> subPositions = new ArrayList<>();

        for (LinkFeatureDecl decl : linkFeatures) {
            Feature linkFeature = aFs.getType().getFeatureByBaseName(decl.getName());
            var array = FSUtil.getFeature(aFs, linkFeature, ArrayFS.class);

            if (array == null) {
                continue;
            }

            for (FeatureStructure linkFS : array.toArray()) {
                String role = linkFS.getStringValue(
                        linkFS.getType().getFeatureByBaseName(decl.getRoleFeature()));
                AnnotationFS target = (AnnotationFS) linkFS.getFeatureValue(
                        linkFS.getType().getFeatureByBaseName(decl.getTargetFeature()));
                Position pos = getPosition(aCasId, aFs, decl.getName(), role, target.getBegin(),
                        target.getEnd(), aLinkCompareBehavior);
                subPositions.add(pos);
            }
        }

        return subPositions;
    }
}
