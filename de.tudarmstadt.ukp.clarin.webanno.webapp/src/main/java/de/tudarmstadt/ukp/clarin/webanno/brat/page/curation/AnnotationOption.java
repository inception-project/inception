package de.tudarmstadt.ukp.clarin.webanno.brat.page.curation;

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
