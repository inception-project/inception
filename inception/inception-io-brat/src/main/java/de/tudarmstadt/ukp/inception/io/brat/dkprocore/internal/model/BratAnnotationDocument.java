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
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;

import com.fasterxml.jackson.core.JsonGenerator;

public class BratAnnotationDocument
{
    private Map<String, BratAnnotation> annotations = new LinkedHashMap<>();
    private Map<String, BratAttribute> attributes = new LinkedHashMap<>();

    public static BratAnnotationDocument read(Reader aReader)
    {
        var doc = new BratAnnotationDocument();

        var events = new ArrayList<BratEventAnnotation>();

        // Read from file
        var lines = IOUtils.lineIterator(aReader);
        while (lines.hasNext()) {
            var line = lines.next();

            if (line.isBlank()) {
                continue;
            }

            switch (line.charAt(0)) {
            case '#':
                doc.addAnnotation(BratNoteAnnotation.parse(line));
                break;
            case 'T':
                doc.addAnnotation(BratTextAnnotation.parse(line));
                break;
            case 'A':
            case 'M':
                doc.addAttribute(BratAttribute.parse(line));
                break;
            case 'R':
                doc.addAnnotation(BratRelationAnnotation.parse(line));
                break;
            case 'E': {
                BratEventAnnotation e = BratEventAnnotation.parse(line);
                events.add(e);
                doc.addAnnotation(e);
                break;
            }
            default:
                throw new IllegalStateException("Unknown annotation format: [" + line + "]");
            }
        }

        // Attach attributes to annotations
        for (var attr : doc.attributes.values()) {
            var target = doc.annotations.get(attr.getTarget());

            if (target == null) {
                throw new IllegalStateException(
                        "Attribute refers to unknown annotation [" + attr.getTarget() + "]");
            }

            target.addAttribute(attr);
        }

        // FIXME this is currently inconsistent between reading and manual creation / writing
        // when we read the triggers, they no longer appear as individual annotations
        // when we manually create the brat annotations, we leave the triggers

        // Attach triggers to events and remove them as individual annotations
        List<String> triggersIds = new ArrayList<>();
        for (BratEventAnnotation event : events) {
            BratTextAnnotation trigger = (BratTextAnnotation) doc.annotations
                    .get(event.getTrigger());

            if (trigger == null) {
                throw new IllegalStateException(
                        "Trigger refers to unknown annotation [" + event.getTrigger() + "]");
            }

            triggersIds.add(trigger.getId());
            event.setTriggerAnnotation(trigger);
        }

        // Remove trigger annotations, they are owned by the event
        doc.annotations.keySet().removeAll(triggersIds);

        return doc;
    }

    public void write(JsonGenerator aJG, String aText) throws IOException
    {
        aJG.writeStartObject();

        aJG.writeStringField("text", aText);

        aJG.writeFieldName("entities");
        aJG.writeStartArray();
        for (BratAnnotation ann : annotations.values()) {
            if (ann instanceof BratTextAnnotation) {
                ann.write(aJG);
            }
        }
        aJG.writeEndArray();

        aJG.writeFieldName("relations");
        aJG.writeStartArray();
        for (BratAnnotation ann : annotations.values()) {
            if (ann instanceof BratRelationAnnotation) {
                ann.write(aJG);
            }
        }
        aJG.writeEndArray();

        aJG.writeFieldName("triggers");
        aJG.writeStartArray();
        for (BratAnnotation ann : annotations.values()) {
            if (ann instanceof BratEventAnnotation) {
                ((BratEventAnnotation) ann).getTriggerAnnotation().write(aJG);
            }
        }
        aJG.writeEndArray();

        aJG.writeFieldName("events");
        aJG.writeStartArray();
        for (BratAnnotation ann : annotations.values()) {
            if (ann instanceof BratEventAnnotation) {
                ann.write(aJG);
            }
        }
        aJG.writeEndArray();

        aJG.writeFieldName("attributes");
        aJG.writeStartArray();
        for (BratAnnotation ann : annotations.values()) {
            for (BratAttribute attr : ann.getAttributes()) {
                attr.write(aJG);
            }
        }
        aJG.writeEndArray();

        aJG.writeEndObject();
    }

    public void write(Writer aWriter) throws IOException
    {
        // First render only the spans because brat wants to now them first for their IDs
        for (BratAnnotation anno : annotations.values()) {
            if (anno instanceof BratTextAnnotation) {
                write(aWriter, anno);
            }

            if (anno instanceof BratEventAnnotation) {
                // Just write the trigger for now
                BratEventAnnotation event = (BratEventAnnotation) anno;
                write(aWriter, event.getTriggerAnnotation());
            }
        }

        // Second render all annotations that point to span annotations
        for (BratAnnotation anno : annotations.values()) {
            // Skip the text annotations, we already rendered them in the first pass
            if (anno instanceof BratTextAnnotation) {
                continue;
            }

            write(aWriter, anno);
        }
    }

    private void write(Writer aWriter, BratAnnotation aAnno) throws IOException
    {
        aWriter.append(aAnno.toString());
        aWriter.append('\n');
        for (BratAttribute attr : aAnno.getAttributes()) {
            aWriter.append(attr.toString());
            aWriter.append('\n');
        }
    }

    public void addAttribute(BratAttribute aAttribute)
    {
        attributes.put(aAttribute.getId(), aAttribute);
    }

    public void addAnnotation(BratAnnotation aAnnotation)
    {
        annotations.put(aAnnotation.getId(), aAnnotation);
    }

    public BratAnnotation getAnnotation(String aId)
    {
        return annotations.get(aId);
    }

    public Collection<BratAnnotation> getAnnotations()
    {
        return annotations.values();
    }
}
