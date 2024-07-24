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

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import de.tudarmstadt.ukp.inception.annotation.feature.link.LinkFeatureMultiplicityMode;

public abstract class Position_ImplBase
    implements Position
{
    private static final long serialVersionUID = -1237180459049008357L;

    private final String type;
    private final String feature;

    private final String role;

    private final int linkTargetBegin;
    private final int linkTargetEnd;
    private final String linkTargetText;

    private final LinkFeatureMultiplicityMode linkCompareBehavior;

    private final String collectionId;
    private final String documentId;

    public Position_ImplBase(String aCollectionId, String aDocumentId, String aType,
            String aFeature, String aRole, int aLinkTargetBegin, int aLinkTargetEnd,
            String aLinkTargetText, LinkFeatureMultiplicityMode aBehavior)
    {
        type = aType;
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
    public LinkFeatureMultiplicityMode getLinkCompareBehavior()
    {
        return linkCompareBehavior;
    }

    @Override
    public int compareTo(Position aOther)
    {
        int typeCmp = type.compareTo(aOther.getType());
        if (typeCmp != 0) {
            return typeCmp;
        }

        int featureCmp = ObjectUtils.compare(feature, aOther.getFeature());
        if (featureCmp != 0) {
            return featureCmp;
        }

        int linkCmpCmp = ObjectUtils.compare(linkCompareBehavior, aOther.getLinkCompareBehavior());
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
            case ONE_TARGET_MULTIPLE_ROLES:
                // Include role into position
                return ObjectUtils.compare(role, aOther.getRole());
            case MULTIPLE_TARGETS_ONE_ROLE:
                // Include target into position
                if (linkTargetBegin != aOther.getLinkTargetBegin()) {
                    return linkTargetBegin - aOther.getLinkTargetBegin();
                }

                return linkTargetEnd - aOther.getLinkTargetEnd();
            case MULTIPLE_TARGETS_MULTIPLE_ROLES:
                var roleCmp = ObjectUtils.compare(role, aOther.getRole());
                if (roleCmp != 0) {
                    return roleCmp;
                }

                // Include role and target into position
                if (linkTargetBegin != aOther.getLinkTargetBegin()) {
                    return linkTargetBegin - aOther.getLinkTargetBegin();
                }

                return linkTargetEnd - aOther.getLinkTargetEnd();
            default:
                throw new IllegalStateException(
                        "Unknown link target comparison mode [" + linkCompareBehavior + "]");
            }
        }
        else {
            return linkCmpCmp;
        }
    }

    protected void toStringFragment(StringBuilder builder)
    {
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
            case ONE_TARGET_MULTIPLE_ROLES:
                builder.append(", role=");
                builder.append(getRole());
                break;
            case MULTIPLE_TARGETS_ONE_ROLE:
                builder.append(", linkTarget=(");
                builder.append(getLinkTargetBegin()).append('-').append(getLinkTargetEnd());
                builder.append(')');
                builder.append('[').append(linkTargetText).append(']');
                break;
            case MULTIPLE_TARGETS_MULTIPLE_ROLES:
                builder.append(", role=");
                builder.append(getRole());
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
