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
package de.tudarmstadt.ukp.clarin.webanno.brat.curation.component.model;

/**
 * State of an annotated span (or arc). Contains color code for visualization
 *
 * @author straninger
 */
public enum AnnotationState
{
	/**
	 * All annotators and the curated document have the same annotation.
	 */
	AGREE("#bbbbbb"),
	/**
	 * color for the arc annotation that is in agreement
	 */
	AGREE_ARC("#000000"),
	/**
	 * Annotators have annotated differently. Curated document not yet has any annotations.
	 */
	DISAGREE("#7fa2ff"),
	/**
	 * Annotators have annotated differently. Annotation for current document and curation
	 * document are equal.
	 */
	USE("#7fffa2"),
	/**
	 * Annotators have annotated differently. Annotation for current document and curation
	 * document are not equal.
	 */
	DO_NOT_USE("#ff7fa2"),
	/**
	 * Error state. Annotation has been added to the visualization, but has not been identified
	 * by the CasDiff.
	 */
	NOT_SUPPORTED("#111111");

	private String colorCode;

	private AnnotationState(String aColorCode)
	{
		colorCode = aColorCode;
	}

	public String getColorCode()
	{
		return colorCode;
	}
}
