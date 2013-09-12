/*******************************************************************************
 * Copyright 2012
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
package de.tudarmstadt.ukp.clarin.webanno.crowdflower;

import static org.apache.commons.lang.StringEscapeUtils.escapeHtml;
import static org.uimafit.util.JCasUtil.select;
import static org.uimafit.util.JCasUtil.selectCovered;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Serializable;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.uima.jcas.JCas;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.ObjectWriter;

import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;

public class NamedEntityTaskManager implements Serializable
{

    private static final long serialVersionUID = -166689748276508297L;

    private CrowdClient crowdclient;

    private static final String noGoldNER1Reason = "Dieser Textabschnitt beinhaltet keine Named Entities (in solchen F&auml;llen m&uuml;ssen sie auf den daf&uuml;r vorgesehen Button klicken. Es reicht nicht aus nichts zu makieren). Sie k&ouml;nnen diese Meinung anfechten und uns erkl&auml;ren warum Sie meinen das dies falsch ist.\n";
    private static final String goldNER1ReasonHints = "\n <br/> Tipps: Wenn eine Named Entity l&auml;nger als ein Wort lang ist, m&uuml;ssen sie beim ersten Wort anfangen zu makieren und beim letzten Wort loslassen. Falsch ist: Klicken Sie stattdessen auf die W&ouml;rter einzeln, erzeugt dies mehrere einzelne Makierungen! Sie k&ouml;nnen auch nur bis z.B. zu der H&auml;lfte eines Wortes makieren, dies erfasst trotzdem das ganze Wort. \n";

    private static final String bogusNER2Reason = "Wenn sie anderer Meinung sind, k&ouml;nnen Sie unsere Meinung anfechten.";

    private static Map<String, String> task2NeMap;

    static {
        task2NeMap = new HashMap<String, String>();
        task2NeMap.put("PER", "Person");
        task2NeMap.put("ORG", "Organisation");
        task2NeMap.put("ORGpart", "Organisation");
        task2NeMap.put("LOC", "Ort");
        task2NeMap.put("LOCderiv", "Ort");
        task2NeMap.put("OTH", "Etwas anderes");
    }

    public NamedEntityTaskManager()
    {
        crowdclient = new CrowdClient();
    }

    /**
     * Generates a new job on crowdflower.com based on the supplied template string.
     * The new job won't have any data items
     *
     * @param template
     * @return
     * @throws JsonProcessingException
     * @throws IOException
     */
    public CrowdJob createJob(String template) throws JsonProcessingException, IOException
    {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonTemplate = mapper.readTree(template);
        CrowdJob job = new CrowdJob(jsonTemplate);
        return crowdclient.createNewJob(job);
    }

    public static String concatWithSeperator(Collection<String> words, String seperator) {
        StringBuilder wordList = new StringBuilder();
        for (String word : words) {
            wordList.append(new String(word) + seperator);
        }
        return new String(wordList.deleteCharAt(wordList.length() - 1));
    }

 /**
  * Generate data for NER task1. This is quite specific code, but a more general version could one day extend this.
  * @param documentsJCas List of Cas containing either the documents to be annotated or gold documents
  * @param goldOffset This is an offset to the token number that is send to Crowdflower, so that (i - goldOffset) = real token offset in webanno.
  *                     This is needed so that continuous token numbers can be send to Crowdflower
  * @param generateGold
  * @return
 * @throws Exception
  */
    public Vector<NamedEntityTask1Data> generateTask1Data(List<JCas>documentsJCas, int goldOffset, boolean generateGold, int limit) throws Exception
    {
        Log LOG = LogFactory.getLog(getClass());
        Vector<NamedEntityTask1Data> data = new Vector<NamedEntityTask1Data>();
        int i=goldOffset;
        StringBuilder textBuilder = new StringBuilder();
        int docNo = 0;

        //DecimalFormat percentFormat = new DecimalFormat("0.00");

        jcasloop:
        for (JCas documentJCas : documentsJCas)
        {
            int offset = i;
            int documentSize = documentJCas.size();

            int sentenceNo = 0;
            LOG.info("Generating data for document: " + docNo + "/" + documentsJCas.size());
            LOG.info("Document size: " + documentSize);
            for (Sentence sentence : select(documentJCas, Sentence.class)) {

                //if global limit of sentences reached, abort whole iteration
                if(limit != -1 && sentenceNo >= limit) {
                    break jcasloop;
                }

                textBuilder.setLength(0);

                //debug: output progress every 200 sentences
                /*if(sentenceNo % 200 == 0) {
                    LOG.info(percentFormat.format(((float)sentence.getBegin() / (float)documentSize)*1000.0)+"%");
                }*/

                //Maps our own token offsets (needed by JS in the crowdflower task) to Jcas offsets
                HashMap<Integer,Integer> charOffsetStartMapping = new HashMap<Integer,Integer>();
                HashMap<Integer,Integer> charOffsetEndMapping = new HashMap<Integer,Integer>();

                for (Token token : selectCovered(Token.class, sentence)) {

                    //check that token offsets match to (i - goldOffset)
                    String tokenString = new String(token.getCoveredText());
                    textBuilder.append("<span id=\"token=");
                    textBuilder.append(String.valueOf(i));
                    textBuilder.append("\">");
                    textBuilder.append(escapeHtml(tokenString));
                    textBuilder.append(" </span>");

                    charOffsetStartMapping.put(token.getBegin(), i);
                    charOffsetEndMapping.put(token.getEnd(), i);

                    i++;
                }

                String text = new String(textBuilder);

                //clear string builder
                textBuilder.setLength(0);

                //System.out.println(text);
                NamedEntityTask1Data task1Data = new NamedEntityTask1Data(text);
                task1Data.setOffset(offset);
                task1Data.setDocument("S"+String.valueOf(docNo));

                if (generateGold)
                {
                    ArrayList<String> goldElems = new ArrayList<String>();
                    ArrayList<String> goldTokens = new ArrayList<String>();
                    ArrayList<String> goldTypes = new ArrayList<String>();

                    int lastNeBegin = -1;
                    int lastNeEnd = -1;

                    for (NamedEntity namedEntity : selectCovered(NamedEntity.class, sentence))
                    {
                        List<Token> tokens = selectCovered(documentJCas, Token.class, namedEntity.getBegin(),
                                namedEntity.getEnd());

                        List<String> strTokens = new ArrayList<String>();

                        //transform List<Tokens> to List<Strings>
                        for (Token t : tokens )
                        {
                            strTokens.add(new String(t.getCoveredText()));
                        }

                        String strToken = concatWithSeperator(strTokens," ");
                        String type = namedEntity.getValue();


                        int neBegin = namedEntity.getBegin();
                        int neEnd = namedEntity.getEnd();
                        String strElem = buildTask1TokenJSON(textBuilder, namedEntity,charOffsetStartMapping,charOffsetEndMapping);

                        LOG.debug("Checking new gold elem " + strElem + " Begin:" + neBegin + " End:" + neEnd);
                        //nested NE's
                        if(lastNeEnd != -1 && neBegin <= lastNeEnd)
                        {
                            LOG.debug("Nested NE! Last ne size: " + (lastNeEnd - lastNeBegin) + " this NE:" + (neEnd - neBegin));
                            //this NE is bigger = better
                            if((neEnd - neBegin) > (lastNeEnd - lastNeBegin))
                            {
                                //remove last NE
                                goldElems.remove(goldElems.size()-1);
                                goldTokens.remove(goldElems.size()-1);
                                goldTypes.remove(goldElems.size()-1);

                                //add new NE
                                goldElems.add(strElem);
                                goldTokens.add(strToken);
                                goldTypes.add(type);

                                lastNeBegin = neBegin;
                                lastNeEnd = neEnd;
                            }//else ignore this NE, keep last one
                            else
                            {
                                LOG.debug("Ignored elem " + strElem + " because it is a nested NE and previous NE is bigger");
                            }
                        }else{
                            //standard case, no nested NE, or first NE
                            goldElems.add(strElem);
                            goldTokens.add(strToken);
                            goldTypes.add(type);

                            lastNeBegin = neBegin;
                            lastNeEnd = neEnd;
                        }
                    }

                    //Standard case: no gold elements
                    String strTypes = "[]";
                    String strGold = "[\"none\"]";
                    String strGoldReason = noGoldNER1Reason;

                    //Case where we have gold elements and want to give feedback to the crowd user
                    if(goldElems.size() > 0)
                    {
                        strGold = buildTask1GoldElem(textBuilder, goldElems);
                        strTypes = buildTask1GoldElem(textBuilder, goldTypes);
                        strGoldReason = buildTask1GoldElemReason(textBuilder, goldTokens);
                    }

                    task1Data.setMarkertext_gold(strGold);
                    task1Data.setMarkertext_gold_reason(strGoldReason);

                    task1Data.setTypes(strTypes);
                    task1Data.setDocument("G"+String.valueOf(docNo));

                    // Marker flag for crowdflower that this data is gold data.
                    // Still need to click on "convert uploaded gold" manually in the interface.
                    task1Data.set_golden("TRUE");
                }

                data.add(task1Data);
                sentenceNo++;
            }
            docNo++;
        }

        return data;
    }

    /**
     * Helper method for generateTask1Data, builds a string that explains to the user which named entities he had to select
     *
     * @param textBuilder
     * @param goldTokens
     * @return
     */
    private String buildTask1GoldElemReason(StringBuilder textBuilder, ArrayList<String> goldTokens)
    {
        String strGoldReason;
        textBuilder.setLength(0);
        textBuilder.append("Der Text beinhaltet ");
        textBuilder.append(goldTokens.size());
        textBuilder.append(" Named Entiti(es): ");
        textBuilder.append(escapeHtml(concatWithSeperator(goldTokens,", ")));
        textBuilder.append(goldNER1ReasonHints);

        strGoldReason = new String(textBuilder);
        return strGoldReason;
    }

    /**
     * Helper method for generateTask1Data, concatenates all JSON formatted
     * tokens (sorted by position) which is then the gold solution and contains
     * markers to all NE in the text fragment.
     *
     * This same format is produced by the JS in the crowdflower.com task1.
     *
     * @param textBuilder
     * @param goldElems
     * @return
     */
    private String buildTask1GoldElem(StringBuilder textBuilder, ArrayList<String> goldElems)
    {
        String strGold;
        textBuilder.setLength(0);
        //strGold = "["+concatWithSeperator(goldElems,",")+"]";
        textBuilder.append("[");
        textBuilder.append(concatWithSeperator(goldElems,","));
        textBuilder.append("]");
        strGold = new String(textBuilder);
        return strGold;
    }

    /**
     * Helper method for generateTask1Data, builds a single JSON formatted elemented describing
     * start and end position of a named entity.
     * @param textBuilder
     * @param namedEntity
     * @return
     * @throws Exception
     */
    private String buildTask1TokenJSON(StringBuilder textBuilder, NamedEntity namedEntity, HashMap<Integer,Integer> charOffsetStartMapping, HashMap<Integer,Integer> charOffsetEndMapping) throws Exception
    {
        //JSON named enitity marker for the gold data. The JSON here gets enclosed into the data JSON that is uploaded
        //"{\"s\":"+begin+",\"e\":"+end+"}"
        if(!charOffsetStartMapping.containsKey(namedEntity.getBegin())
           || !charOffsetEndMapping.containsKey(namedEntity.getEnd()))
        {
            throw new Exception("Data generation error: char offset to token mapping is inconsistent. Contact developpers!");
        }

        int start = charOffsetStartMapping.get(namedEntity.getBegin());
        int end = charOffsetEndMapping.get(namedEntity.getEnd());

        textBuilder.setLength(0);
        textBuilder.append("{\"s\":");
        textBuilder.append(start);
        textBuilder.append(",\"e\":");
        textBuilder.append(end);
        textBuilder.append("}");

        return new String(textBuilder);
    }

    /**
     * Set apiKey to the underlying Crowdclient which manages the crowdflower.com REST-protocol
     * @param key
     */
    public void setAPIKey(String key)
    {
        Log LOG = LogFactory.getLog(getClass());
        LOG.info("Using apiKey:" + key);
        crowdclient.setApiKey(key);
    }

    public String uploadNewNERTask2(String template, List<JCas>documentsJCas , List<JCas>goldsJCas, String task1Id)
            throws JsonProcessingException, IOException, Exception
    {
        System.out.println();
        return "";
    }

    public String uploadNewNERTask1(String template, List<JCas>documentsJCas , List<JCas>goldsJCas)
            throws JsonProcessingException, IOException, Exception
            {
                return uploadNewNERTask1(template, documentsJCas , goldsJCas, -1 , -1);
            }

    /**
     * Upload a new NER task1 (german) to crowdflower.com. This method is called from the crowd page and is the starting point for a new task1 upload.
     * @param template
     * @param documentsJCas
     * @param goldsJCas
     * @return
     * @throws JsonProcessingException
     * @throws IOException
     * @throws Exception
     */

    public String uploadNewNERTask1(String template, List<JCas>documentsJCas , List<JCas>goldsJCas, int useSents , int useGoldSents)
            throws JsonProcessingException, IOException, Exception
    {
        Log LOG = LogFactory.getLog(getClass());
        LOG.info("Creating new Job for Ner task 1.");
        CrowdJob job = createJob(template);
        LOG.info("Done, new job id is: "+job.getId()+". Now generating data for NER task 1");

        //by default, allow only German speaking countries
        //would be better to make this configurable

        Vector<String> includedCountries = new Vector<String>();
        includedCountries.add("DE");
        includedCountries.add("AT");
        includedCountries.add("CH");
        job.setIncludedCountries(includedCountries);

        crowdclient.updateAllowedCountries(job);

        int goldOffset = 0;

        Vector<NamedEntityTask1Data> goldData = new Vector<NamedEntityTask1Data>();

        //if we have gold data, than generate data for it
        if(goldsJCas != null && goldsJCas.size() > 0)
        {
            LOG.info("Gold data available, generating gold data first.");
            goldData = generateTask1Data(goldsJCas,0,true, useGoldSents);
            goldOffset = goldData.size();
        }

        LOG.info("Generate normal task data.");
        Vector<NamedEntityTask1Data> data = generateTask1Data(documentsJCas,goldOffset,false,useSents);

        Vector<NamedEntityTask1Data> mergedData = new Vector<NamedEntityTask1Data>();

        if(goldsJCas != null && goldsJCas.size() > 0)
        {

            mergedData.addAll(goldData);
        }

        mergedData.addAll(data);

        LOG.info("Job data prepared, starting upload.");
        LOG.info("Uploading data to job #" + job.getId());
        crowdclient.uploadData(job,mergedData);
        LOG.info("Done, finished uploading data to #" + job.getId());
        return job.getId();
    }

    /**
     * Gets called from Crowdflower page to determine URL for a given job
     * @param jobID
     * @return
     */

   public String getURLforID(String jobID)
    {
        return "https://crowdflower.com/jobs/"+jobID+"/";
    }

   public String getStatusString(String jobID1, String jobID2)
   {
       //first case: no job ids
       if((jobID1 == null || jobID1.equals("")) && (jobID2 == null || jobID2.equals("")))
       {
           return "No jobs uploaded.";
       }
       //second case, got first job id
       else if(!(jobID1 == null || jobID1.equals("")) && (jobID2 == null || jobID2.equals("")))
       {
           JsonNode status = crowdclient.getStatus(jobID1);

           int uploadedUnits = -1;
           boolean finsished = false;

           if(status != null && status.has("count") && status.has("done"))
           {
               uploadedUnits = status.path("count").getIntValue();
               finsished = status.path("done").getBooleanValue();
           }
           else
           {
               return "Error retrieving status";
           }

           return "Job1 has "+uploadedUnits+" uploaded units and is "+
           (finsished ? " finished. You can continue with task 2." :
               " not yet finished. Check the link for more information on crowdflower.com. You have to finish task1 before doing task2.");
       }else
       {
           return "Todo: not yet implemented";
       }
   }


   /**
    * Get the string char position of the i-th span from the HTML token spans that the crowdflower job task1 uses to display a textfragment.
    * @param spans
    * @param spannumber
    * @return
    * @throws Exception
    */
   private int getSpanPos(String spans, int spannumber) throws Exception
   {
       // find all occurrences <span> forward
       int count = 0;
       for (int i = -1; (i = spans.indexOf("<span ", i + 1)) != -1; ) {
           if (spannumber==count)
           {
                   return i;
           }
           count++;
       }
       throw new Exception("Index not found:" + spannumber);
   }


   /**
    * Extract certain spans from the HTML token spans that the crowdflower job task1 uses to display a textfragment.
    * @param spans
    * @param start
    * @param end
    * @return
    * @throws Exception
    */
   private String extractSpan(String spans, int start, int end) throws Exception
   {
       int offset = getFirstSpanOffset(spans);

       assert(start >= offset);
       assert(end >= offset);

       //to have a span beyond the last one
       spans += "<span ";

       int substart = getSpanPos(spans,start-offset);
       int subend = getSpanPos(spans,end-offset+1);

       return spans.substring(substart,subend);
   }

