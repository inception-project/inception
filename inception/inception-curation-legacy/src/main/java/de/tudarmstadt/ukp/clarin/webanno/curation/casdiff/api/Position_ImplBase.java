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
    private final String feature;

    private final LinkFeatureMultiplicityMode linkCompareBehavior;

    private final String role;

    private final int linkTargetBegin;
    private final int linkTargetEnd;

    // BEGIN: For debugging only - not included in compareTo/hashCode/equals!
    private final String collectionId;
    private final String documentId;
    private final String linkTargetText;
    // END: For debugging only - not included in compareTo/hashCode/equals!

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

        collectionId = aCollectionId;
        documentId = aDocumentId;
        linkTargetText = aLinkTargetText;
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
    public boolean isLinkFeaturePosition()
    {
        return getFeature() != null;
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
        // int collectionIdCmp = collectionId.compareTo(aOther.getCollectionId());
        // if (collectionIdCmp != 0) {
        // return collectionIdCmp;
        // }
        //
        // int documentIdCmp = documentId.compareTo(aOther.getDocumentId());
        // if (documentIdCmp != 0) {
        // return documentIdCmp;
        // }

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
        if (linkCompareBehavior == null) {
            return linkCmpCmp;
        }

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

    @Override
    public boolean equals(final Object other)
    {
        if (!(other instanceof Position_ImplBase)) {
            return false;
        }

        Position_ImplBase castOther = (Position_ImplBase) other;
        var result = // Objects.equals(collectionId, castOther.collectionId) //
                // && Objects.equals(documentId, castOther.documentId) //
                Objects.equals(type, castOther.type) //
                        && Objects.equals(feature, castOther.feature) //
                        && Objects.equals(linkCompareBehavior, castOther.linkCompareBehavior);

        // If the base properties are equal, then we have to continue only linkCompareBehavior if it
        // is non-null.
        if (!result && linkCompareBehavior == null) {
            return false;
        }

        switch (linkCompareBehavior) {
        case ONE_TARGET_MULTIPLE_ROLES:
            // Include role into position
            return Objects.equals(role, castOther.role);
        case MULTIPLE_TARGETS_ONE_ROLE:
            // Include target into position
            return Objects.equals(linkTargetBegin, castOther.linkTargetBegin) //
                    && Objects.equals(linkTargetEnd, castOther.linkTargetEnd);
        case MULTIPLE_TARGETS_MULTIPLE_ROLES:
            return Objects.equals(role, castOther.role) //
                    && Objects.equals(linkTargetBegin, castOther.linkTargetBegin) //
                    && Objects.equals(linkTargetEnd, castOther.linkTargetEnd);
        default:
            throw new IllegalStateException(
                    "Unknown link target comparison mode [" + linkCompareBehavior + "]");
        }
    }

    @Override
    public int hashCode()
    {
        var builder = new HashCodeBuilder() //
                // .append(collectionId) //
                // .append(documentId) //
                .append(type) //
                .append(feature) //
                .append(linkCompareBehavior);

        if (linkCompareBehavior == null) {
            return builder.toHashCode();
        }

        switch (linkCompareBehavior) {
        case ONE_TARGET_MULTIPLE_ROLES:
            // Include role into position
            builder.append(role);
            break;
        case MULTIPLE_TARGETS_ONE_ROLE:
            // Include target into position
            builder.append(linkTargetBegin);
            builder.append(linkTargetEnd);
            break;
        case MULTIPLE_TARGETS_MULTIPLE_ROLES:
            builder.append(role);
            builder.append(linkTargetBegin);
            builder.append(linkTargetEnd);
            break;
        default:
            throw new IllegalStateException(
                    "Unknown link target comparison mode [" + linkCompareBehavior + "]");
        }

        return builder.toHashCode();
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
