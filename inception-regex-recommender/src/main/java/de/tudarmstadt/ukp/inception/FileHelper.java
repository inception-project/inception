package de.tudarmstadt.ukp.inception;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import de.tudarmstadt.ukp.clarin.webanno.support.SettingsUtil;

public class FileHelper {

    /**
     * Export a resource embedded into a Jar file to the local file path.
     *
     * @param resourceName ie.: "/SmartLibrary.dll"
     * @return The path to the exported resource
     * @throws Exception
     */
    static public void exportResource(String resourceName,
                                      String pathToNewLocation) throws Exception
    {
        InputStream stream = null;
        OutputStream resStreamOut = null;
        try {
            try {
                //note that each / is a directory down in the "jar tree"
                //with the jar as the root of the tree
                stream = RegexRecommender.class.getResourceAsStream("/" + resourceName);
            } catch (NullPointerException e) {
                e.printStackTrace();
                System.out.println(resourceName);
            }
            if (stream == null) {
                throw new Exception("Cannot get resource \"" + resourceName + "\" from Jar file.");
            }

            int readBytes;
            byte[] buffer = new byte[4096];
            resStreamOut = new FileOutputStream(pathToNewLocation);
            while ((readBytes = stream.read(buffer)) > 0) {
                resStreamOut.write(buffer, 0, readBytes);
            }
        
        } catch (Exception ex) {
            throw ex;
        } finally {
            stream.close();
            resStreamOut.close();
        }
    }
    
