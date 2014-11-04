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
import static org.apache.uima.fit.util.JCasUtil.select;
import static org.apache.uima.fit.util.JCasUtil.selectCovered;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Serializable;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
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

/**
 * Class to manage data generation for German Named Entity tasks on Crowdflower.
 * Uses Crowdclient to upload and create jobs.
 * @author Benjamin Milde
 */
public class NamedEntityTaskManager
    implements Serializable
{

    private static final String JSON_VALUE_NONE2 = "\"none\"";
    private static final String JSON_VALUE_NONE1 = "none";
    private static final String JSON_VALUE_HIDDEN_GOLD = "hidden_gold";
    private static final String JSON_VALUE_GOLDEN = "golden";
    private static final String JSON_FIELD_JUDGMENTS = "judgments";
    private static final String HTML_OPEN_SPAN = "<span ";
    private static final String HTML_SPAN_TOKEN_CLOSE = "\">";
    private static final String HTML_SPAN_CLOSE = "</span>";
    private static final String HTML_SPAN_TOKEN_START = "<span id=\"token=";
    private static final String JSON_SOURCEDOC_SHORTFORM = "S";
    private static final String JSON_VALUE_EMPTY_ARRAY = "[]";
    private static final String JSON_VALUE_NONE_MARKER = "[\"none\"]";
    private static final String JSON_GOLDDOC_SHORTFORM = "G";
    private static final String JSON_TRUE = "TRUE";
    private static final String COUNTRY_CODE_CH = "CH";
    private static final String COUNTRY_CODE_AT = "AT";
    private static final String COUNTRY_CODE_DE = "DE";
    private static final String JSON_FIELD_STATUS_NEEDED_JUDGMENTS = "needed_judgments";
    private static final String JSON_FIELD_STATUS_ALL_JUDGMENTS = "all_judgments";
    private static final String JSON_FIELD_DONE = "done";
    private static final String JSON_FIELD_COUNT = "count";
    private static final String JSON_FIELD_ERROR = "error";
    //JSON field constant
    private static final String JSON_FIELD_END_MARKER = "e";
    private static final String JSON_FIELD_START_MARKER = "s";
    private static final String JSON_FIELD_RESULTS = "results";
    private static final String JSON_FIELD_AGGREGATED = "agg";
    private static final String JSON_FIELD_FINALIZED = "finalized";
    private static final String JSON_FIELD_STATE = "state";
    private static final String JSON_FIELD_DOCUMENT = "document";
    private static final String JSON_FIELD_DATA = "data";

    private static final String malformedStatusErrorMsg = "error retrieving status: malformed response from Crowdflower";

    private static final long serialVersionUID = -166689748276508297L;

    private CrowdClient crowdclient;

    private static final String noGoldNER1Reason = "Dieser Textabschnitt beinhaltet keine Named Entities (in solchen F&auml;llen m&uuml;ssen sie auf den daf&uuml;r vorgesehen Button klicken. Es reicht nicht aus nichts zu makieren). Sie k&ouml;nnen diese Meinung anfechten und uns erkl&auml;ren warum Sie meinen das dies falsch ist.\n";
    private static final String goldNER1ReasonHints = "\n <br/> Tipps: Wenn eine Named Entity l&auml;nger als ein Wort lang ist, m&uuml;ssen sie beim ersten Wort anfangen zu makieren und beim letzten Wort loslassen. Falsch ist: Klicken Sie stattdessen auf die W&ouml;rter einzeln, erzeugt dies mehrere einzelne Makierungen! Sie k&ouml;nnen auch nur bis z.B. zu der H&auml;lfte eines Wortes makieren, dies erfasst trotzdem das ganze Wort. \n";

    private static final String bogusNER2Reason = "Wenn sie anderer Meinung sind, k&ouml;nnen Sie unsere Meinung anfechten.";

    private static Map<String, String> task2NeMap;

    private int lastGoldOffset = 0;

    //omitted sentences in the last run because of errors
    private int omittedSentences = 0;

    //omitted entites in the last run because of errors
    private int omittedEntities = 0;

    public int getOmittedSentences()
    {
        return omittedSentences;
    }

    public void setOmittedSentences(int omittedSentences)
    {
        this.omittedSentences = omittedSentences;
    }

    public int getOmittedEntities()
    {
        return omittedEntities;
    }

    public void setOmittedEntities(int omittedEntities)
    {
        this.omittedEntities = omittedEntities;
    }

    //Static mapping of WebAnno short forms to user displayed types in Crowdflower
    //used for gold

    //this could be made configurable to allow more flexibility

    static {
        task2NeMap = new HashMap<String, String>();
        task2NeMap.put("PER", "Person");
        task2NeMap.put("PERderiv","Indirekte Person");
        task2NeMap.put("PERpart","Person");
        task2NeMap.put("ORG", "Organisation");
        task2NeMap.put("ORGderiv", "Organisation");
        task2NeMap.put("ORGpart", "Partielle Organisation");
        task2NeMap.put("LOC", "Ort");
        task2NeMap.put("LOCderiv", "Indirekter Ort");
        task2NeMap.put("LOCpart", "Ort");
        task2NeMap.put("OTH", "Etwas anderes");
        task2NeMap.put("OTHderiv", "Etwas anderes");
        task2NeMap.put("OTHpart", "Etwas anderes");
    }

    private static Map<String, String> ne2TaskMap;

    // static mapping of user displayed types in Crowdflower to WebAnno short forms
    static {
        ne2TaskMap = new HashMap<String, String>();
        ne2TaskMap.put("Person", "PER");
        ne2TaskMap.put("Indirekte Person", "PERderiv");
        ne2TaskMap.put("Organisation", "ORG");
        ne2TaskMap.put("Partielle Organisation", "ORGpart");
        ne2TaskMap.put("Ort", "LOC");
        ne2TaskMap.put("Indirekter Ort", "LOCderiv");
        ne2TaskMap.put("Etwas anderes", "OTH");
    }

    /**
     * Default constructor
     */
    public NamedEntityTaskManager()
    {
        crowdclient = new CrowdClient();
    }

    /**
     * Generates a new job on crowdflower.com based on the supplied template string. The new job
     * won't have any data items
     *
     * @param template - template as JSON string to use for the new job
     * @return the job.
     * @throws JsonProcessingException hum?
     * @throws IOException hum?s
     */
    public CrowdJob createJob(String template)
        throws JsonProcessingException, IOException
    {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonTemplate = mapper.readTree(template);
        CrowdJob job = new CrowdJob(jsonTemplate);
        return crowdclient.createNewJob(job);
    }

    /**
     * Helper function to concatenate a list of strings with the supplied separator
     * {"item","item3","test"} -> "item,item3,test"
     *
     * @param words the words.
     * @param separator the separator.
     * @return string the result.
     */
    private static String concatWithSeparator(Collection<String> words, String separator)
    {
        StringBuilder wordList = new StringBuilder();
        for (String word : words) {
            wordList.append(new String(word) + separator);
        }
        return new String(wordList.deleteCharAt(wordList.length() - 1));
    }

    /**
     * Generate data for NER task1. This is a quite specific function exclusively for German NER,
     * but a more general version could one day replace this.
     *
     * You usually call this function twice, one time to generate gold data (generateGold=true) and one time to generate normal data
     *
     * @param documentsJCas
     *            List of Cas containing either the documents to be annotated or gold documents
     * @param goldOffset
     *            This is an offset to the token number that is send to Crowdflower, so that (i -
     *            goldOffset) = real token offset in webanno. This is needed so that continuous
     *            token numbers can be send to Crowdflower
     * @param generateGold - whether gold data should be produced or normal data
     * @param limit - limit number of data items to this number
     * @return {@code List<NamedEntityTask1Data>}, representing a crowdflower task1 data upload. It can be directly mapped to JSON.
     * @throws CrowdException hum?
     */
    public List<NamedEntityTask1Data> generateTask1Data(List<JCas> documentsJCas, int goldOffset,
            boolean generateGold, int limit)
        throws CrowdException
    {
        Log LOG = LogFactory.getLog(getClass());

        List<NamedEntityTask1Data> data = new ArrayList<NamedEntityTask1Data>();
        int i = goldOffset;
        StringBuilder textBuilder = new StringBuilder();
        int docNo = 0;

        jcasloop: for (JCas documentJCas : documentsJCas) {
            int offset = i;

            int sentenceNo = 0;
            LOG.info("Generating data for document: " + docNo + "/" + documentsJCas.size());

            for (Sentence sentence : select(documentJCas, Sentence.class)) {

                // if global limit of sentences reached, abort whole iteration
                if (limit != -1 && sentenceNo >= limit) {
                    break jcasloop;
                }

                textBuilder.setLength(0);

                // Maps our own token offsets (needed by JS in the crowdflower task) to Jcas offsets
                Map<Integer, Integer> charOffsetStartMapping = new HashMap<Integer, Integer>();
                Map<Integer, Integer> charOffsetEndMapping = new HashMap<Integer, Integer>();

                for (Token token : selectCovered(Token.class, sentence)) {

                    // check that token offsets match to (i - goldOffset)
                    //String tokenString = ;
                    textBuilder.append(HTML_SPAN_TOKEN_START);
                    textBuilder.append(String.valueOf(i));
                    textBuilder.append(HTML_SPAN_TOKEN_CLOSE);
                    textBuilder.append(escapeHtml(token.getCoveredText()));
                    textBuilder.append(" ");
                    textBuilder.append(HTML_SPAN_CLOSE);

                    charOffsetStartMapping.put(token.getBegin(), i);
                    charOffsetEndMapping.put(token.getEnd(), i);

                    i++;
                }

                String text = textBuilder.toString();

                // clear string builder
                textBuilder.setLength(0);

                NamedEntityTask1Data task1Data = new NamedEntityTask1Data(text);
                task1Data.setOffset(offset);
                task1Data.setDocument(JSON_SOURCEDOC_SHORTFORM + String.valueOf(docNo));

                if (generateGold) {
                    List<String> goldElems = new ArrayList<String>();
                    List<String> goldTokens = new ArrayList<String>();
                    List<String> goldTypes = new ArrayList<String>();

                    int lastNeBegin = -1;
                    int lastNeEnd = -1;

                    for (NamedEntity namedEntity : selectCovered(NamedEntity.class, sentence)) {
                        List<Token> tokens = selectCovered(documentJCas, Token.class,
                                namedEntity.getBegin(), namedEntity.getEnd());

                        List<String> strTokens = new ArrayList<String>();

                        // transform List<Tokens> to List<Strings>
                        for (Token t : tokens) {
                            strTokens.add(new String(t.getCoveredText()));
                        }

                        String strToken = concatWithSeparator(strTokens, " ");
                        String type = namedEntity.getValue();

                        int neBegin = namedEntity.getBegin();
                        int neEnd = namedEntity.getEnd();
                        String strElem = buildTask1TokenJSON(textBuilder, namedEntity,
                                charOffsetStartMapping, charOffsetEndMapping);

                        // handling of nested NEs
                        if (lastNeEnd != -1 && neBegin <= lastNeEnd) {
                            // this NE is bigger = better
                            if ((neEnd - neBegin) > (lastNeEnd - lastNeBegin)) {
                                // remove last NE
                                goldElems.remove(goldElems.size() - 1);
                                goldTokens.remove(goldElems.size() - 1);
                                goldTypes.remove(goldElems.size() - 1);

                                // add new NE
                                goldElems.add(strElem);
                                goldTokens.add(strToken);
                                goldTypes.add(type);

                                lastNeBegin = neBegin;
                                lastNeEnd = neEnd;
                            }// else ignore this NE, keep last one
                        }
                        else {
                            // standard case, no nested NE, or first NE of nested NEs
                            goldElems.add(strElem);
                            goldTokens.add(strToken);
                            goldTypes.add(type);

                            lastNeBegin = neBegin;
                            lastNeEnd = neEnd;
                        }
                    }


                    String strTypes = JSON_VALUE_EMPTY_ARRAY;

                    String strGold = JSON_VALUE_NONE_MARKER;
                    String strGoldReason = noGoldNER1Reason;
                    int difficulty = 1;

                    // Case where we have gold elements and want to give feedback to the crowd user
                    if (goldElems.size() > 0) {
                        strGold = buildTask1GoldElem(textBuilder, goldElems);

                        // Difficulty is used to hint Crowdflower that more difficult sentences (the
                        // ones
                        // where users must mark many NEs) are displayed with less probability.

                        difficulty = goldElems.size();

                        strTypes = buildTask1GoldElem(textBuilder, goldTypes);
                        strGoldReason = buildTask1GoldElemReason(textBuilder, goldTokens);
                    }

                    task1Data.set_difficulty(difficulty);
                    task1Data.setMarkertext_gold(strGold);
                    task1Data.setMarkertext_gold_reason(strGoldReason);

                    task1Data.setTypes(strTypes);
                    task1Data.setDocument(JSON_GOLDDOC_SHORTFORM + String.valueOf(docNo));

                    // Marker flag for crowdflower that this data is gold data.
                    // Note: Users still need to click on "convert uploaded gold" manually in the
                    // interface.
                    task1Data.set_golden(JSON_TRUE);
                }

                data.add(task1Data);
                sentenceNo++;
            }
            docNo++;
        }

        if(generateGold)
        {
            lastGoldOffset = i;
        }

        return data;
    }

    /**
     * Helper method for generateTask1Data, builds a string that explains to the user which named
     * entities he had to select
     *
     * @param textBuilder
     * @param goldTokens
     * @return a string that explains to the user which named
     *         entities he had to select to get the answer right
     */
    private String buildTask1GoldElemReason(StringBuilder textBuilder, List<String> goldTokens)
    {
        String strGoldReason;
        textBuilder.setLength(0);
        textBuilder.append("Der Text beinhaltet ");
        textBuilder.append(goldTokens.size());
        textBuilder.append(" Named Entiti(es): ");
        textBuilder.append(escapeHtml(concatWithSeparator(goldTokens, ", ")));
        textBuilder.append(goldNER1ReasonHints);

        strGoldReason = new String(textBuilder);
        return strGoldReason;
    }

    /**
     * Helper method for generateTask1Data, concatenates all JSON formatted tokens (sorted by
     * position) which is then the gold solution and contains markers to all NE in the text
     * fragment.
     *
     * This same format is produced by the JS in the crowdflower.com task1.
     *
     * @param textBuilder - reuse this textbuilder
     * @param goldElems - string gold elements (JSON), should be sorted
     * @return JSON string containing list of gold item marker positions
     */
    private String buildTask1GoldElem(StringBuilder textBuilder, List<String> goldElems)
    {
        String strGold;
        textBuilder.setLength(0);
        // strGold = "["+concatWithSeperator(goldElems,",")+"]";
        textBuilder.append("[");
        textBuilder.append(concatWithSeparator(goldElems, ","));
        textBuilder.append("]");
        strGold = new String(textBuilder);
        return strGold;
    }

    /**
     * Helper method for generateTask1Data, builds a single JSON formatted element describing start
     * and end position of a named entity.
     *
     * @param textBuilder - reuse this textbuilder
     * @param namedEntity - namedEntity which should mapped to positions formatted to JSON
     * @charOffsetStartMapping - mapping for start positions to use for WebAnno offset to Crowdflower token offset
     * @charOffsetEndMapping - mapping for end positions to use for WebAnno offset to Crowdflower token offset
     * @return JSON formatted string describing positions of single marker
     * @throws CrowdException
     */
    private String buildTask1TokenJSON(StringBuilder textBuilder, NamedEntity namedEntity,
            Map<Integer, Integer> charOffsetStartMapping,
            Map<Integer, Integer> charOffsetEndMapping)
        throws CrowdException
    {
        // JSON named enitity marker for the gold data. The JSON here gets enclosed into the data
        // JSON that is uploaded
        // "{\"s\":"+begin+",\"e\":"+end+"}"
        if (!charOffsetStartMapping.containsKey(namedEntity.getBegin())
                || !charOffsetEndMapping.containsKey(namedEntity.getEnd())) {
            throw new CrowdException(
                    "Data generation error: char offset to token mapping is inconsistent. Contact developpers!");
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
     *
     * @param key - API key as string
     */
    public void setAPIKey(String key)
    {
        Log LOG = LogFactory.getLog(getClass());
        LOG.info("Using apiKey:" + key);
        crowdclient.setApiKey(key);
    }

    public String uploadNewNERTask1(String template, List<JCas> documentsJCas, List<JCas> goldsJCas)
        throws JsonProcessingException, IOException, Exception
    {
        return uploadNewNERTask1(template, documentsJCas, goldsJCas, -1, -1);
    }

    /**
     * Upload a new NER task1 (German) to crowdflower.com. This method is called from the crowd page
     * and is the starting point for a new task1 upload.
     *
     * @param template - template as string to be used to upload the job
     * @param documentsJCas - list of source documents to be annotated
     * @param goldsJCas - gold data which should be used
     * @param useSents - number of sentences to use
     * @param useGoldSents - number of gold sentences to use
     * @return the job ID.
     * @throws JsonProcessingException hum?
     * @throws IOException hum?
     * @throws Exception hum?
     */

    public String uploadNewNERTask1(String template, List<JCas> documentsJCas,
            List<JCas> goldsJCas, int useSents, int useGoldSents)
        throws JsonProcessingException, IOException, Exception
    {
        Log LOG = LogFactory.getLog(getClass());
        LOG.info("Creating new Job for Ner task 1.");
        CrowdJob job = createJob(template);
        LOG.info("Done, new job id is: " + job.getId() + ". Now generating data for NER task 1");

        setAllowedCountries(job);
        crowdclient.updateAllowedCountries(job);

        int goldOffset = 0;

        List<NamedEntityTask1Data> goldData = new ArrayList<NamedEntityTask1Data>();

        // If we have gold data, than generate data for it
        if (goldsJCas != null && goldsJCas.size() > 0) {
            LOG.info("Gold data available, generating gold data first.");
            goldData = generateTask1Data(goldsJCas, 0, true, useGoldSents);
            //Gold offset is measured in tokens
            goldOffset = this.lastGoldOffset;
        }

        LOG.info("Generate normal task data.");
        List<NamedEntityTask1Data> data = generateTask1Data(documentsJCas, goldOffset, false,
                useSents);

        List<NamedEntityTask1Data> mergedData = new ArrayList<NamedEntityTask1Data>();

        if (goldsJCas != null && goldsJCas.size() > 0) {

            mergedData.addAll(goldData);
        }

        mergedData.addAll(data);

        LOG.info("Job data prepared, starting upload.");
        LOG.info("Uploading data to job #" + job.getId());
        crowdclient.uploadData(job, mergedData);
        LOG.info("Done, finished uploading data to #" + job.getId());
        return job.getId();
    }

    /**
     * Sets the default countries
     * @param job
     */
    private void setAllowedCountries(CrowdJob job)
    {
        // by default, allow only German speaking countries
        // would be better to make this configurable
        Vector<String> includedCountries = new Vector<String>();
        includedCountries.add(COUNTRY_CODE_DE);
        includedCountries.add(COUNTRY_CODE_AT);
        includedCountries.add(COUNTRY_CODE_CH);
        job.setIncludedCountries(includedCountries);
    }

    /**
     * Describe the current status, given two job IDs. They can also be the empty string, which means
     * @param jobID1 - Job ID or empty string
     * @param jobID2 - Job ID or empty string
     * @return status string for both job IDs
     */
    public String getStatusString(String jobID1, String jobID2)
    {
        String line;
        // first case: no job ids
        if (jobID1.equals("") && jobID2.equals("")) {
            return "No jobs uploaded.";
        }
        // second case, got first job id
        else if (!jobID1.equals("") && jobID2.equals("")) {
            line = "Job1: " + getStatusString(jobID1);
        }
        else if(!jobID1.equals("") && !jobID2.equals("")){
            line = "Job2: " + getStatusString(jobID2);
        }else
        {
            line = "error retrieving status";
        }
        return line;
    }

    /**
     * Get status string for just one job id
     * @param jobId - Crowdflower job ID
     * @return status string describing the current status on crowdflower
     */
    private String getStatusString(String jobId)
    {
        Log LOG = LogFactory.getLog(getClass());
        JsonNode uploadStatus;
        try{
        uploadStatus = crowdclient.getUploadStatus(jobId);
        }catch (Exception e)
        {
            return "Error while trying to connect to Crowdflower: " + e.getMessage();
        }

        String line;
        int uploadedUnits = -1;
        boolean finsishedUpload = false;
        int judgments = 0;
        int neededJudgments = 0;

        //Crowdflower reported an error in its JSON output
        if (uploadStatus != null && uploadStatus.has(JSON_FIELD_ERROR))
        {
            return "error retrieving status: " + uploadStatus.path(JSON_FIELD_ERROR).getTextValue();
        }

        //No error and JSON has required fields
        if (uploadStatus != null && uploadStatus.has(JSON_FIELD_COUNT) && uploadStatus.has(JSON_FIELD_DONE)) {
            uploadedUnits = uploadStatus.path(JSON_FIELD_COUNT).getIntValue();
            LOG.info("status job1:" + uploadStatus.toString());
            finsishedUpload = uploadStatus.path(JSON_FIELD_DONE).getBooleanValue();
            if(finsishedUpload)
            {
                JsonNode status;
                //retrieve judgment stats
                try{
                status = crowdclient.getStatus(jobId);
                }catch (Exception e)
                {
                    return "Error while trying to connect to Crowdflower: " + e.getMessage();
                }
                if(status != null && status.has(JSON_FIELD_STATUS_ALL_JUDGMENTS) && status.has(JSON_FIELD_STATUS_NEEDED_JUDGMENTS))
                {
                    judgments = status.path(JSON_FIELD_STATUS_ALL_JUDGMENTS).getIntValue();
                    neededJudgments = status.path(JSON_FIELD_STATUS_NEEDED_JUDGMENTS).getIntValue();
                }
                else {
                    return malformedStatusErrorMsg;
                }
            }
        }
        else {
            return malformedStatusErrorMsg;
        }

        line = "is uploaded and has "
                + uploadedUnits
                + " uploaded units. "
                + (finsishedUpload ? "There are " + judgments + " judgments. Needed to complete job: "+neededJudgments+")"
                        : "Crowdflower is still processing the upload." );
        return line;
    }

    /**
     * Helper function for uploadNewNERTask2: Get the string char position of the i-th span from the
     * HTML token spans that the Crowdflower job task1 uses to display a textfragment.
     *
     * @param spans - HTML span string
     * @param spannumber - the span number to extract the position for
     * @return index in spans string for the n-th occurence of a span
     * @throws Exception
     */
    private int getSpanPos(String spans, int spannumber)
        throws IndexOutOfBoundsException
    {
        // find all occurrences <span> forward
        int count = 0;
        for (int i = -1; (i = spans.indexOf(HTML_OPEN_SPAN, i + 1)) != -1;) {
            if (spannumber == count) {
                return i;
            }
            count++;
        }
        throw new IndexOutOfBoundsException("Token index not found in getSpanPos:" + spannumber + " in span: " + spans);
    }

    /**
     * Helper function for uploadNewNERTask2: Extract certain token spans from the HTML token spans that
     * the Crowdflower job task1 uses to display a text fragment. It is used to cut out a named entity from HTML token spans.
     *
     * @param spans - HTML span string
     * @param start - token number
     * @param end - token number
     * @return Substring of spans containing the tokens starting at start token and ending at end token
     * @throws IndexOutOfBoundsException
     */
    private String extractSpan(String spans, int start, int end) throws IndexOutOfBoundsException
    {
        int offset = getFirstSpanOffset(spans);

        assert (start >= offset);
        assert (end >= offset);

        // so that the text has a span beyond the last one
        spans += HTML_OPEN_SPAN;

        int substart = getSpanPos(spans, start - offset);
        int subend = getSpanPos(spans, end - offset + 1);

        return spans.substring(substart, subend);
    }

    /**
     * Helper function that retrieves the first token number in a task1 html span
     * @param spans
     * @return first number in span offset string
     */
    private int getFirstSpanOffset(String spans)
    {
        String firstNum = "0";
        boolean foundDigit = false;

        // span beginn will look like: "<span id='token=num'>", so will just search the first number
        // in the string

        /*
         * regex for this would be:
         * Pattern p = Pattern.compile("(^|\\s)([0-9]+)($|\\s)");
         * Matcher m = p.matcher(s);
         * if (m.find()) { String num = m.group(2); }
         */

        // but hey, extractSpan gets called a lot, this is faster:

        for (int i = 0; i < spans.length(); i++) {
            if (Character.isDigit(spans.charAt(i))) {
                foundDigit = true;
                firstNum = firstNum + Character.toString(spans.charAt(i));
            }
            else if (foundDigit && !Character.isDigit(spans.charAt(i))) {
                break;
            }
        }

        int offset = Integer.valueOf(firstNum);
        return offset;
    }

    /**
     * Uploads a new task2 to Crowdflower, producing all data entirely of the raw judgments file
     * retrieved from a task1 ID.
     *
     * @param template the template.
     * @param jobID1 the job ID.
     * @param documentsJCas the documents.
     * @param goldsJCas the gold documents.
     * @return Crowdflower ID as string of the new task
     * @throws JsonProcessingException hum?
     * @throws IOException hum?
     * @throws CrowdException hum?
     */
    public String uploadNewNERTask2(String template, String jobID1, List<JCas> documentsJCas,
            List<JCas> goldsJCas)
        throws JsonProcessingException, IOException, CrowdException
    {
        Log LOG = LogFactory.getLog(getClass());
        omittedSentences = 0;

        // Reader that also downloades the raw judgments for the supplied job id
        BufferedReader br = getReaderForRawJudgments(jobID1);
        String line;

        // JSON object mapper
        ObjectMapper mapper = new ObjectMapper();
        ObjectWriter writer = mapper.writer();

        // Used to represent task2 data that is send as JSON to crowdflower
        Vector<NamedEntityTask2Data> uploadData = new Vector<NamedEntityTask2Data>();

        // Judgments come in as a quite exotic multiline-json, we need to parse every line of it
        // separately
        while ((line = br.readLine()) != null) {
            // try to process each line, omit data if an error occurs (but inform user)
            try {
                JsonNode elem = mapper.readTree(line);
                String text = elem.path(JSON_FIELD_DATA).path(NamedEntityTask1Data.FIELD_TEXT).getTextValue();
                String state = elem.path(JSON_FIELD_STATE).getTextValue();

                // omit hidden gold items
                if (state.equals(JSON_VALUE_HIDDEN_GOLD)) {
                    continue;
                }

                String document = elem.path(JSON_FIELD_DATA).path(JSON_FIELD_DOCUMENT).getTextValue();
                int offset = elem.path(JSON_FIELD_DATA).path(NamedEntityTask1Data.FIELD_OFFSET).getIntValue();

                if (state.equals(JSON_VALUE_GOLDEN)) {
                    // produce gold data
                    String markertext_gold = elem.path(JSON_FIELD_DATA).path(NamedEntityTask1Data.FIELD_MARKERTEXT_GOLD)
                            .getTextValue();
                    String types = elem.path(JSON_FIELD_DATA).path(NamedEntityTask1Data.FIELD_TYPES).getTextValue();

                    if (!types.equals(JSON_VALUE_EMPTY_ARRAY)) {
                        // sentence has atleast one NE
                        List<String> NEtypes = Arrays.asList(types.substring(1, types.length() - 1)
                                .split(","));

                        JsonNode markers = mapper.readTree(markertext_gold);

                        if (NEtypes.size() != markers.size()) {
                            LOG.warn("Warning, skipping ill formated gold item in task1! (NEtypes.size() != markers.size())");
                            continue;
                        }

                        int i = 0;
                        for (JsonNode marker : markers) {
                            int start = marker.path(JSON_FIELD_START_MARKER).getIntValue();
                            int end = marker.path(JSON_FIELD_END_MARKER).getIntValue();

                            NamedEntityTask2Data task2_gold_datum = new NamedEntityTask2Data(text,
                                    extractSpan(text, start, end),
                                    writer.writeValueAsString(marker),
                                    String.valueOf(getFirstSpanOffset(text)), document,
                                    task2NeMap.get(NEtypes.get(i)), bogusNER2Reason);

                            task2_gold_datum.setDocOffset(offset);
                            uploadData.add(task2_gold_datum);
                            i++;
                        }
                    }// else ignore this sentence
                }
                else // normal data entry
                {
                    if (!elem.path(JSON_FIELD_RESULTS).path(JSON_FIELD_JUDGMENTS).isMissingNode()) {
                        Map<String, Integer> votings = new HashMap<String, Integer>();
                        // Majority voting for each marker in all judgments
                        for (JsonNode judgment : elem.path(JSON_FIELD_RESULTS).path(JSON_FIELD_JUDGMENTS)) {
                            if (!judgment.path(JSON_FIELD_DATA).path(NamedEntityTask1Data.FIELD_MARKERTEXT).isMissingNode()) {
                                String markertext = judgment.path(JSON_FIELD_DATA).path(NamedEntityTask1Data.FIELD_MARKERTEXT)
                                        .getTextValue();

                                JsonNode markers = mapper.readTree(markertext);

                                // iterate over votes
                                for (JsonNode marker : markers) {
                                    String voteText = writer.writeValueAsString(marker);

                                    // first case: add entry for this voting position
                                    if (!votings.containsKey(voteText)) {
                                        votings.put(voteText, 1);
                                    }// second case: increment voting
                                    else {
                                        votings.put(voteText, votings.get(voteText) + 1);
                                    }
                                }
                            }
                            else {
                                LOG.warn("Warning, missing path in JSON result file from crowdflower: results/judgments");
                            }
                        }
                        // Consider any marked span which has at least two votes. Bogus spans can still be filtered out by task2
                        int votes_needed = 2;

                        List<String> majorityMarkers = new ArrayList<String>();

                        for (String vote : votings.keySet()) {
                            if (votings.get(vote) >= votes_needed) {
                                majorityMarkers.add(vote);
                            }
                        }

                        // process majority markers
                        for (String strMarker : majorityMarkers) {
                            if (!strMarker.equals(JSON_VALUE_NONE1) && !strMarker.equals(JSON_VALUE_NONE2)) {
                                JsonNode marker = mapper.readTree(strMarker);
                                int start = marker.path(JSON_FIELD_START_MARKER).getIntValue();
                                int end = marker.path(JSON_FIELD_END_MARKER).getIntValue();

                                NamedEntityTask2Data task2_datum = new NamedEntityTask2Data(text,
                                        extractSpan(text, start, end), strMarker,
                                        String.valueOf(getFirstSpanOffset(text)), document);
                                task2_datum.setDocOffset(offset);
                                uploadData.add(task2_datum);
                            }
                        }

                    }
                    else {
                        LOG.warn("Warning, missing path in JSON result file from crowdflower: data/markertext");
                    }
                }
            }
            catch (Exception e) {
                omittedSentences++;
                LOG.warn("Warning, omitted a sentence from task2 upload because of an error in processing it: "
                        + e.getMessage());
            }
        }

        LOG.info("Data generation complete. Creating new Job for Ner task 2.");
        CrowdJob job = createJob(template);
        setAllowedCountries(job);
        crowdclient.updateAllowedCountries(job);
        LOG.info("Done, new job id is: " + job.getId() + ". Now generating data for NER task 2");

        crowdclient.uploadData(job, uploadData);

        LOG.info("Done uploading data to task2 #" + job.getId() + ".");

        return job.getId();
    }

    /**
     * Helper function for uploadNewNERTask2 and retrieveAggJudgmentsTask2 that retrieves the raw
     * judgments for the supplied ID and returns a buffered reader for it.
     *
     * @param jobID
     * @return Buffered reader for raw judgment data (unzipped)
     * @throws UnsupportedEncodingException
     * @throws IOException
     * @throws CrowdException
     */

    private BufferedReader getReaderForRawJudgments(String jobID)
        throws UnsupportedEncodingException, IOException, CrowdException
    {
        Log LOG = LogFactory.getLog(getClass());
        LOG.info("Retrieving data for job: " + jobID);

        // Retrieve job data
        CrowdJob job = crowdclient.retrieveJob(jobID);

        LOG.info("Retrieving raw judgments for job: " + jobID);
        // Rawdata is a multiline JSON file
        String rawdata = crowdclient.retrieveRawJudgments(job);

        // This may happen if the server didn't have enough time to prepare the data.
        // (Usually the case for anything else than toy datasets)
        if (rawdata == null || rawdata.equals("")) {
            throw new CrowdException(
                    "No data retrieved for task1 at #"
                            + jobID
                            + ". Crowdflower might need more time to prepare your data, try again in one minute.");
        }

        LOG.info("Got " + rawdata.length() + " chars");

        StringReader reader = new StringReader(rawdata);
        BufferedReader br = new BufferedReader(reader);
        return br;
    }

    /**
     * Aggregates and sets final judgments in the JCases provided by documentsJCas.
     *
     * @param jobID2 the job ID.
     * @param documentsJCas the documents.
     * @throws IOException hum?
     * @throws UnsupportedEncodingException hum?
     * @throws CrowdException hum?
     */

    public void setCrowdJobAnnotationsInDocs(String jobID2, List<JCas> documentsJCas)
        throws UnsupportedEncodingException, IOException, CrowdException
    {
        omittedEntities = 0;

        Log LOG = LogFactory.getLog(getClass());

        BufferedReader br = getReaderForRawJudgments(jobID2);
        String line;

        // Jackson JSON object mapper
        ObjectMapper mapper = new ObjectMapper();

        // Maps our own token offsets (needed by JS in the crowdflower task) to Jcas offsets
        // One map for start and end offset conversion each and new mappings for each document (to be able to use multiple documents)
        // Array position is token number
        List<List<Integer>> charStartMappings = new ArrayList<List<Integer>>();
        List<List<Integer>> charEndMappings = new ArrayList<List<Integer>>();

        for(JCas cas : documentsJCas)
        {
            List<Integer> charStartMapping = new ArrayList<Integer>();
            List<Integer> charEndMapping = new ArrayList<Integer>();
            for (Sentence sentence : select(cas, Sentence.class))
            {
                for (Token token : selectCovered(Token.class, sentence))
                {
                    charStartMapping.add(token.getBegin());
                    charEndMapping.add(token.getEnd());
                }
            }
            charStartMappings.add(charStartMapping);
            charEndMappings.add(charEndMapping);
        }

        while ((line = br.readLine()) != null) {
            // Try to process each line, omit data if an error occurs
            try {
                JsonNode elem = mapper.readTree(line);

                // Document string contains one char to specify type (golden vs. not golden)
                // and a new number starting at 0 for each new document
                int documentNo = Integer.valueOf(elem.path(JSON_FIELD_DATA).path(JSON_FIELD_DOCUMENT).getTextValue().substring(1));
                if (documentNo >= documentsJCas.size())
                {
                    throw new CrowdException("Error, number of documents changed from first upload! Tried to access document: " + documentNo);
                }

                //Only process elements that are finalized, i.e. elements that don't have missing judgments
                String state = elem.path(JSON_FIELD_STATE).getTextValue();
                if (state.equals(JSON_FIELD_FINALIZED)) {

                    JCas cas = documentsJCas.get(documentNo);

                    String typeExplicit = elem.path(JSON_FIELD_RESULTS).path(NamedEntityTask2Data.FIELD_TODECIDE_RESULT)
                            .path(JSON_FIELD_AGGREGATED).getTextValue();
                    String type = ne2TaskMap.get(typeExplicit);

                    //Type is null when it is not in ne2TaskMap.
                    //These are usually difficult cases where workers are unsure or where a wrong word has been marked in task1
                    if (type == null)
                    {
                        //We can simply skip any such cases
                        continue;
                    }

                    String posText = elem.path(JSON_FIELD_DATA).path(NamedEntityTask2Data.FIELD_POSTEXT).getTextValue();
                    int offset = 0;

                    // Element numbering in Crowdflower judgments can be different than token numbering in the Cas,
                    // to support an always incrementing numbering in a multi-document setting for displayed spans (task1)
                    // docOffset is the diff of the marker (saved from task1 and also present in task2 data)
                    // to token numbering of the Cas for the current document

                    if (!elem.path(JSON_FIELD_DATA).path(NamedEntityTask2Data.FIELD_DOCOFFSET).isMissingNode()) {
                        offset = elem.path(JSON_FIELD_DATA).path(NamedEntityTask2Data.FIELD_DOCOFFSET).getIntValue();
                    }

                    JsonNode marker = mapper.readTree(posText);
                    int start = marker.path(JSON_FIELD_START_MARKER).getIntValue();
                    int end = marker.path(JSON_FIELD_END_MARKER).getIntValue();

                    //Map named entity to character offsets and add it to the Cas
                    NamedEntity newEntity = new NamedEntity(cas, charStartMappings.get(documentNo).get(start
                            - offset), charEndMappings.get(documentNo).get(end - offset));
                    newEntity.setValue(type);
                    newEntity.addToIndexes();
                }

            }
            //We catch all exceptions here, in order to guarantee best effort in processing the data.
            //That is, we try to assign to every sentence and don't stop if there is a problem and inform the user afterwards.
            catch (Exception e) {
                omittedEntities++;
                LOG.warn("Warning, omitted a sentence from task2 import because of an error in processing it: "
                        + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}
