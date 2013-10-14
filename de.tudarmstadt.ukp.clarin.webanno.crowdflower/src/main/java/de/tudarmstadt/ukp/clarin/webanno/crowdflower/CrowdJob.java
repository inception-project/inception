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
/*******************************************************************************
 * Copyright 2012
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.ObjectWriter;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
* Class that holds a template and job's data for a Crowdflower.com job
* @author Benjamin Milde
*/
public class CrowdJob
{
    private String id = "-1";

    private JsonNode template;
    private JsonNode data;

    // These need special care, because the translation from JSON to HTTP post values is different than for other arrays
    public static final String includedCountriesKey = "included_countries";
    public static final String excludedCountriesKey = "excluded_countries";
    public static final String countryCodeKey = "code";

    public static final String jobKey = "job";
    public static final String idKey = "id";

    // We'll only care about these variables, but store all of them in template, so that it it is easier to change something later if needed
    public static final String[] argumentFilter = {
        "cml", //main description for the Job, in CML (similar to HTML)
        "instructions", //job instructions for workers
        "support_email", //usually selfservice_notifications@crowdflower.com ?
        "max_judgments_per_ip", // different from max_judgments_per_worker, because a worker may use different IPs
        "js", //problem with JS -> ? http://stackoverflow.com/questions/15297030/crowdflower-api-create-a-job-including-javascript-code
        "variable_judgments_mode", // ?
        "language", // en or de etc.
        "design_verified", //?
        "css", //css file for cml
        "auto_order",//?
        "execution_mode", //"builder" is default
        "worker_ui_remix", //?
        "problem", //description of the problem for the workers
        "max_judgments_per_worker", //max number of judgments a worker is allowed to do
        "title", //job title, as displayed to the workers
        "options", //array with misc options
        "included_countries", //array with names and ISO codes for allowed countries
        "excluded_countries", //array, exclude these countries
        "confidence_fields", //need to find out what those are for
        "fields" //need to find out what those are for
    };

    private MultiValueMap<String, String> argumentMap;

    private Vector<String> includedCountries;
    private Vector<String> excludedCountries;

    private Vector<String> channels;

    public static final Set<String> argumentFilterSet = new HashSet<String>(Arrays.asList(argumentFilter));

    /**
     * Create a new CrowdJob, which is a Java representation of a Job, from a JSON template.
     * CrowdClient must be used to actually create the job on the server.
     * @param template
     */
    public CrowdJob(JsonNode template)
    {
        this.template = template;
        createArgumentMaps();
    }

    /**
     * Create job arguments that are used by CrowdClient to build a POST request for a new job on Crowdflower
     * @return MultiValueMap consisting of job[var]=value pairs
     */
    void createArgumentMaps()
    {
        argumentMap = new LinkedMultiValueMap<String, String>();
        includedCountries = new Vector<String>();
        excludedCountries = new Vector<String>();

        Iterator<Map.Entry<String, JsonNode>> jsonRootIt = template.getFields();

        for (Map.Entry<String, JsonNode> elt ; jsonRootIt.hasNext(); )
        {
            elt = jsonRootIt.next();

            JsonNode currentNode = elt.getValue();
            String currentKey = elt.getKey();

            if(currentNode.isContainerNode())
            {
                //special processing for these arrays:
                if(currentKey.equals(includedCountriesKey) || currentKey.equals(excludedCountriesKey))
                {
                    Iterator<JsonNode> jsonSubNodeIt = currentNode.getElements();
                    for (JsonNode subElt ; jsonSubNodeIt.hasNext(); )
                    {
                        subElt = jsonSubNodeIt.next();
                        (currentKey.equals(includedCountriesKey) ? includedCountries : excludedCountries)
                                                        .addElement(subElt.path(countryCodeKey).asText());
                    }

                }
            }else if(!currentNode.isNull() && argumentFilterSet.contains(currentKey))
            {
                System.out.println(jobKey + "[" + currentKey + "]=" + currentNode.toString());
                argumentMap.add(jobKey + "[" + currentKey + "]",currentNode.asText());
            }
            if(currentKey == idKey) {
                this.id = currentNode.asText();
            }
        }
    }

    /**
     * Serialize the template to a JSON string
     * @return
     * @throws JsonGenerationException
     * @throws JsonMappingException
     * @throws IOException
     */

    String getTemplateAsString() throws JsonGenerationException, JsonMappingException, IOException
    {
        ObjectMapper mapper = new ObjectMapper();
        ObjectWriter writer = mapper.writer().withDefaultPrettyPrinter();
        return writer.writeValueAsString(template);
    }

    public MultiValueMap<String, String> getArgumentMap()
    {
        return argumentMap;
    }

    public void setArgumentMap(MultiValueMap<String, String> agrumentMap)
    {
        this.argumentMap = agrumentMap;
    }

    public String getId()
    {
        return id;
    }

    public void setId(String id)
    {
        this.id = id;
    }

    public JsonNode getTemplate()
    {
        return template;
    }

    public void setTemplate(JsonNode template)
    {
        this.template = template;
    }

    public JsonNode getData()
    {
        return data;
    }

    public void setData(JsonNode data)
    {
        this.data = data;
    }

    public Vector<String> getIncludedCountries()
    {
        return includedCountries;
    }

    public Vector<String> getExcludedCountries()
    {
        return excludedCountries;
    }

    public void setIncludedCountries(Vector<String> includedCountries)
    {
        this.includedCountries = includedCountries;
    }

    public void setExcludedCountries(Vector<String> excludedCountries)
    {
        this.excludedCountries = excludedCountries;
    }

    public MultiValueMap<String, String> getIncludedCountriesMap()
    {
        MultiValueMap<String, String> map = new LinkedMultiValueMap<String, String>();
        for(String country :  getIncludedCountries())
        {
            map.add(jobKey+"["+includedCountriesKey+"][]", country);
        }
        return map;
    }

    public MultiValueMap<String, String> getExcludedCountriesMap()
    {
        MultiValueMap<String, String> map = new LinkedMultiValueMap<String, String>();
        for(String country :  getExcludedCountries())
        {
            map.add(jobKey+"["+excludedCountriesKey+"][]", country);
        }
        return map;
    }

    public Vector<String> getChannels()
    {
        return channels;
    }

    public void setChannels(Vector<String> channels)
    {
        this.channels = channels;
    }
}
