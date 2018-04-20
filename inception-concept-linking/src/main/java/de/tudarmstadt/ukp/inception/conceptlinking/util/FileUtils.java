/*
 * Copyright 2018
 * Ubiquitous Knowledge Processing (UKP) Lab
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

package de.tudarmstadt.ukp.inception.conceptlinking.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;

import de.tudarmstadt.ukp.inception.conceptlinking.model.Property;

/**
 * Contains utility methods to process files needed for Concept Linking
 */
public class FileUtils
{

    private final static Logger log = LoggerFactory.getLogger(FileUtils.class);

    private static List<String> readLines(Resource r)
    {
        List<String> lines = new ArrayList<>();
        String l;
        try {
            InputStream is = r.getInputStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(is, "utf8"));
            while ((l = br.readLine()) != null) {
                lines.add(l);
            }
            br.close();
        } catch (IOException e) {
            log.error("Could not read file " + r.getFilename(), e);
        }
        return lines;
    }

    public static Set<String> loadStopwordFile(Resource r)
    {
        List<String> lines = readLines(r);
        return new HashSet<>(lines);
    }

    public static Map<String, Property> loadPropertyLabels(Resource r)
    {
        Map<String, Property> property2LabelMap = new HashMap<String, Property>();
        List<String> lines = readLines(r);
        for (String line: lines) {
            if (!line.startsWith("#")) {
                String[] col = line.split("\t");
                Property label = new Property(col[1], col[3], col[4], col[5]);
                property2LabelMap.put(col[0].trim(), label);
            }
        }
        return property2LabelMap;
    }

    public static Map<String, Integer> loadEntityFrequencyMap(Resource r)
    {
        Map<String, Integer> entityFreqMap = new HashMap<String, Integer>();
        List<String> lines = readLines(r);
        for (String line : lines) {
            if (!line.startsWith("#")) {
                String[] col = line.split("\t");
                entityFreqMap.put(col[0], Integer.parseInt(col[1]));
            }
        }
        return entityFreqMap;
    }
    
    public static Set<String> loadPropertyBlacklist(Resource r)
    {
        Set<String> propertyBlacklist = new HashSet<>();
        List<String> lines = readLines(r);
        for (String line: lines) {
            if (!line.startsWith("#")) {
                String[] col = line.split("\t");
                propertyBlacklist.add(col[0]);
            }
        }
        return propertyBlacklist;
    }
}
