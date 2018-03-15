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

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;

import de.tudarmstadt.ukp.inception.conceptlinking.model.Property;

/**
 * Contains utility methods to process files needed for Concept Linking
 */
public class Utils
{

    public static Set<String> readFile(String location)
    {
        try {
            File f = new File(location);
            List<String> lines = FileUtils.readLines(f, "UTF-8");
            return new HashSet<>(lines);

        }
        catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Map<String, Property> loadPropertyLabels(String location)
    {
        Map<String, Property> property2LabelMap = new HashMap<String, Property>();
        try {
            File f = new File(location);
            List<String> lines = FileUtils.readLines(f, "UTF-8");
            for (String line: lines) {
                if (!line.startsWith("#")) {
                    String[] col = line.split("\t");
                    Property label = new Property(col[1], col[3], col[4], col[5]);
                    property2LabelMap.put(col[0].trim(), label);
                }
            }
            return property2LabelMap;
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Map<String, Integer> loadEntityFrequencyMap(String filename)
    {
        Map<String, Integer> entityFreqMap = new HashMap<String, Integer>();
        try {
            File f = new File(filename);
            List<String> lines = FileUtils.readLines(f, "UTF-8");
            for (String line: lines) {
                if (!line.startsWith("#")) {
                    String[] col = line.split("\t");
                    entityFreqMap.put(col[0], Integer.parseInt(col[1]));
                }
            }
            return entityFreqMap;
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
    
    public static Set<String> loadPropertyBlacklist(String filename)
    {
        Set<String> propertyBlacklist = new HashSet<>();
        try {
            File f = new File(filename);
            List<String> lines = FileUtils.readLines(f, "UTF-8");
            for (String line: lines) {
                if (!line.startsWith("#")) {
                    String[] col = line.split("\t");
                    propertyBlacklist.add(col[0]);
                }
            }
            return propertyBlacklist;
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    
}
