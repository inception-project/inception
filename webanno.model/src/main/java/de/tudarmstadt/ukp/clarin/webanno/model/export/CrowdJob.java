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
package de.tudarmstadt.ukp.clarin.webanno.model.export;


import java.util.Set;


import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

/**
 * All required contents of a {@link de.tudarmstadt.ukp.clarin.webanno.model.MiraTemplate}  to be exported.
 *
 * @author Seid Muhie Yimam
 *
 */

@JsonIgnoreProperties(ignoreUnknown = true)
public class CrowdJob
{

    @JsonProperty("name")
    private String name;


    @JsonProperty("documents")
    private Set<SourceDocument> documents;

    @JsonProperty("gold_documents")
    private Set<SourceDocument> goldDocuments;

    @JsonProperty("apiKey")
    private String apiKey;

    @JsonProperty("link")
    private String link;

    @JsonProperty("status")
    private String status;

    @JsonProperty("task1Id")
    private String task1Id;

    @JsonProperty("task2Id")
    private String task2Id;

    @JsonProperty("use_ents")
    int useSents =-1;
    
    @JsonProperty("use_gold_ents")
    int useGoldSents =-1;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Set<SourceDocument> getDocuments() {
		return documents;
	}

	public void setDocuments(Set<SourceDocument> documents) {
		this.documents = documents;
	}

	public Set<SourceDocument> getGoldDocuments() {
		return goldDocuments;
	}

	public void setGoldDocuments(Set<SourceDocument> goldDocuments) {
		this.goldDocuments = goldDocuments;
	}

	public String getApiKey() {
		return apiKey;
	}

	public void setApiKey(String apiKey) {
		this.apiKey = apiKey;
	}

	public String getLink() {
		return link;
	}

	public void setLink(String link) {
		this.link = link;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getTask1Id() {
		return task1Id;
	}

	public void setTask1Id(String task1Id) {
		this.task1Id = task1Id;
	}

	public String getTask2Id() {
		return task2Id;
	}

	public void setTask2Id(String task2Id) {
		this.task2Id = task2Id;
	}

	public int getUseSents() {
		return useSents;
	}

	public void setUseSents(int useSents) {
		this.useSents = useSents;
	}

	public int getUseGoldSents() {
		return useGoldSents;
	}

	public void setUseGoldSents(int useGoldSents) {
		this.useGoldSents = useGoldSents;
	}
    
    
}
