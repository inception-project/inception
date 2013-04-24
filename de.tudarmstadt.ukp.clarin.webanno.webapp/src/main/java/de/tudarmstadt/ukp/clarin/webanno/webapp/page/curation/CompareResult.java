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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.uima.cas.FeatureStructure;

public class CompareResult {
	// fs1, fs2
	private Map<FeatureStructure, FeatureStructure> diffs = new HashMap<FeatureStructure, FeatureStructure>();
	// fs1, fs2
	private Map<FeatureStructure, FeatureStructure> agreements = new HashMap<FeatureStructure, FeatureStructure>();
	/*
	private Set<FeatureStructure> fs1only = new HashSet<FeatureStructure>();
	private Set<FeatureStructure> fs2only = new HashSet<FeatureStructure>();
	*/
	public Map<FeatureStructure, FeatureStructure> getDiffs() {
		return diffs;
	}
	public void setDiffs(Map<FeatureStructure, FeatureStructure> diffs) {
		this.diffs = diffs;
	}
	public Map<FeatureStructure, FeatureStructure> getAgreements() {
		return agreements;
	}
	public void setAgreements(Map<FeatureStructure, FeatureStructure> agreements) {
		this.agreements = agreements;
	}
	/*
	public Set<FeatureStructure> getFs1only() {
		return fs1only;
	}
	public void setFs1only(Set<FeatureStructure> fs1only) {
		this.fs1only = fs1only;
	}
	public Set<FeatureStructure> getFs2only() {
		return fs2only;
	}
	public void setFs2only(Set<FeatureStructure> fs2only) {
		this.fs2only = fs2only;
	}
	*/
}