private int getFirstSpanOffset(String spans)
{
    String firstNum = "0";
       boolean foundDigit = false;

       //span beginn will look like: "<span id='token=num'>", so will just search the first number in the string

       /*regex for this would be:
       Pattern p = Pattern.compile("(^|\\s)([0-9]+)($|\\s)");
       Matcher m = p.matcher(s);
       if (m.find()) {
           String num = m.group(2);
       }*/

       //but hey, extractSpan gets called a lot, this is probably much faster:

       for(int i=0; i < spans.length(); i++)
       {
           if(Character.isDigit(spans.charAt(i)))
           {
               foundDigit = true;
               firstNum = firstNum+Character.toString(spans.charAt(i));
           }else if(foundDigit && !Character.isDigit(spans.charAt(i)))
           {
               break;
           }
       }

       int offset = Integer.valueOf(firstNum);
    return offset;
}

   /**
    * Uploads a new task2 to Crowdflower, producing data for the new out of a task-1 ID.
    * @param template
    * @param jobID1
    * @param documentsJCas
    * @param goldsJCas
    * @return
    * @throws JsonProcessingException
    * @throws IOException
    * @throws Exception
    */

   public String uploadNewNERTask2(String template, String jobID1, List<JCas>documentsJCas , List<JCas>goldsJCas)
           throws JsonProcessingException, IOException, Exception
   {
       Log LOG = LogFactory.getLog(getClass());

       LOG.info("retrieving data for job: " + jobID1);

       //retrieve job id 1 data
       CrowdJob job1 = crowdclient.retrieveJob(jobID1);

       //rawdata is a multiline JSON file
       String rawdata = crowdclient.retrieveRawJudgments(job1);

       StringReader reader = new StringReader(rawdata);
       BufferedReader br = new BufferedReader(reader);
       String line;

       //json object mapper
       ObjectMapper mapper = new ObjectMapper();

       //used to represent task2 data that is send as JSON to crowdflower
       Vector<NamedEntityTask2Data> uploadData = new Vector<NamedEntityTask2Data>();

       ObjectWriter writer = mapper.writer();

       while((line=br.readLine())!=null)
       {
           JsonNode elem = mapper.readTree(line);
           String text = elem.path("data").path("text").getTextValue();

           System.out.println("text:" + text);

           boolean isGold = !elem.path("data").path("_golden").isMissingNode();

           if(isGold)
           {
               //System.out.println("goldelem:" + elem);
               //produce gold data
               String markertext_gold = elem.path("data").path("markertext_gold").getTextValue();
               String types = elem.path("data").path("types").getTextValue();
               String document = elem.path("data").path("document").getTextValue();

               //System.out.println("markertext_gold:" + markertext_gold);
               //System.out.println("types:" + types);

               if(!types.equals("[]"))
               {
                   //sentence has atleast one NE
                   List<String> NEtypes = Arrays.asList(types.substring(1, types.length()-1).split(","));

                   List<Integer> startMarkers = new ArrayList<Integer>();
                   List<Integer> endMarkers = new ArrayList<Integer>();
                   List<String> strMarkers = new ArrayList<String>();

                   List<String> NE = new ArrayList<String>();

                   JsonNode markers = mapper.readTree(markertext_gold);

                   for(JsonNode marker : markers)
                   {
                       strMarkers.add(writer.writeValueAsString(marker));
                       int start = marker.path("s").getIntValue();
                       int end = marker.path("e").getIntValue();
                       startMarkers.add(start);
                       endMarkers.add(end);
                       NE.add(extractSpan(text, start, end));
                   }

                   //debug
                   //System.out.println(NE);
                   //System.out.println(NEtypes);
                   for(int i = 0; i < NE.size(); i++)
                   {
                       //NamedEntityTask2Data(String text, String toDecide, String posText, String tokenOffset, String document, String ist_todecide_ein_gold, String ist_todecide_ein_gold_reason)
                       NamedEntityTask2Data task2_datum = new NamedEntityTask2Data(text,NE.get(i),strMarkers.get(i),
                               String.valueOf(getFirstSpanOffset(text)),document,task2NeMap.get(NEtypes.get(i)),bogusNER2Reason);
                       uploadData.add(task2_datum);
                   }

               }//else ignore this sentence
           }else
           {
               //System.out.println("elem:" + elem);
               //majority voting for
               for (JsonNode judgment : elem.path("results").path("judgments"))
               {
                   if(!judgment.path("data").path("markertext").isMissingNode())
                   {
                       String markertext = judgment.path("data").path("markertext").getTextValue();
                       System.out.println("markertext votes:" + markertext);
                   }else
                   {
                       LOG.warn("Warning, missing path in JSON result file from crowdflower: results/judgments");
                   }
               }
           }
       }

       LOG.info("Data generation complete. Creating new Job for Ner task 2.");
       CrowdJob job = createJob(template);
       LOG.info("Done, new job id is: "+job.getId()+". Now generating data for NER task 2");

       crowdclient.uploadData(job,uploadData);

       LOG.info("Done uploading data to task2 #"+job.getId()+".");

       return job.getId();
   }

}
