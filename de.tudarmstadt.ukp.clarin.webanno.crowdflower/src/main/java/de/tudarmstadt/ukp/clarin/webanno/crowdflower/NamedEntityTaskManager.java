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

import static org.uimafit.util.JCasUtil.select;
import static org.uimafit.util.JCasUtil.selectCovered;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.uima.jcas.JCas;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.ObjectMapper;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;

public class NamedEntityTaskManager implements Serializable
{

    private static final long serialVersionUID = -166689748276508297L;

    private CrowdClient crowdclient;

    public NamedEntityTaskManager()
    {
        crowdclient = new CrowdClient();
        //TODO set API key
        //crowdclient.setApiKey()
    }

    public CrowdJob createJob(String template) throws JsonProcessingException, IOException
    {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonTemplate = mapper.readTree(template);
        CrowdJob job = new CrowdJob(jsonTemplate);
        return crowdclient.createNewJob(job);
    }

    /**
     *
     * @param documentsJCas
     * @return
     */
    public Vector<NamedEntityTask1Data> generateTask1Data(List<JCas>documentsJCas)
    {
        Vector<NamedEntityTask1Data> data = new Vector<NamedEntityTask1Data>();
        int i=0;
        for (JCas documentJCas : documentsJCas)
        {
            String text = "";
            for (Sentence sentence : select(documentJCas, Sentence.class)) {
                List<Token> tokens = new ArrayList<Token>();
                for (Token token : selectCovered(Token.class, sentence)) {
                    //TODO: check that token.toString() does what you think it does
                    text += "<span id=\"token=" + String.valueOf(i) + "\">" + token.getCoveredText() + " </span>";
                    i++;
                }
                //System.out.println(text);
                data.add(new NamedEntityTask1Data(text));
            }
        }

        return data;
    }

    public void setAPIKey(String key)
    {
        crowdclient.setApiKey(key);
    }

    public String uploadNewNERTask1(String template, List<JCas>documentsJCas , List<JCas>goldJCas) throws JsonProcessingException, IOException, Exception
    {
        Log LOG = LogFactory.getLog(getClass());
        LOG.info("Creating new Job for Ner task 1.");
        CrowdJob job = createJob(template);
        LOG.info("Done, new job id is: "+job.getId()+". Now generating data for NER task 1");
        Vector<NamedEntityTask1Data> data = generateTask1Data(documentsJCas);
        LOG.info("Uploading data to job #" + job.getId());
        crowdclient.uploadData(job,data);
        LOG.info("Done, finished uploading data to #" + job.getId());
        return job.getId();
    }

}
