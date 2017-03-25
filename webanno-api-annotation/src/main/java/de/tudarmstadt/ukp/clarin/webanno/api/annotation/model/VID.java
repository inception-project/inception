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

import java.io.Serializable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

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
    
    public static final Pattern PATTERN_VID = Pattern.compile("(?<ID>\\d+)(?:\\-(?<SUB>\\d+))?(?:\\.(?<ATTR>\\d+))?(?:\\.(?<SLOT>\\d+))?");
    public static final int NONE = -1;

    public static final VID NONE_ID = new VID(NONE);

    private final int annotationId;
    private final int subAnnotationId;
    private final int attribute;
    private final int slot;

    public VID(int aAnnotationID)
    {
        this(aAnnotationID, NONE, NONE, NONE);
    }

    public VID(int aAnnotationID, int aAttribute)
    {
        this(aAnnotationID, NONE, aAttribute, NONE);
    }
    
    public VID(int aAnnotationID, int aAttribute, int aSlot)
    {
        this(aAnnotationID, NONE, aAttribute, aSlot);
    }

    public VID(int aAnnotationID, int aSubAnnotationId, int aAttribute, int aSlot)
    {
        annotationId = aAnnotationID;
        subAnnotationId = aSubAnnotationId;
        attribute = aAttribute;
        slot = aSlot;
    }

    public boolean isSet()
    {
        return annotationId >= 0;
    }
    
    public boolean isNotSet()
    {
        return !isSet();
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
            if (m.group(SUB) != null) {
                subAnnotationId = Integer.valueOf(m.group(SUB));
            }
            if (m.group(ATTR) != null) {
                feature = Integer.valueOf(m.group(ATTR));
            }
            if (m.group(SLOT) != null) {
                slot = Integer.valueOf(m.group(SLOT));
            }
            return new VID(annotationId, subAnnotationId, feature, slot);
        }
        else {
            throw new IllegalArgumentException("Cannot parse visual identifier [" + aVid + "]");
        }
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();

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

        return sb.toString();
    }
}
