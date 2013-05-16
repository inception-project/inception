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
package de.tudarmstadt.ukp.clarin.webanno.webapp.page.curation.component.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class CurationSegment implements Serializable {

	private Integer begin;
	private Integer end;
	private String text;
	private SentenceState sentenceState;
	private Integer sentenceNumber;
	
	private Map<String, Integer> sentenceAddress = new HashMap<String, Integer>();
	
	public CurationSegment() {
		
	}

	public String getText() {
		return text;
	}


	public void setText(String text) {
		this.text = text;
	}


	public Integer getBegin() {
		return begin;
	}

	public void setBegin(Integer begin) {
		this.begin = begin;
	}

	public Integer getEnd() {
		return end;
	}

	public void setEnd(Integer end) {
		this.end = end;
	}

	public Map<String, Integer> getSentenceAddress() {
		return sentenceAddress;
	}

	public Boolean getHasDiff() {
		return sentenceState.hasDiff();
	}

	public SentenceState getSentenceState() {
		return sentenceState;
	}

	public void setSentenceState(SentenceState sentenceState) {
		this.sentenceState = sentenceState;
	}

	public Integer getSentenceNumber() {
		return sentenceNumber;
	}

	public void setSentenceNumber(Integer sentenceNumber) {
		this.sentenceNumber = sentenceNumber;
	}

	
}
