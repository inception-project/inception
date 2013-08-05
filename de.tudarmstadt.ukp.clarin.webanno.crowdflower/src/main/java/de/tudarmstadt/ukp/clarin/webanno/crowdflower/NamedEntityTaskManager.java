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

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.uima.jcas.JCas;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.ObjectMapper;

import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;

public class NamedEntityTaskManager implements Serializable
{

    private static final long serialVersionUID = -166689748276508297L;

    private CrowdClient crowdclient;

    private static final String noGoldNER1Reason = "Dieser Textabschnitt beinhaltet keine Named Entities (in solchen F&auml;llen m&uuml;ssen sie auf den daf&uuml;r vorgesehen Button klicken. Es reicht nicht aus nichts zu makieren). Sie k&ouml;nnen diese Meinung anfechten und uns erkl&auml;ren warum Sie meinen das dies falsch ist.\n";
    private static final String goldNER1ReasonHints = "\n <br/> Tipps: Wenn eine Named Entity l&auml;nger als ein Wort lang ist, m&uuml;ssen sie beim ersten Wort anfangen zu makieren und beim letzten Wort loslassen. Falsch ist: Klicken Sie stattdessen auf die W&ouml;rter einzeln, erzeugt dies mehrere einzelne Makierungen! Sie k&ouml;nnen auch nur bis z.B. zu der H&auml;lfte eines Wortes makieren, dies erfasst trotzdem das ganze Wort. \n";


    public NamedEntityTaskManager()
    {
        crowdclient = new CrowdClient();
        //TODO set API key
        //crowdclient.setApiKey()
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
  */
    public Vector<NamedEntityTask1Data> generateTask1Data(List<JCas>documentsJCas, int goldOffset, boolean generateGold)
    {
        Vector<NamedEntityTask1Data> data = new Vector<NamedEntityTask1Data>();
        int i=goldOffset;
        StringBuilder textBuilder = new StringBuilder();

        for (JCas documentJCas : documentsJCas)
        {
            int offset = i;
            //String text = "";

            for (Sentence sentence : select(documentJCas, Sentence.class)) {

                textBuilder.setLength(0);

                for (Token token : selectCovered(Token.class, sentence)) {

                    //check that token offsets match to (i - goldOffset)
                    String tokenString = new String(token.getCoveredText());
                    textBuilder.append("<span id=\"token=");
                    textBuilder.append(String.valueOf(i));
                    textBuilder.append("\">");
                    textBuilder.append(escapeHtml(tokenString));
                    textBuilder.append(" </span>");
                    i++;
                }

                String text = new String(textBuilder);

                //clear string builder
                textBuilder.setLength(0);

                //System.out.println(text);
                NamedEntityTask1Data task1Data = new NamedEntityTask1Data(text);
                task1Data.setOffset(offset);

                if (generateGold)
                {
                    ArrayList<String> goldElems = new ArrayList<String>();
                    ArrayList<String> goldTokens = new ArrayList<String>();

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

                        textBuilder.setLength(0);
                        textBuilder.append("{\"s\":");
                        textBuilder.append(namedEntity.getBegin());
                        textBuilder.append(",\"e\":");
                        textBuilder.append(namedEntity.getEnd());
                        textBuilder.append("}");

                        //JSON named enitity marker for the gold data. The JSON here gets enclosed into the data JSON that is uploaded
                        //String posElem = "{\"s\":"+strBegin+",\"e\":"+strEnd+"}";
                        String strToken = concatWithSeperator(strTokens," ");

                        goldElems.add(new String(textBuilder));
                        goldTokens.add(strToken);
                    }


                    //standard case: no gold elements
                    String strGold = "[]";
                    String strGoldReason = noGoldNER1Reason;

                    //case where we have gold elements and want to give feedback to the crowd user
                    if(goldElems.size() > 0)
                    {
                        textBuilder.setLength(0);
                        //strGold = "["+concatWithSeperator(goldElems,",")+"]";
                        textBuilder.append("[");
                        textBuilder.append(concatWithSeperator(goldElems,","));
                        textBuilder.append("]");
                        strGold = new String(textBuilder);

                        textBuilder.setLength(0);
                        textBuilder.append("Der Text beinhaltet ");
                        textBuilder.append(goldTokens.size());
                        textBuilder.append(" Named Entiti(es): ");
                        textBuilder.append(escapeHtml(concatWithSeperator(goldTokens,", ")));
                        textBuilder.append(goldNER1ReasonHints);

                        strGoldReason = new String(textBuilder);
                        //strGoldReason = "Der Text beinhaltet " + String.valueOf(goldTokens.size()) + " Named Entiti(es): " + concatWithSeperator(goldTokens,", ");
                        //strGoldReason += goldNER1ReasonHints;
                    }

                    task1Data.setMarkertext_gold_reason(strGoldReason);
                    task1Data.setMarkertext_gold(strGold);

                }

                data.add(task1Data);
            }
        }

        return data;
    }

    public void setAPIKey(String key)
    {
        Log LOG = LogFactory.getLog(getClass());
        LOG.info("Using apiKey:" + key);
        crowdclient.setApiKey(key);
    }

    public String uploadNewNERTask1(String template, List<JCas>documentsJCas , List<JCas>goldsJCas) throws JsonProcessingException, IOException, Exception
    {
        Log LOG = LogFactory.getLog(getClass());
        LOG.info("Creating new Job for Ner task 1.");
        CrowdJob job = createJob(template);
        LOG.info("Done, new job id is: "+job.getId()+". Now generating data for NER task 1");

        int goldOffset = 0;

        Vector<NamedEntityTask1Data> goldData = new Vector<NamedEntityTask1Data>();

        //if we have gold data, than generate data for it
        if(goldsJCas != null && goldsJCas.size() > 0)
        {
            goldData = generateTask1Data(documentsJCas,0,true);
            goldOffset = goldData.size();
        }

        Vector<NamedEntityTask1Data> data = generateTask1Data(documentsJCas,goldOffset,false);

        Vector<NamedEntityTask1Data> mergedData = new Vector<NamedEntityTask1Data>();

        if(goldsJCas != null && goldsJCas.size() > 0)
        {
            mergedData.addAll(goldData);
        }

        mergedData.addAll(data);

        LOG.info("Uploading data to job #" + job.getId());
        crowdclient.uploadData(job,mergedData);
        LOG.info("Done, finished uploading data to #" + job.getId());
        return job.getId();
    }

}
