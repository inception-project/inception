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

public class CurationUserSegment2 implements Serializable {
	private String documentResponse;
	private String collectionData = "{}";
	private String username = "";
	
	
	public String getDocumentResponse() {
		return documentResponse;
	}
	public void setDocumentResponse(String documentResponse) {
		this.documentResponse = documentResponse;
	}
	public String getCollectionData() {
		return collectionData;
	}
	public void setCollectionData(String collectionData) {
		this.collectionData = collectionData;
	}
	
	public boolean equals(CurationUserSegment2 segment) {
		return segment.getCollectionData().equals(collectionData)
				&& segment.getDocumentResponse().equals(documentResponse);
	}
	public String getUsername() {
		return username;
	}
	public void setUsername(String aUsername) {
		username = aUsername;
	}
	
	
}