    static public String exportDornseiffFile(String aResource) 
    {
        File fileInInceptionHome = new File(SettingsUtil.getApplicationHome().getPath() +
                                                                        "/" + aResource);
        if (!fileInInceptionHome.exists()) {
            try {
                exportResource(aResource, fileInInceptionHome.getPath());
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        return fileInInceptionHome.getPath();
    }
    
    private static Map<String, Set<String>> readDornseiffFile(String fileName)
            throws FileNotFoundException, IOException {
        
        Map<String, Set<String>> categories = new HashMap<>();
        
        BufferedReader br = null;
        try {
            br = new BufferedReader(new InputStreamReader(new FileInputStream(fileName), "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        String line;
        boolean listen = false;
        String currentCat = null;
        Set<String> catEntries = new HashSet<>();
        while ((line = br.readLine()) != null) {

            if (listen && !line.startsWith("\t")) {
                listen = false;
                Set<String> entriesClone = new HashSet<>(catEntries);
                categories.put(currentCat, entriesClone);
                currentCat = null;
                catEntries.clear();
            }
            
            if (!(line.startsWith("\t") || line.equals(""))) {
                listen = true;
                currentCat = line;
                
            }

            if (listen && line.startsWith("\t")) {
                //go through all the entries in each line
                catEntries.add(line.trim().toLowerCase());
            }    
        }
        br.close();
    
        return categories;
    }
    
    public static void writeAccRejFile(RecommendationAcceptedListener accLis,
                                       RecommendationRejectedListener rejLis) {
                
        File fout = new File(SettingsUtil.getApplicationHome().getPath() + "/accRej.txt");
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(fout);
        } catch (FileNotFoundException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        BufferedWriter writer;
        try {
            writer = new BufferedWriter(new OutputStreamWriter(fos, "UTF-8"));
            
            for (String feature: accLis.getAcceptedCount().keySet()) {
                writer.write(feature);
                writer.newLine();
                for (String regex: accLis.getAcceptedCount().get(feature).keySet()) {
                    writer.write("\t" + regex);
                    try {
                        Integer acceptedCount = accLis.getAcceptedCount().get(feature).get(regex);
                        Integer rejectedCount = rejLis.getRejectedCount().get(feature).get(regex);
                        writer.write("\t" + acceptedCount.toString() +
                                     "\t" + rejectedCount.toString());
                        writer.newLine();
                    } catch (NullPointerException e) {
                        e.printStackTrace();
                    }
                }
                writer.newLine();
            }
            writer.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }       
    }
    
    public static Map<String, Map<String, Integer>>
                    readAcceptedRejectedFile(String mode, String fileName)
                    throws IOException {
        int i;
        if (mode.equals("accepted")) {
            i = 2;
        } else {
            i = 3;
        }
        
        Map<String, Map<String, Integer>> accRejMap = new HashMap<>();
        Map<String, Integer> featureAccRejMap = null;
        
        try (BufferedReader br = new BufferedReader(
                                    new InputStreamReader(
                                        new FileInputStream(fileName), "UTF-8"))) {
            String line;
            while ((line = br.readLine()) != null) {

                if (!line.startsWith("\t")) {
                    featureAccRejMap = new HashMap<String, Integer>();
                    accRejMap.put(line, featureAccRejMap);
                }
                
                if (line.startsWith("\t")) {
                    //go through all the entries in each line
                    String[] splitLine = line.split("\\t");
                    int count = Integer.parseInt(splitLine[i]);
                    featureAccRejMap.put(splitLine[1], count);
                }            
                
            }
        } catch (FileNotFoundException e) {
            Map<String, Set<String>> allRegexes;
            try {
                allRegexes = readDornseiffFile(SettingsUtil.getApplicationHome().getPath() + "/LemmatizedZeit.txt");
            } catch (FileNotFoundException e1) {
                exportDornseiffFile("LemmatizedZeit.txt");
                allRegexes = readDornseiffFile(SettingsUtil.getApplicationHome().getPath() + "/LemmatizedZeit.txt");
            } 
            
            Map<String, Integer> featAccRejMap;
            
            for (Map.Entry<String, Set<String>> entry: allRegexes.entrySet()) {
                featAccRejMap = new HashMap<String, Integer>();
                accRejMap.put(entry.getKey(), featAccRejMap);
                for (String regex: entry.getValue()) {
                    featAccRejMap.put(regex, 0);
                }
            }
        } catch (IOException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        return accRejMap;
    }
    
    public static void writeList(Set<String> dornseiffList,
                                 String uiFeatureName,
                                 String fileName) throws FileNotFoundException {
        
        
        Map<String, Set<String>> categories = null;
        try {
            categories = readDornseiffFile(fileName);
        } catch (IOException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        
        categories.replace(uiFeatureName, dornseiffList);
        
        File fout = new File(fileName);
        FileOutputStream fos = new FileOutputStream(fout);
        BufferedWriter writer;
        try {
            writer = new BufferedWriter(new OutputStreamWriter(fos, "UTF-8"));
            for (String key: categories.keySet()) {
                writer.write(key);
                writer.newLine();
                for (String regex: categories.get(key)) {
                    writer.write("\t" + regex);
                    writer.newLine();
                }
                writer.newLine();
            }
            writer.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }       
    }
    
    /* reads the DornseiffFile at the location {@link filename}
     * and returns a HashSet that contains all the regexes that are saved
     * there under the DornseiffFeature {@link uiFeatureName}
     * 
     * @param filename The absolute path to the DornseiffFile
     * @param uiFeatureName The name of the DornseiffFeature to extract the regexes for
     * @return A Set<String> containing the regexes under the specified DornseiffFeature
     */
    public static Set<String> readDornseiffFile(String filename, String uiFeatureName) {
        
        Set<String> dornseiffList = new HashSet<>();
        Boolean listen = false;
        try (BufferedReader br = new BufferedReader(
                                    new InputStreamReader(
                                        new FileInputStream(filename), "UTF-8"))) {
            String line;
            while ((line = br.readLine()) != null) {

                if (listen && !line.startsWith("\t")) {
                    break;
                }
                
                if (line.equals(uiFeatureName)) {
                    listen = true;
                }

                if (listen && line.startsWith("\t")) {
                    //go through all the entries in each line
                    for (String tokens: line.subSequence(1, line.length()).toString().split("; ")) {
                        dornseiffList.add(tokens.toLowerCase());
                    }
                }             
            }  
            
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }     
               
        return dornseiffList;
    }
}
