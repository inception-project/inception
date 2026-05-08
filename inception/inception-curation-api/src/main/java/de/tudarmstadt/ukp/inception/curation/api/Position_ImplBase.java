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

import java.util.Objects;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import de.tudarmstadt.ukp.inception.annotation.feature.link.LinkFeatureMultiplicityMode;

public abstract class Position_ImplBase
    implements Position
{
    private static final long serialVersionUID = -1237180459049008357L;

    private final String type;

    private final String linkFeature;
    private final LinkFeatureMultiplicityMode linkFeatureMultiplicityMode;
    private final String linkRole;
    private final int linkTargetBegin;
    private final int linkTargetEnd;

    // BEGIN: For debugging only - not included in compareTo/hashCode/equals!
    private final String collectionId;
    private final String documentId;
    private final String linkTargetText;
    // END: For debugging only - not included in compareTo/hashCode/equals!

    public Position_ImplBase(String aCollectionId, String aDocumentId, String aType)
    {
        type = aType;
        collectionId = aCollectionId;
        documentId = aDocumentId;

        linkFeature = null;
        linkRole = null;
        linkFeatureMultiplicityMode = null;
        linkTargetBegin = -1;
        linkTargetEnd = -1;
        linkTargetText = null;

    }

    public Position_ImplBase(String aCollectionId, String aDocumentId, String aType,
            String aFeature, String aRole, int aLinkTargetBegin, int aLinkTargetEnd,
            String aLinkTargetText, LinkFeatureMultiplicityMode aBehavior)
    {
        type = aType;
        collectionId = aCollectionId;
        documentId = aDocumentId;

        linkFeature = aFeature;
        linkRole = aRole;
        linkFeatureMultiplicityMode = aBehavior;
        linkTargetBegin = aLinkTargetBegin;
        linkTargetEnd = aLinkTargetEnd;
        linkTargetText = aLinkTargetText;
    }

    @Override
    public String getType()
    {
        return type;
    }

    @Override
    public String getLinkFeature()
    {
        return linkFeature;
    }

    @Override
    public String getLinkRole()
    {
        return linkRole;
    }

    @Override
    public boolean isLinkFeaturePosition()
    {
        return getLinkFeature() != null;
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
    public LinkFeatureMultiplicityMode getLinkFeatureMultiplicityMode()
    {
        return linkFeatureMultiplicityMode;
    }

    @Override
    public int compareTo(Position aOther)
    {
        int typeCmp = type.compareTo(aOther.getType());
        if (typeCmp != 0) {
            return typeCmp;
        }

        int featureCmp = ObjectUtils.compare(linkFeature, aOther.getLinkFeature());
        if (featureCmp != 0) {
            return featureCmp;
        }

        int linkCmpCmp = ObjectUtils.compare(linkFeatureMultiplicityMode,
                aOther.getLinkFeatureMultiplicityMode());
        if (linkCmpCmp != 0) {
            // If the linkCompareBehavior is not the same, then we are dealing with different
            // positions
            return linkCmpCmp;
        }

        // If linkCompareBehavior is equal, then we still only have to continue if it is non-
        // null.
        if (linkFeatureMultiplicityMode == null) {
            return linkCmpCmp;
        }

        // If we are dealing with sub-positions generated for link features, then we need to
        // check this, otherwise linkTargetBegin, linkTargetEnd, linkCompareBehavior,
        // feature and role are all unset.
        switch (linkFeatureMultiplicityMode) {
        case ONE_TARGET_MULTIPLE_ROLES:
            // Include role into position
            return ObjectUtils.compare(linkRole, aOther.getLinkRole());
        case MULTIPLE_TARGETS_ONE_ROLE:
            // Include target into position
            if (linkTargetBegin != aOther.getLinkTargetBegin()) {
                return linkTargetBegin - aOther.getLinkTargetBegin();
            }

            return linkTargetEnd - aOther.getLinkTargetEnd();
        case MULTIPLE_TARGETS_MULTIPLE_ROLES:
            var roleCmp = ObjectUtils.compare(linkRole, aOther.getLinkRole());
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
                    "Unknown link target comparison mode [" + linkFeatureMultiplicityMode + "]");
        }
    }

    @Override
    public boolean equals(final Object other)
    {
        if (!(other instanceof Position_ImplBase)) {
            return false;
        }

        Position_ImplBase castOther = (Position_ImplBase) other;
        var result = Objects.equals(type, castOther.type) //
                && Objects.equals(linkFeature, castOther.linkFeature) //
                && Objects.equals(linkFeatureMultiplicityMode,
                        castOther.linkFeatureMultiplicityMode);

        // If the base properties are equal, then we have to continue only linkCompareBehavior if it
        // is non-null.
        if (!result && linkFeatureMultiplicityMode == null) {
            return false;
        }

        switch (linkFeatureMultiplicityMode) {
        case ONE_TARGET_MULTIPLE_ROLES:
            // Include role into position
            return Objects.equals(linkRole, castOther.linkRole);
        case MULTIPLE_TARGETS_ONE_ROLE:
            // Include target into position
            return Objects.equals(linkTargetBegin, castOther.linkTargetBegin) //
                    && Objects.equals(linkTargetEnd, castOther.linkTargetEnd);
        case MULTIPLE_TARGETS_MULTIPLE_ROLES:
            return Objects.equals(linkRole, castOther.linkRole) //
                    && Objects.equals(linkTargetBegin, castOther.linkTargetBegin) //
                    && Objects.equals(linkTargetEnd, castOther.linkTargetEnd);
        default:
            throw new IllegalStateException(
                    "Unknown link target comparison mode [" + linkFeatureMultiplicityMode + "]");
        }
    }

    @Override
    public int hashCode()
    {
        var builder = new HashCodeBuilder() //
                .append(type) //
                .append(linkFeature) //
                .append(linkFeatureMultiplicityMode);

        if (linkFeatureMultiplicityMode == null) {
            return builder.toHashCode();
        }

        switch (linkFeatureMultiplicityMode) {
        case ONE_TARGET_MULTIPLE_ROLES:
            // Include role into position
            builder.append(linkRole);
            break;
        case MULTIPLE_TARGETS_ONE_ROLE:
            // Include target into position
            builder.append(linkTargetBegin);
            builder.append(linkTargetEnd);
            break;
        case MULTIPLE_TARGETS_MULTIPLE_ROLES:
            builder.append(linkRole);
            builder.append(linkTargetBegin);
            builder.append(linkTargetEnd);
            break;
        default:
            throw new IllegalStateException(
                    "Unknown link target comparison mode [" + linkFeatureMultiplicityMode + "]");
        }

        return builder.toHashCode();
    }

    protected void toStringFragment(StringBuilder builder)
    {
        if (getCollectionId() != null) {
            if (!builder.isEmpty()) {
                builder.append(", ");
            }
            builder.append("coll=");
            builder.append(getCollectionId());
        }

        if (getDocumentId() != null) {
            if (!builder.isEmpty()) {
                builder.append(", ");
            }
            builder.append("doc=");
            builder.append(getDocumentId());
        }

        if (!builder.isEmpty()) {
            builder.append(", ");
        }
        builder.append("type=");

        if (getType().contains(".")) {
            builder.append(StringUtils.substringAfterLast(getType(), "."));
        }
        else {
            builder.append(getType());
        }

        if (getLinkFeature() != null) {
            if (!builder.isEmpty()) {
                builder.append(", ");
            }
            builder.append("linkFeature=");
            builder.append(getLinkFeature());

            if (!builder.isEmpty()) {
                builder.append(", ");
            }
            switch (getLinkFeatureMultiplicityMode()) {
            case ONE_TARGET_MULTIPLE_ROLES:
                builder.append("role=");
                builder.append(getLinkRole());
                break;
            case MULTIPLE_TARGETS_ONE_ROLE:
                builder.append("linkTarget=(");
                builder.append(getLinkTargetBegin()).append('-').append(getLinkTargetEnd());
                builder.append(')');
                builder.append('[').append(linkTargetText).append(']');
                break;
            case MULTIPLE_TARGETS_MULTIPLE_ROLES:
                builder.append("role=");
                builder.append(getLinkRole());
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
