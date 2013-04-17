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
package de.tudarmstadt.ukp.clarin.webanno.brat.page.curation;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.apache.uima.cas.FeatureStructure;

public class AnnotationSelection implements Serializable {
	Map<String, Integer> addressByUsername = new HashMap<String, Integer>();
	AnnotationOption annotationOption = null;

	public Map<String, Integer> getAddressByUsername() {
		return addressByUsername;
	}

	public void setAddressByUsername(Map<String, Integer> aAddressByUsername) {
		this.addressByUsername = aAddressByUsername;
	}

	public AnnotationOption getAnnotationOption() {
		return annotationOption;
	}

	public void setAnnotationOption(AnnotationOption annotationOption) {
		this.annotationOption = annotationOption;
	}
	
	public String toString() {
		return addressByUsername.toString();
	}
	
	public boolean equals(Object obj) {
		if(!(obj instanceof AnnotationSelection)) {
			return false;
		}
		AnnotationSelection as = (AnnotationSelection) obj;
		return addressByUsername.equals(as.getAddressByUsername());
	}
	
	public int hashCode() {
		return addressByUsername.hashCode();
	}
}
