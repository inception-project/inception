/*
 * Copyright 2017
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.inception.io.brat.dkprocore.internal.model;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.core.JsonGenerator;

public class BratEventAnnotation
    extends BratAnnotation
{
    // Multiple slots with the same name:
    // E3 Binding:T9 Theme:T4 Theme2:T5 Theme3:T6 Theme4:T3
    // These at all "Theme" slots, the number basically just for disambiguation

    // If two stacking events are created, they reuse the trigger span
    // T31 Binding 6 16 annotation
    // E11 Binding:T31 Theme:T19 Theme2:T29
    // E12 Binding:T31

    // Attributes on events attach to the event, not to the trigger
    // E9 Binding:T27 Theme:T12 Theme2:T11
    // A1 Negation E9

    // Probably relations pointing to events also attach to the event, not to the trigger

    private static final Pattern PATTERN = Pattern.compile("(?<ID>E[0-9]+)[\\t]"
            + "(?<TYPE>[a-zA-Z0-9_][a-zA-Z0-9_-]+):" + "(?<TRIGGER>[ET][0-9]+)"
            + "(?<ARGS>( [a-zA-Z_][a-zA-Z0-9_-]+:[ET][0-9]+)*)[ ]*");

    private static final String ID = "ID";
    private static final String TYPE = "TYPE";
    private static final String TRIGGER = "TRIGGER";
    private static final String ARGS = "ARGS";

    private final String trigger;

    private BratTextAnnotation triggerAnnotation;

    private List<BratEventArgument> arguments;

    public BratEventAnnotation(int aId, String aType, String aTrigger,
            BratEventArgument... aArguments)
    {
        this("E" + aId, aType, aTrigger, aArguments);
    }

    public BratEventAnnotation(String aId, String aType, String aTrigger,
            BratEventArgument... aArguments)
    {
        super(aId, aType);
        trigger = aTrigger;
        setArguments(aArguments == null ? null : Arrays.asList(aArguments));
    }

    public BratEventAnnotation(String aId, String aType, String aTrigger,
            Collection<BratEventArgument> aArguments)
    {
        super(aId, aType);
        trigger = aTrigger;
        setArguments(aArguments);
    }

    public String getTrigger()
    {
        return trigger;
    }

    public BratTextAnnotation getTriggerAnnotation()
    {
        return triggerAnnotation;
    }

    public void setTriggerAnnotation(BratTextAnnotation aTriggerAnnotation)
    {
        triggerAnnotation = aTriggerAnnotation;
    }

    public void setArguments(Collection<BratEventArgument> aArguments)
    {
        arguments = aArguments == null ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<BratEventArgument>(aArguments));
    }

    public List<BratEventArgument> getArguments()
    {
        return arguments;
    }

    public Map<String, List<BratEventArgument>> getGroupedArguments()
    {
        Map<String, List<BratEventArgument>> grouped = new LinkedHashMap<>();
        for (BratEventArgument e : arguments) {
            List<BratEventArgument> l = grouped.get(e.getSlot());
            if (l == null) {
                l = new ArrayList<>();
                grouped.put(e.getSlot(), l);
            }
            l.add(e);
        }
        return grouped;
    }

    @Override
    public void write(JsonGenerator aJG) throws IOException
    {
        aJG.writeStartArray();
        aJG.writeString(getId());
        aJG.writeString(trigger);
        aJG.writeStartArray();
        for (BratEventArgument arg : arguments) {
            arg.write(aJG);
        }
        aJG.writeEndArray();
        aJG.writeEndArray();
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(getId());
        sb.append('\t');
        sb.append(getType());
        sb.append(':');
        sb.append(trigger);
        for (BratEventArgument arg : arguments) {
            sb.append(" ");
            sb.append(arg);
        }
        return sb.toString();
    }

    public static BratEventAnnotation parse(String aLine)
    {
        Matcher m = PATTERN.matcher(aLine);

        if (!m.matches()) {
            throw new IllegalArgumentException("Illegal event annotation format [" + aLine + "]");
        }

        List<BratEventArgument> arguments = null;
        if (StringUtils.isNotBlank(m.group(ARGS))) {
            arguments = Arrays.stream(m.group(ARGS).trim().split(" "))
                    .map(arg -> BratEventArgument.parse(arg)).collect(Collectors.toList());
        }

        return new BratEventAnnotation(m.group(ID), m.group(TYPE), m.group(TRIGGER), arguments);
    }
}
