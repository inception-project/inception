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

import java.util.Vector;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJacksonHttpMessageConverter;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

/**
* Abstracts away the details of communicating with crowdflower.com's API
* @author Benjamin Milde
*/

public class CrowdClient
{
    // Crowdflower API documentation: https://crowdflower.com/docs/api
    // for creating a new Job, data needs to be posted to that URL:
    static final String newJobURL = "https://api.crowdflower.com/v1/jobs.json?key={apiKey}";
    // a GET to this address retrieves a JSON-template for a Job (not documented)
    static final String baseJobURL = "https://api.crowdflower.com/v1/jobs/{jobid}.json?key={apiKey}";
    static final String uploadDataWithJobURL = "https://api.crowdflower.com/v1/jobs/{jobid}/upload.json?key={apiKey}";
    static final String uploadDataURL = "https://api.crowdflower.com/v1/jobs/upload.json?key={apiKey}";
    static final String orderJobURL = "https://api.crowdflower.com/v1/jobs/{jobid}/orders?key={apiKey}";
    static final String judgmentsURL = "https://api.crowdflower.com/v1/jobs/{jobid}/judgments.json?key={apiKey}";
    static final String channelsURL = "https://api.crowdflower.com/v1/jobs/{jobid}/channels?key={apiKey}";

    static final String channelKey = "channels";
    static final String debitKey = "debit[units_count]";
    static final String jobPaymentKey = "job[payment_cents]";
    private String apiKey = "";

    /**
     * This sets the API key to be used with Crowdflower. This has to be set to a valid key prior to any other requests to the API.
     * @param apiKey
     */
    public void setApiKey(String apiKey)
    {
        this.apiKey = apiKey;
    }

    CrowdClient()
    {

    }

    /**
     *
     * @param job
     * @return
     * @throws HttpServerErrorException
     */
    CrowdJob createNewJob(CrowdJob job) throws HttpServerErrorException
    {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.getMessageConverters().add(new MappingJacksonHttpMessageConverter());
        restTemplate.getMessageConverters().add(new FormHttpMessageConverter());

        JsonNode result = restTemplate.postForObject(newJobURL, job.getArgumentMap(), JsonNode.class, apiKey);

        return new CrowdJob(result);
    }

    /**
     *
     * @param jobid
     * @return
     * @throws HttpServerErrorException
     */
    CrowdJob retrieveJob(String jobid) throws HttpServerErrorException
    {
        RestTemplate restTemplate = new RestTemplate();
        // restTemplate.getMessageConverters().add(new StringHttpMessageConverter());
        restTemplate.getMessageConverters().add(new MappingJacksonHttpMessageConverter());
        JsonNode result = restTemplate.getForObject(baseJobURL, JsonNode.class, jobid, apiKey);

        return new CrowdJob(result);
    }

    /**
     * Update the list of allowed countries for this job to Crowdflower (the list is set in the job)
     * @param job
     * @throws HttpServerErrorException
     */
    void updateAllowedCountries(CrowdJob job) throws HttpServerErrorException
    {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.getMessageConverters().add(new FormHttpMessageConverter());

        if(job.getExcludedCountries().size() > 0)
        {
            restTemplate.put(baseJobURL, job.getExcludedCountriesMap(), job.getId(), apiKey);
        }

        if(job.getIncludedCountries().size() > 0)
        {
            restTemplate.put(baseJobURL, job.getIncludedCountriesMap(), job.getId(), apiKey);
        }
       // restTemplate.put(url, request, urlVariables)
    }

    /**
     * Upload the data vector as JSON to the specified Crowdflower job
     * @param job
     * @param data
     * @throws Exception
     */
    @SuppressWarnings("rawtypes")
    void uploadData(CrowdJob job, Vector data) throws Exception
    {
        //Crowdflower wants a Multi-line JSON, with each line having a new JSON object
        //Thus we have to map each (raw) object in data individually to a JSON string

        ObjectMapper mapper = new ObjectMapper();
        String jsonObjectCollection = "";

        for(Object obj : data)
        {
            JsonNode jsonData = mapper.convertValue(obj, JsonNode.class);
            jsonObjectCollection += jsonData.toString() + "\n";
        }

        RestTemplate restTemplate = new RestTemplate();
        restTemplate.getMessageConverters().add(new StringHttpMessageConverter());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> request= new HttpEntity<String>(jsonObjectCollection, headers);

        String result = "";

        if(job == null) {
            System.out.println("Upload new data and create new job: " + jsonObjectCollection);
            result = restTemplate.postForObject(uploadDataURL, request, String.class, apiKey);
        }
        else {
            System.out.println("Uploading new data to job: "+ job.getId() + ":" + jsonObjectCollection);
            result = restTemplate.postForObject(uploadDataWithJobURL, request, String.class, job.getId(), apiKey);
        }
        System.out.println("Upload response:" + result);

        //set gold? this is what i would like to do...
        //updateVariable(job, "https://api.crowdflower.com/v1/jobs/{jobid}/gold?key={apiKey}", "set_standard_gold", "TRUE");
    }

    /**
     * sets a single key in Crowdflowers job with a given value
     * @param job
     * @param key
     * @param value
     */
    void updateVariable(CrowdJob job, String Url, String key, String value)
    {
        MultiValueMap<String, String> argumentMap = new LinkedMultiValueMap<String, String>();
        argumentMap.add(key, value);

        RestTemplate restTemplate = new RestTemplate();
        restTemplate.getMessageConverters().add(new FormHttpMessageConverter());

        restTemplate.put(Url, argumentMap, job.getId(), apiKey);
    }

    void updateVariable(CrowdJob job, String key, String value)
    {
        updateVariable(job, baseJobURL, key, value);
    }

    /**
     *
     * @param job
     * @param channels : a vector of channels, in which the job should be made available
     * @param units : number of units to order
     * @param payPerAssigment : pay in (dollar) cents for each assignments
     * @return JsonNode that Crowdflower returns
     */

    JsonNode orderJob(CrowdJob job, Vector<String> channels, int units, int payPerAssigment)
    {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.getMessageConverters().add(new MappingJacksonHttpMessageConverter());
        restTemplate.getMessageConverters().add(new FormHttpMessageConverter());

        MultiValueMap<String, String> argumentMap = new LinkedMultiValueMap<String, String>();

        argumentMap.add(debitKey, String.valueOf(units));
        for(String channel :  channels)
        {
            argumentMap.add(channelKey+"[]", channel);
        }

        System.out.println(argumentMap);

        updateVariable(job, jobPaymentKey, String.valueOf(payPerAssigment));

        JsonNode result = restTemplate.postForObject(orderJobURL, argumentMap, JsonNode.class, job.getId(), apiKey);

        return result;
    }

    /**
     * Retrieves raw judgments for a given job
     * @param job
     * @return
     */
    JsonNode retrieveJudgments(CrowdJob job)
    {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.getMessageConverters().add(new MappingJacksonHttpMessageConverter());

        JsonNode result = restTemplate.getForObject(judgmentsURL, JsonNode.class, job.getId(), apiKey);

        return result;
    }

    /**
     * This would help to get the short names of crowdflowers channels, but doesn't work currently
     * @param job
     */
    /*Vector<String>*/ void retrieveAvailableChannels(CrowdJob job)
    {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.getMessageConverters().add(new MappingJacksonHttpMessageConverter());

        JsonNode result = restTemplate.getForObject(channelsURL, JsonNode.class, job.getId(), apiKey);

        System.out.println(result);
    }
}
