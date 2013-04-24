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
package de.tudarmstadt.ukp.clarin.webanno.webapp.page.curation;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

public class AnnotationOption implements Serializable {
	private List<AnnotationSelection> annotationSelections = new LinkedList<AnnotationSelection>();

	public List<AnnotationSelection> getAnnotationSelections() {
		return annotationSelections;
	}

	public void setAnnotationSelections(
			List<AnnotationSelection> annotationSelections) {
		this.annotationSelections = annotationSelections;
	}
	
	public String toString() {
		return annotationSelections.toString();
	}
	
}
