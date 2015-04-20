/*******************************************************************************
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
 ******************************************************************************/
package de.tudarmstadt.ukp.clarin.webanno.brat.display.model;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.codehaus.jackson.map.ser.std.ToStringSerializer;

/**
 * Visual ID. An ID used in communication with the brat-based annotation editor.
 */
@JsonSerialize(using = ToStringSerializer.class)
public class VID
{
    public static final Pattern PATTERN_VID = Pattern.compile("(\\d+)(?:\\.(\\d+))?(?:\\.(\\d+))?");
    public static final int NONE = -1;
    public static final int GHOST = -2;
    
    public static final VID GHOST_ID = new VID(GHOST);
    
    private final int annotationId;
    private final int attribute;
    private final int slot;

    public VID(int aAnnotationID)
    {
        this(aAnnotationID, NONE, NONE);
    }

    public VID(int aAnnotationID, int aAttribute)
    {
        this(aAnnotationID, aAttribute, NONE);
    }

    public VID(int aAnnotationID, int aAttribute, int aSlot)
    {
        annotationId = aAnnotationID;
        attribute = aAttribute;
        slot = aSlot;
    }
    
    public boolean isSet()
    {
        return annotationId >= 0;
    }
    
    public boolean isGhost()
    {
        return annotationId == GHOST;
    }

    public int getAnnotationId()
    {
        return annotationId;
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
            int annotationId = Integer.valueOf(m.group(1));
            int feature = NONE;
            int slot = NONE;
            if (m.group(2) != null) {
                feature = Integer.valueOf(m.group(2));
            }
            if (m.group(3) != null) {
                slot = Integer.valueOf(m.group(3));
            }
            return new VID(annotationId, feature, slot);
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
