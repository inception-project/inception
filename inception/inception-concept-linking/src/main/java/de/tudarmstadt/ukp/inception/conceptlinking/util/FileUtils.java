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

package de.tudarmstadt.ukp.inception.conceptlinking.util;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.inception.conceptlinking.model.Property;

/**
 * Contains utility methods to process files needed for Concept Linking
 */
public class FileUtils
{

    private final static Logger log = LoggerFactory.getLogger(FileUtils.class);

    private static List<String> readLines(File r, String reason)
    {
        List<String> lines = new ArrayList<>();
        String l;
        try (var br = new BufferedReader(new InputStreamReader(new FileInputStream(r), UTF_8))) {
            while ((l = br.readLine()) != null) {
                lines.add(l);
            }
        }
        catch (IOException e) {
            log.debug("File [{}] is missing - {}", r.getName(), reason);
        }
        return lines;
    }

    public static Set<String> loadStopwordFile(File r)
    {
        String reason = "Using entity linking support without stopwords will have a negative "
                + "impact on the suggestion ranking.";
        List<String> lines = readLines(r, reason);
        return new HashSet<>(lines);
    }

    public static Map<String, Property> loadPropertyLabels(File r)
    {
        String reason = "Using entity linking support without propertyId:propertyLabel dictionary "
                + "file may have a negative impact on the suggestion ranking.";
        Map<String, Property> property2LabelMap = new HashMap<>();
        List<String> lines = readLines(r, reason);
        for (String line : lines) {
            if (!line.startsWith("#")) {
                String[] col = line.split("\t");
                Property label = new Property(col[1], col[3], col[4], col[5]);
                property2LabelMap.put(col[0].trim(), label);
            }
        }
        return property2LabelMap;
    }

    public static Map<String, Integer> loadEntityFrequencyMap(File r)
    {
        String reason = "Using entity linking support without entity frequency file will "
                + "have a negative impact on the suggestion ranking.";
        Map<String, Integer> entityFreqMap = new HashMap<>();
        List<String> lines = readLines(r, reason);
        for (String line : lines) {
            if (!line.startsWith("#")) {
                String[] col = line.split("\t");
                entityFreqMap.put(col[0], Integer.parseInt(col[1]));
            }
        }
        return entityFreqMap;
    }

    public static Set<String> loadPropertyBlacklist(File r)
    {
        String reason = "Using entity linking support without property blacklist file may have a negative "
                + "impact on the suggestion ranking.";
        Set<String> propertyBlacklist = new HashSet<>();
        List<String> lines = readLines(r, reason);
        for (String line : lines) {
            if (!line.startsWith("#")) {
                String[] col = line.split("\t");
                propertyBlacklist.add(col[0]);
            }
        }
        return propertyBlacklist;
    }
}
