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
