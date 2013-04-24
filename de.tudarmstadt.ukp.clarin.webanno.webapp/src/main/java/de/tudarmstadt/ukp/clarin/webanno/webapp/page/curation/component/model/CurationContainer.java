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
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.User;

public class CurationContainer implements Serializable {

	private Map<Integer, CurationSegment> curationSegmentByBegin = new HashMap<Integer, CurationSegment>();
	
	private SourceDocument sourceDocument;
	
	public List<CurationSegment> getCurationSegments() {
		LinkedList<Integer> segmentsBegin = new LinkedList<Integer>(curationSegmentByBegin.keySet());
		Collections.sort(segmentsBegin);
		List<CurationSegment> curationSegments = new LinkedList<CurationSegment>();
		for (Integer begin : segmentsBegin) {
			curationSegments.add(curationSegmentByBegin.get(begin));
		}
		return curationSegments;
	}
	
	public Map<Integer, CurationSegment> getCurationSegmentByBegin() {
		return curationSegmentByBegin;
	}

	public void setCurationSegmentByBegin(
			Map<Integer, CurationSegment> curationSegmentByBegin) {
		this.curationSegmentByBegin = curationSegmentByBegin;
	}
	
	public SourceDocument getSourceDocument() {
		return sourceDocument;
	}

	public void setSourceDocument(SourceDocument aSourceDocument) {
		sourceDocument = aSourceDocument;
	}

	public String toString() {
		return "curationSegmentByBegin"+curationSegmentByBegin.toString();
		
	}

	
}
