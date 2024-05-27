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
package de.tudarmstadt.ukp.inception.rendering.vmodel;

import static de.tudarmstadt.ukp.inception.support.uima.ICasUtil.getAddr;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.io.Serializable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.text.AnnotationFS;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

/**
 * Visual ID. An ID used in communication with the brat-based annotation editor.
 */
@JsonSerialize(using = ToStringSerializer.class)
public class VID
    implements Serializable
{
    private static final long serialVersionUID = -8490129995678288943L;

    private static final String ID = "ID";
    private static final String SUB = "SUB";
    private static final String ATTR = "ATTR";
    private static final String SLOT = "SLOT";
    private static final String EXTENSION = "EXT";
    private static final String LAYER = "LAYER";
    private static final String EXT_PAYLOAD = "PAYLOAD";

    // @formatter:off
    public static final Pattern PATTERN_EXT = Pattern.compile(
            "(?:(?<EXT>\\w+)\\:)" + 
            "(?<ID>\\d+)" +
            "(?:\\-(?<PAYLOAD>.+))?"
            );
    
    public static final Pattern PATTERN_VID = Pattern.compile(
            "(?<ID>-?\\d+)" +
            "(?:\\-(?<SUB>\\d+))?" +
            "(?:\\.(?<ATTR>\\d+))?" +
            "(?:\\.(?<SLOT>\\d+))?" +
            "(@(?<LAYER>\\d+))?"
            );
    // @formatter:on

    public static final int NONE = -1;

    public static final VID NONE_ID = new VID(NONE);

    @Deprecated
    private final long layerId;
    private final int annotationId;
    private final int subAnnotationId;
    private final int attribute;
    private final int slot;
    private final String extensionId;
    private final String extensionPayload;

    private VID(Builder builder)
    {
        this.layerId = NONE;
        this.annotationId = builder.annotationId;
        this.subAnnotationId = builder.subAnnotationId;
        this.attribute = builder.attribute;
        this.slot = builder.slot;
        this.extensionId = builder.extensionId;
        this.extensionPayload = builder.extensionPayload;
    }

    /**
     * @deprecated Use {@link #builder()}
     */
    @Deprecated
    public VID(FeatureStructure aFS)
    {
        this(getAddr(aFS), NONE, NONE, NONE);
    }

    /**
     * For testing only.
     * 
     * @param aAnnotationID
     *            the annotation ID
     */
    public VID(int aAnnotationID)
    {
        this(aAnnotationID, NONE, NONE, NONE);
    }

    @Deprecated
    public VID(int aAnnotationID, String aExtensionId, String aPayload)
    {
        this(aExtensionId, -1l, aAnnotationID, NONE, NONE, NONE, aPayload);
    }

    @Deprecated
    public VID(AnnotationFS aFS, int aAttribute)
    {
        this(getAddr(aFS), NONE, aAttribute, NONE);
    }

    @Deprecated
    public VID(int aAnnotationID, int aAttribute)
    {
        this(aAnnotationID, NONE, aAttribute, NONE);
    }

    @Deprecated
    public VID(AnnotationFS aFS, int aAttribute, int aSlot)
    {
        this(getAddr(aFS), NONE, aAttribute, aSlot);
    }

    @Deprecated
    public VID(int aAnnotationID, int aAttribute, int aSlot)
    {
        this(aAnnotationID, NONE, aAttribute, aSlot);
    }

    @Deprecated
    public VID(AnnotationFS aFS, int aSubAnnotationId, int aAttribute, int aSlot)
    {
        this(getAddr(aFS), aSubAnnotationId, aAttribute, aSlot);
    }

    @Deprecated
    public VID(String aExtensionId, int aAnnotationID)
    {
        this(aAnnotationID, aExtensionId, null);
    }

    @Deprecated
    public VID(int aAnnotationID, int aSubAnnotationId, int aAttribute, int aSlot)
    {
        this(null, -1l, aAnnotationID, aSubAnnotationId, aAttribute, aSlot, null);
    }

    @Deprecated
    public VID(long aLayerId, int aAnnotationID, int aSubAnnotationId)
    {
        this(null, aLayerId, aAnnotationID, aSubAnnotationId, NONE, NONE, null);
    }

    @Deprecated
    public VID(long aLayerId, int aAnnotationID, int aSubAnnotationId, int aAttribute, int aSlot)
    {
        this(null, aLayerId, aAnnotationID, aSubAnnotationId, aAttribute, aSlot, null);
    }

    @Deprecated
    public VID(String aExtensionId, long aLayerId, int aAnnotationID, int aSubAnnotationId,
            int aAttribute, int aSlot)
    {
        this(aExtensionId, aLayerId, aAnnotationID, aSubAnnotationId, aAttribute, aSlot, null);
    }

    @Deprecated
    public VID(String aExtensionId, long aLayerId, int aAnnotationID, int aSubAnnotationId,
            String aExtensionPayload)
    {
        this(aExtensionId, aLayerId, aAnnotationID, aSubAnnotationId, NONE, NONE,
                aExtensionPayload);
    }

    @Deprecated
    public VID(String aExtensionId, long aLayerId, int aAnnotationID, int aSubAnnotationId,
            int aAttribute, int aSlot, String aExtensionPayload)
    {
        annotationId = aAnnotationID;
        subAnnotationId = aSubAnnotationId;
        attribute = aAttribute;
        slot = aSlot;
        extensionId = aExtensionId;
        layerId = aLayerId;
        extensionPayload = aExtensionPayload;
    }

    public boolean isSet()
    {
        return annotationId >= 0;
    }

    public boolean isNotSet()
    {
        return !isSet();
    }

    @Deprecated
    public long getLayerId()
    {
        return layerId;
    }

    public int getId()
    {
        return annotationId;
    }

    public int getSubId()
    {
        return subAnnotationId;
    }

    public int getAttribute()
    {
        return attribute;
    }

    public int getSlot()
    {
        return slot;
    }

    public boolean isSlotSet()
    {
        return slot >= 0;
    }

    /**
     * @return {@code true} if the annotation referred to does not exist in the CAS. These are
     *         annotations created by editor extensions.
     */
    public boolean isSynthetic()
    {
        return isNotBlank(extensionId);
    }

    /**
     * @return the ID of the editor extension that created the annotation referred to.
     */
    public String getExtensionId()
    {
        return extensionId;
    }

    public String getExtensionPayload()
    {
        return extensionPayload;
    }

    public static VID parseOptional(String aVid)
    {
        if (StringUtils.isNotBlank(aVid)) {
            return parse(aVid);
        }
        else {
            return new VID(NONE);
        }
    }

    public static VID parse(String aVid)
    {
        Matcher m = PATTERN_EXT.matcher(aVid);

        if (m.matches()) {
            String extId = null;
            String extPayload = null;
            int annotationId = Integer.valueOf(m.group(ID));

            if (m.group(EXTENSION) != null) {
                extId = m.group(EXTENSION);
            }
            if (m.group(EXT_PAYLOAD) != null) {
                extPayload = m.group(EXT_PAYLOAD);
            }
            return new VID(annotationId, extId, extPayload);
        }

        m = PATTERN_VID.matcher(aVid);
        if (m.matches()) {
            int annotationId = Integer.valueOf(m.group(ID));
            int subAnnotationId = NONE;
            int feature = NONE;
            int slot = NONE;
            int layerId = NONE;

            if (m.group(SUB) != null) {
                subAnnotationId = Integer.valueOf(m.group(SUB));
            }
            if (m.group(ATTR) != null) {
                feature = Integer.valueOf(m.group(ATTR));
            }
            if (m.group(SLOT) != null) {
                slot = Integer.valueOf(m.group(SLOT));
            }
            if (m.group(LAYER) != null) {
                layerId = Integer.valueOf(m.group(LAYER));
            }
            return new VID(null, layerId, annotationId, subAnnotationId, feature, slot);
        }
        else {
            throw new IllegalArgumentException("Cannot parse visual identifier [" + aVid + "]");
        }
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        if (isNotBlank(extensionId)) {
            sb.append(extensionId);
            sb.append(':');
            sb.append(annotationId);
            if (extensionPayload != null) {
                sb.append("-");
                sb.append(extensionPayload);
            }
            return sb.toString();
        }

        sb.append(annotationId);

        if (subAnnotationId >= 0) {
            sb.append('-');
            sb.append(subAnnotationId);
        }

        if (attribute >= 0) {
            sb.append('.');
            sb.append(attribute);
        }

        if (slot >= 0) {
            sb.append('.');
            sb.append(slot);
        }

        if (layerId >= 0) {
            sb.append('@');
            sb.append(layerId);
        }

        return sb.toString();
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + annotationId;
        result = prime * result + attribute;
        result = prime * result + ((extensionId == null) ? 0 : extensionId.hashCode());
        result = prime * result + ((layerId == NONE) ? 0 : extensionId.hashCode());
        result = prime * result + slot;
        result = prime * result + subAnnotationId;
        return result;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        VID other = (VID) obj;
        if (layerId != other.layerId) {
            return false;
        }
        if (annotationId != other.annotationId) {
            return false;
        }
        if (attribute != other.attribute) {
            return false;
        }
        if (extensionId == null) {
            if (other.extensionId != null) {
                return false;
            }
        }
        else if (!extensionId.equals(other.extensionId)) {
            return false;
        }
        if (slot != other.slot) {
            return false;
        }
        if (subAnnotationId != other.subAnnotationId) {
            return false;
        }
        return true;
    }

    public static VID of(FeatureStructure aFS)
    {
        return new VID(aFS);
    }

    public static Builder builder()
    {
        return new Builder();
    }

    public static final class Builder
    {
        private int annotationId = NONE;
        private int subAnnotationId = NONE;
        private int attribute = NONE;
        private int slot = NONE;
        private String extensionId;
        private String extensionPayload;

        private Builder()
        {
        }

        public Builder forAnnotation(AnnotationFS aAnnotation)
        {
            withAnnotationId(getAddr(aAnnotation));
            return this;
        }

        public Builder withAnnotationId(int aAnnotationId)
        {
            this.annotationId = aAnnotationId;
            return this;
        }

        public Builder withSubAnnotationId(int aSubAnnotationId)
        {
            this.subAnnotationId = aSubAnnotationId;
            return this;
        }

        public Builder withAttribute(int aAttribute)
        {
            this.attribute = aAttribute;
            return this;
        }

        public Builder withSlot(int aSlot)
        {
            this.slot = aSlot;
            return this;
        }

        public Builder withExtensionId(String aExtensionId)
        {
            this.extensionId = aExtensionId;
            return this;
        }

        public Builder withExtensionPayload(String aExtensionPayload)
        {
            this.extensionPayload = aExtensionPayload;
            return this;
        }

        public VID build()
        {
            return new VID(this);
        }
    }
}
