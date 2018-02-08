/*
 * Copyright 2015
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
package de.tudarmstadt.ukp.clarin.webanno.api.annotation.model;

import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.getAddr;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.io.Serializable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
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

    public static final Pattern PATTERN_VID = Pattern.compile(
            "(?:(?<EXT>\\w+)\\:)?" + 
            "(?<ID>-?\\d+)" +
            "(?:\\-(?<SUB>\\d+))?" +
            "(?:\\.(?<ATTR>\\d+))?" +
            "(?:\\.(?<SLOT>\\d+))?" +
            "(@(?<LAYER>\\d+))?"
            );

    public static final int NONE = -1;

    public static final VID NONE_ID = new VID(NONE);

    private final long layerId;
    private final int annotationId;
    private final int subAnnotationId;
    private final int attribute;
    private final int slot;
    private final String extensionId;

    public VID(AnnotationFS aFS)
    {
        this(null, getAddr(aFS), NONE, NONE, NONE);
    }

    public VID(int aAnnotationID)
    {
        this(null, aAnnotationID, NONE, NONE, NONE);
    }

    public VID(AnnotationFS aFS, int aAttribute)
    {
        this(null, getAddr(aFS), NONE, aAttribute, NONE);
    }

    public VID(int aAnnotationID, int aAttribute)
    {
        this(null, aAnnotationID, NONE, aAttribute, NONE);
    }

    public VID(AnnotationFS aFS, int aAttribute, int aSlot)
    {
        this(null, getAddr(aFS), NONE, aAttribute, aSlot);
    }

    public VID(int aAnnotationID, int aAttribute, int aSlot)
    {
        this(null, aAnnotationID, NONE, aAttribute, aSlot);
    }

    public VID(int aAnnotationID, int aSubAnnotationId, int aAttribute, int aSlot)
    {
        this(null, aAnnotationID, aSubAnnotationId, aAttribute, aSlot);
    }

    public VID(AnnotationFS aFS, int aSubAnnotationId, int aAttribute, int aSlot)
    {
        this(null, getAddr(aFS), aSubAnnotationId, aAttribute, aSlot);
    }

    public VID(String aExtensionId, int aAnnotationID)
    {
        this(aExtensionId, aAnnotationID, NONE, NONE, NONE);
    }

    public VID(String aExtensionId, int aAnnotationID, int aAttribute)
    {
        this(aExtensionId, aAnnotationID, NONE, aAttribute, NONE);
    }

    public VID(String aExtensionId, int aAnnotationID, int aAttribute, int aSlot)
    {
        this(aExtensionId, aAnnotationID, NONE, aAttribute, aSlot);
    }

    public VID(String aExtensionId, int aAnnotationID, int aSubAnnotationId, int aAttribute,
            int aSlot)
    {
        this(aExtensionId, -1l, aAnnotationID, aSubAnnotationId, aAttribute, aSlot);
    }

    public VID(String aExtensionId, long aLayerId, int aAnnotationID, int aSubAnnotationId,
            int aAttribute, int aSlot)
    {
        annotationId = aAnnotationID;
        subAnnotationId = aSubAnnotationId;
        attribute = aAttribute;
        slot = aSlot;
        extensionId = aExtensionId;
        layerId = aLayerId;
    }

    public boolean isSet()
    {
        return annotationId >= 0;
    }

    public boolean isNotSet()
    {
        return !isSet();
    }

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
        Matcher m = PATTERN_VID.matcher(aVid);
        if (m.matches()) {
            int annotationId = Integer.valueOf(m.group(ID));
            int subAnnotationId = NONE;
            int feature = NONE;
            int slot = NONE;
            int layerId = NONE;
            String extensionId = null;

            if (m.group(SUB) != null) {
                subAnnotationId = Integer.valueOf(m.group(SUB));
            }
            if (m.group(ATTR) != null) {
                feature = Integer.valueOf(m.group(ATTR));
            }
            if (m.group(SLOT) != null) {
                slot = Integer.valueOf(m.group(SLOT));
            }
            if (m.group(EXTENSION) != null) {
                extensionId = m.group(EXTENSION);
            }
            if (m.group(LAYER) != null) {
                layerId = Integer.valueOf(m.group(LAYER));
            }
            return new VID(extensionId, layerId, annotationId, subAnnotationId, feature, slot);
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
}
