/*
 * Copyright 2017
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.inception.recommendation.model;

import static org.apache.commons.lang3.Validate.notNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import de.tudarmstadt.ukp.inception.recommendation.imls.core.classificationtool.ClassificationTool;
import de.tudarmstadt.ukp.inception.recommendation.imls.dl4j.pos.DL4JPosClassificationTool;
import de.tudarmstadt.ukp.inception.recommendation.imls.mira.pos.MiraPosClassificationTool;
import de.tudarmstadt.ukp.inception.recommendation.imls.opennlp.ner.OpenNlpNerClassificationTool;
import de.tudarmstadt.ukp.inception.recommendation.imls.opennlp.pos.OpenNlpPosClassificationTool;
import de.tudarmstadt.ukp.inception.recommendation.imls.stringmatch.ner.StringMatchingNerClassificationTool;
import de.tudarmstadt.ukp.inception.recommendation.imls.stringmatch.pos.StringMatchingPosClassificationTool;

/**
 * Simple registry like class. Containing the implemented classification tools mapped to their
 * annotation layer.
 * 
 * To add another classification tool, you have to put the class name into the list of available
 * classification tools for the specific annotation layer.
 */
@Component
public class ClassificationToolRegistry
{
    private static final Logger log = LoggerFactory.getLogger(ClassificationToolRegistry.class);
    private final Map<String, List<Class<?>>> classificationToolClasses;
    private final Map<String, List<String>> classificationToolIds;

    public ClassificationToolRegistry()
    {
        String key;
        Map<String, List<Class<?>>> classificationToolClasses = new HashMap<>();

        // POS Annotation Layer
        key = POS.class.getName();
        classificationToolClasses.put(key,
                Arrays.asList(
                        StringMatchingPosClassificationTool.class,
                        OpenNlpPosClassificationTool.class, 
                        MiraPosClassificationTool.class,
                        DL4JPosClassificationTool.class
                        ));

        key = NamedEntity.class.getName();
        classificationToolClasses.put(key, Arrays.asList(StringMatchingNerClassificationTool.class,
                OpenNlpNerClassificationTool.class));

        this.classificationToolClasses = Collections.unmodifiableMap(classificationToolClasses);

        Map<String, List<String>> classificationToolIds = new HashMap<>();
        for (Entry<String, List<Class<?>>> entry : classificationToolClasses.entrySet()) {
            List<String> ids = new LinkedList<>();
            for (Class<?> clazz : entry.getValue()) {
                ids.add(clazz.getName());
            }
            classificationToolIds.put(entry.getKey(), ids);
        }
        this.classificationToolIds = Collections.unmodifiableMap(classificationToolIds);
    }

    public Map<String, List<Class<?>>> getRegisteredClassificationTools()
    {
        return classificationToolClasses;
    }

    public Map<String, List<String>> getRegisteredClassificationToolIds()
    {
        return classificationToolIds;
    }

    public ClassificationTool<?> createClassificationTool(String aLayer,
            int aRecId, String aToolId, String aFeature, int aMaxPredictions)
    {
        notNull(aLayer);
        notNull(aToolId);
        
        List<Class<?>> classificationTools = classificationToolClasses.get(aLayer);
        if (classificationTools == null ) {
            throw new IllegalArgumentException("No tools found for layer [" + aLayer + "]");
        }

        for (Class<?> ct : classificationTools) {
            if (aToolId.equals(ct.getName())) {
                try {
                    Class[] cArg = new Class[3];
                    cArg[0] = int.class;
                    cArg[1] = int.class;
                    cArg[2] = String.class;
                    ClassificationTool<?> tool = (ClassificationTool<?>) ct
                            .getDeclaredConstructor(cArg)
                            .newInstance(aRecId, aMaxPredictions, aFeature);
                    return tool;
                }
                catch (Exception e) {
                    try {
                        Class[] cArg = new Class[2];
                        cArg[0] = int.class;
                        cArg[1] = String.class;
                        ClassificationTool<?> tool = (ClassificationTool<?>) 
                                ct.getDeclaredConstructor(cArg).newInstance(aRecId, aFeature);
                        return tool;
                    }
                    catch (Exception e2) {
                        throw new IllegalStateException(
                                "Cannot instantiate tool [" + ct.getName() + "]", e2);
                    }
                }
            }
        }

        throw new IllegalArgumentException("No tools found for layer [" + aLayer + "]");
    }
}
