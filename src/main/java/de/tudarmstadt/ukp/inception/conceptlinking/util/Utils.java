package de.tudarmstadt.ukp.inception.conceptlinking.util;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;

import de.tudarmstadt.ukp.inception.conceptlinking.model.Label;

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

    public static Map<String, Label> loadPropertyLabels(String location)
    {
        Map<String, Label> property2LabelMap = new HashMap<String, Label>();
        try {
            File f = new File(location);
            List<String> lines = FileUtils.readLines(f, "UTF-8");
            for (String line: lines) {
                if (!line.startsWith("#")) {
                    String[] col = line.split("\t");
                    Label label = new Label(col[1], col[3], col[4], col[5]);
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
}
