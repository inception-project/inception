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
package de.tudarmstadt.ukp.inception.curation.api;

import static java.util.Collections.unmodifiableSet;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.tudarmstadt.ukp.inception.annotation.feature.link.LinkFeatureDiffMode;
import de.tudarmstadt.ukp.inception.annotation.feature.link.LinkFeatureMultiplicityMode;

public abstract class DiffAdapter_ImplBase
    implements DiffAdapter
{
    private final String type;

    private final Set<String> features;

    private final Map<String, LinkFeatureDecl> linkFeatures = new LinkedHashMap<>();

    public DiffAdapter_ImplBase(String aType, Set<String> aFeatures)
    {
        type = aType;
        features = unmodifiableSet(new HashSet<>(aFeatures));
    }

    @Override
    public void addLinkFeature(String aName, String aRoleFeature, String aTargetFeature,
            LinkFeatureMultiplicityMode aCompareBehavior, LinkFeatureDiffMode aDiffMode)
    {
        linkFeatures.put(aName, new LinkFeatureDecl(aName, aRoleFeature, aTargetFeature,
                aCompareBehavior, aDiffMode));
    }

    @Override
    public String getType()
    {
        return type;
    }

    @Override
    public Set<String> getFeatures()
    {
        return features;
    }

    @Override
    public Set<String> getLinkFeatures()
    {
        return linkFeatures.keySet();
    }

    public List<LinkFeatureDecl> getLinkFeaturesDecls()
    {
        return new ArrayList<>(linkFeatures.values());
    }

    @Override
    public LinkFeatureDecl getLinkFeature(String aFeature)
    {
        for (var decl : linkFeatures.values()) {
            if (decl.getName().equals(aFeature)) {
                return decl;
            }
        }
        return null;
    }

    @Override
    public boolean isIncludeInDiff(String aFeature)
    {
        var decl = getLinkFeature(aFeature);
        if (decl == null) {
            return false;
        }
        return decl.getDiffMode() == LinkFeatureDiffMode.INCLUDE;
    }
}
