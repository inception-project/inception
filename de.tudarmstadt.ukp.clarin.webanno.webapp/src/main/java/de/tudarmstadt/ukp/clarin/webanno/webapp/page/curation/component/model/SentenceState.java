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

public enum SentenceState {
	AGREE(false, null), DISAGREE(true, "#FF0000"), RESOLVED(true, "#FFFF00"), CONFIRMED(true, "#00FF00");
	
	private boolean hasDiff;
	private String colorCode;
	
	private SentenceState(boolean aHasDiff, String aColorCode) {
		hasDiff = aHasDiff;
		colorCode = aColorCode;
	}
	
	public boolean hasDiff() {
		return hasDiff;
	}
	
	public String getColorCode() {
		return colorCode;
	}
}
