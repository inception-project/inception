/*
 * Copyright 2019
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
package de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.api;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.LinkCompareBehavior;

public abstract class Position_ImplBase implements Position
{
    private final String type;
    private final int casId;
    private final String feature;

    private final String role;
    
    private final int linkTargetBegin;
    private final int linkTargetEnd;
    private final String linkTargetText;

    private final LinkCompareBehavior linkCompareBehavior;
    
    private final String collectionId;
    private final String documentId;

    public Position_ImplBase(String aCollectionId, String aDocumentId, int aCasId,
            String aType, String aFeature, String aRole, int aLinkTargetBegin,
            int aLinkTargetEnd, String aLinkTargetText, LinkCompareBehavior aBehavior)
    {
        type = aType;
        casId = aCasId;
        feature = aFeature;

        linkCompareBehavior = aBehavior;

        role = aRole;
        linkTargetBegin = aLinkTargetBegin;
        linkTargetEnd = aLinkTargetEnd;
        linkTargetText = aLinkTargetText;

        collectionId = aCollectionId;
        documentId = aDocumentId;
    }

    @Override
    public String getType()
    {
        return type;
    }
    
    @Override
    public int getCasId()
    {
        return casId;
    }
    
    @Override
    public String getFeature()
    {
        return feature;
    }
    
    @Override
    public String getRole()
    {
        return role;
    }
    
    @Override
    public int getLinkTargetBegin()
    {
        return linkTargetBegin;
    }
    
    @Override
    public int getLinkTargetEnd()
    {
        return linkTargetEnd;
    }
    
    public String getLinkTargetText()
    {
        return linkTargetText;
    }
    
    @Override
    public String getCollectionId()
    {
        return collectionId;
    }
    
    @Override
    public String getDocumentId()
    {
        return documentId;
    }
    
    @Override
    public LinkCompareBehavior getLinkCompareBehavior()
    {
        return linkCompareBehavior;
    }
    
    @Override
    public int compareTo(Position aOther) {
        if (casId != aOther.getCasId()) {
            return casId - aOther.getCasId();
        }
        
        int typeCmp = type.compareTo(aOther.getType());
        if (typeCmp != 0) {
            return typeCmp;
        }

        int featureCmp = ObjectUtils.compare(feature, aOther.getFeature());
        if (featureCmp != 0) {
            return featureCmp;
        }

        int linkCmpCmp = ObjectUtils.compare(linkCompareBehavior,
                aOther.getLinkCompareBehavior());
        if (linkCmpCmp != 0) {
            // If the linkCompareBehavior is not the same, then we are dealing with different
            // positions
            return linkCmpCmp;
        }
        
        // If linkCompareBehavior is equal, then we still only have to continue if it is non-
        // null.
        else if (linkCompareBehavior != null) {
            // If we are dealing with sub-positions generated for link features, then we need to
            // check this, otherwise linkTargetBegin, linkTargetEnd, linkCompareBehavior,
            // feature and role are all unset.
            switch (linkCompareBehavior) {
            case LINK_TARGET_AS_LABEL:
                // Include role into position
                return ObjectUtils.compare(role, aOther.getRole());
            case LINK_ROLE_AS_LABEL:
                // Include target into position
                if (linkTargetBegin != aOther.getLinkTargetBegin()) {
                    return linkTargetBegin - aOther.getLinkTargetBegin();
                }
                
                return linkTargetEnd - aOther.getLinkTargetEnd();
            default:
                throw new IllegalStateException("Unknown link target comparison mode ["
                        + linkCompareBehavior + "]");
            }
        }
        else {
            return linkCmpCmp;
        }
    }
    
    protected void toStringFragment(StringBuilder builder)
    {
        builder.append("cas=");
        builder.append(getCasId());
        if (getCollectionId() != null) {
            builder.append(", coll=");
            builder.append(getCollectionId());
        }
        if (getDocumentId() != null) {
            builder.append(", doc=");
            builder.append(getDocumentId());
        }
        builder.append(", type=");
        if (getType().contains(".")) {
            builder.append(StringUtils.substringAfterLast(getType(), "."));
        }
        else {
            builder.append(getType());
        }
        if (getFeature() != null) {
            builder.append(", linkFeature=");
            builder.append(getFeature());
            switch (getLinkCompareBehavior()) {
            case LINK_TARGET_AS_LABEL:
                builder.append(", role=");
                builder.append(getRole());
                break;
            case LINK_ROLE_AS_LABEL:
                builder.append(", linkTarget=(");
                builder.append(getLinkTargetBegin()).append('-').append(getLinkTargetEnd());
                builder.append(')');
                builder.append('[').append(linkTargetText).append(']');
                break;
            default:
                builder.append(", BAD LINK BEHAVIOR");
            }
        }
    }
}
