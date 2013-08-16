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
package de.tudarmstadt.ukp.clarin.webanno.webapp.page.curation;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

/**
 * This class comprises instances of {@link AnnotationSelection}, each representing
 * a possible choice of disagreeing annotation sets.
 *
 * @author Andreas Straninger
 */
public class AnnotationOption implements Serializable {
	/**
	 *
	 */
	private static final long serialVersionUID = -688656645133996937L;
	private List<AnnotationSelection> annotationSelections = new LinkedList<AnnotationSelection>();

	public List<AnnotationSelection> getAnnotationSelections() {
		return annotationSelections;
	}

	public void setAnnotationSelections(
			List<AnnotationSelection> annotationSelections) {
		this.annotationSelections = annotationSelections;
	}

	@Override
    public String toString() {
		return annotationSelections.toString();
	}

}
