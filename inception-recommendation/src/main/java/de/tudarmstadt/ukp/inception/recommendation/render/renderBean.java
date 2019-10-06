package de.tudarmstadt.ukp.inception.recommendation.render;

import org.apache.uima.cas.CAS;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VDocument;
import de.tudarmstadt.ukp.inception.recommendation.api.LearningRecordService;
import de.tudarmstadt.ukp.inception.recommendation.api.RecommendationService;

public class renderBean {
	public static VDocument aVdoc;
	public static AnnotatorState aState;
	public static CAS aCas;
    public static AnnotationSchemaService aAnnotationService;
    public static RecommendationService aRecService;
    public static LearningRecordService aLearningRecordService;
    public static FeatureSupportRegistry aFsRegistry;
    public static DocumentService aDocumentService;
    
	public VDocument getaVdoc() {
		return aVdoc;
	}
	public void setaVdoc(VDocument aVdoc) {
		this.aVdoc = aVdoc;
	}
	
	public AnnotatorState getaState() {
		return aState;
	}
	public void setaState(AnnotatorState aState) {
		this.aState = aState;
	}
	
	public CAS getaCas() {
		return aCas;
	}
	public void setaCas(CAS aCas) {
		this.aCas = aCas;
	}
	
	public AnnotationSchemaService getaAnnotationService() {
		return aAnnotationService;
	}
	public void setaAnnotationService(AnnotationSchemaService aAnnotationService) {
		this.aAnnotationService = aAnnotationService;
	}
	
	public RecommendationService getaRecService() {
		return aRecService;
	}
	public void setaRecService(RecommendationService aRecService) {
		this.aRecService = aRecService;
	}
	
	public LearningRecordService getaLearningRecordService() {
		return aLearningRecordService;
	}
	public void setaLearningRecordService(LearningRecordService aLearningRecordService) {
		this.aLearningRecordService = aLearningRecordService;
	}
	public FeatureSupportRegistry getaFsRegistry() {
		return aFsRegistry;
	}
	public void setaFsRegistry(FeatureSupportRegistry aFsRegistry) {
		this.aFsRegistry = aFsRegistry;
	}
	public DocumentService getaDocumentService() {
		return aDocumentService;
	}
	public void setaDocumentService(DocumentService aDocumentService) {
		this.aDocumentService = aDocumentService;
	}

}
