/*
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
 */
package de.tudarmstadt.ukp.clarin.webanno.automation.service;


import static de.tudarmstadt.ukp.clarin.webanno.api.ProjectService.ANNOTATION;
import static de.tudarmstadt.ukp.clarin.webanno.api.ProjectService.PROJECT;
import static de.tudarmstadt.ukp.clarin.webanno.api.ProjectService.SOURCE;
import static de.tudarmstadt.ukp.clarin.webanno.support.WebAnnoConst.MIRA;
import static de.tudarmstadt.ukp.clarin.webanno.support.WebAnnoConst.MIRA_TEMPLATE;
import static de.tudarmstadt.ukp.clarin.webanno.support.WebAnnoConst.TRAIN;
import static org.apache.commons.io.IOUtils.copyLarge;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipFile;

import javax.annotation.Resource;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.uima.UIMAException;
import org.apache.uima.cas.CASException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.util.CasCreationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import de.tudarmstadt.ukp.clarin.webanno.api.CasStorageService;
import de.tudarmstadt.ukp.clarin.webanno.api.ImportExportService;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectLifecycleAware;
import de.tudarmstadt.ukp.clarin.webanno.automation.model.AutomationStatus;
import de.tudarmstadt.ukp.clarin.webanno.automation.model.MiraTemplate;
import de.tudarmstadt.ukp.clarin.webanno.diag.CasDoctor;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.TrainingDocument;
import de.tudarmstadt.ukp.clarin.webanno.support.WebAnnoConst;
import de.tudarmstadt.ukp.clarin.webanno.support.logging.Logging;

@Component(AutomationService.SERVICE_NAME)
public class MiraAutomationServiceImpl
    implements AutomationService, ProjectLifecycleAware
{
 

    private final Logger log = LoggerFactory.getLogger(getClass());
    
    @Resource(name = "casStorageService")
    private CasStorageService casStorageService;
    
    @Value(value = "${repository.path}")
    private File dir;

    @Resource(name = "casDoctor")
    private CasDoctor casDoctor;
    
    @Resource(name = "importExportService")
    private ImportExportService importExportService;
    
    @PersistenceContext
    private EntityManager entityManager;
    
    @Override
    public List<String> listTemplates(Project aProject)
    {
        // list all MIRA template files
        File[] files = new File(dir.getAbsolutePath() + PROJECT + aProject.getId() + MIRA
                + MIRA_TEMPLATE).listFiles();

        // Name of the MIRA template files
        List<String> templateFiles = new ArrayList<String>();
        if (files != null) {
            for (File file : files) {
                templateFiles.add(file.getName());
            }
        }

        return templateFiles;
    }

    @Override
    @Transactional
    public List<MiraTemplate> listMiraTemplates(Project aProject)
    {
        List<MiraTemplate> allTenplates = entityManager.createQuery(
                "FROM MiraTemplate ORDER BY trainFeature ASC ", MiraTemplate.class).getResultList();
        List<MiraTemplate> templatesInThisProject = new ArrayList<MiraTemplate>();
        for (MiraTemplate miraTemplate : allTenplates) {
            if (miraTemplate.getTrainFeature() != null
                    && miraTemplate.getTrainFeature().getProject().getId() == aProject.getId()) {
                templatesInThisProject.add(miraTemplate);
            }
        }
        return templatesInThisProject;
    }

    @Override
    public void removeTemplate(Project aProject, String aFileName, String aUsername)
        throws IOException
    {
        FileUtils.forceDelete(new File(dir.getAbsolutePath() + PROJECT + aProject.getId() + MIRA
                + MIRA_TEMPLATE + aFileName));
        
        Logging.setMDC(aProject.getId(), aUsername);
        log.info("Removed template file [{}] from project [{}] ({})", aFileName, aProject.getName(),
                aProject.getId());
        Logging.clearMDC();
    }

    @Override
    public void createTemplate(Project aProject, File aContent, String aFileName, String aUsername)
        throws IOException
    {
        String templatePath = dir.getAbsolutePath() + PROJECT + aProject.getId() + MIRA
                + MIRA_TEMPLATE;
        FileUtils.forceMkdir(new File(templatePath));
        copyLarge(new FileInputStream(aContent), new FileOutputStream(new File(templatePath
                + aFileName)));

        Logging.setMDC(aProject.getId(), aUsername);
        log.info("Removed template file [{}] from project [{}] ({})", aFileName, aProject.getName(),
                aProject.getId());
        Logging.clearMDC();
    }

    @Override
    @Transactional
    public void removeMiraTemplate(MiraTemplate aTemplate)
    {
        try {
            removeAutomationStatus(getAutomationStatus(aTemplate));
        }
        catch (NoResultException e) {
            // do nothing - automation was not started and no status created for this template
        }
        entityManager.remove(aTemplate);
    }
    
    @Override
    @Transactional
    public void createAutomationStatus(AutomationStatus aStatus)
    {
        entityManager.persist(aStatus);
    }

    @Override
    public boolean existsAutomationStatus(MiraTemplate aTemplate)
    {
        try {
            entityManager
                    .createQuery("FROM AutomationStatus WHERE template =:template",
                            AutomationStatus.class).setParameter("template", aTemplate)
                    .getSingleResult();
            return true;
        }
        catch (NoResultException ex) {
            return false;
        }
    }

    @Override
    @Transactional
    public AutomationStatus getAutomationStatus(MiraTemplate aTemplate)
    {
        return entityManager
                .createQuery("FROM AutomationStatus WHERE template =:template",
                        AutomationStatus.class).setParameter("template", aTemplate)
                .getSingleResult();
    }

    @Override
    @Transactional
    public void removeAutomationStatus(AutomationStatus aStstus)
    {

        entityManager.remove(aStstus);
    }

    @Override
    public File getMiraModel(AnnotationFeature aFeature, boolean aOtherLayer,
            TrainingDocument aDocument)
    {
        if (aDocument != null) {
            return new File(getMiraDir(aFeature), aDocument.getId() + "- "
                    + aDocument.getProject().getId() + "-model");
        }
        else if (aOtherLayer) {
            return new File(getMiraDir(aFeature), aFeature.getId() + "-model");
        }
        else {
            return new File(getMiraDir(aFeature), aFeature.getLayer().getId() + "-"
                    + aFeature.getId() + "-model");
        }
    }

    @Override
    public File getMiraDir(AnnotationFeature aFeature)
    {
        return new File(dir, PROJECT + aFeature.getProject().getId() + MIRA);
    }

    @Override
    @Transactional
    public void createTemplate(MiraTemplate aTemplate)
    {
        if (aTemplate.getId() == 0) {
            entityManager.persist(aTemplate);
        }
        else {
            entityManager.merge(aTemplate);
        }
    }

    @Override
    @Transactional(noRollbackFor = NoResultException.class)
    public MiraTemplate getMiraTemplate(AnnotationFeature aFeature)
    {
        return entityManager
                .createQuery("FROM MiraTemplate WHERE trainFeature =:trainFeature",
                        MiraTemplate.class).setParameter("trainFeature", aFeature)
                .getSingleResult();
    }

    @Override
    public boolean existsMiraTemplate(AnnotationFeature aFeature)
    {
        try {
            entityManager
                    .createQuery("FROM MiraTemplate WHERE trainFeature =:trainFeature",
                            MiraTemplate.class).setParameter("trainFeature", aFeature)
                    .getSingleResult();
            return true;
        }
        catch (NoResultException ex) {
            return false;
        }
    }

    @Override
    @Transactional(noRollbackFor = NoResultException.class)
    public List<TrainingDocument> listTabSepDocuments(Project aProject)
    {
        List<TrainingDocument> trainingDocuments = entityManager
                .createQuery("FROM TrainingDocument where project =:project", TrainingDocument.class)
                .setParameter("project", aProject).getResultList();
        List<TrainingDocument> tabSepDocuments = new ArrayList<TrainingDocument>();
        for (TrainingDocument trainingDocument : trainingDocuments) {
            if (trainingDocument.getFormat().equals(WebAnnoConst.TAB_SEP)) {
                tabSepDocuments.add(trainingDocument);
            }
        }
        return tabSepDocuments;
    }
    
    @Override
    @Transactional
    public boolean existsTrainingDocument(Project aProject, String aFileName)
    {
        try {
            entityManager
                    .createQuery(
                            "FROM TrainingDocument WHERE project = :project AND " + "name =:name ",
                            TrainingDocument.class).setParameter("project", aProject)
                    .setParameter("name", aFileName).getSingleResult();
            return true;
        }
        catch (NoResultException ex) {
            return false;
        }
    }
    @Override
    @Transactional(noRollbackFor = NoResultException.class)
   public File getDocumentFolder(TrainingDocument trainingDocument) throws IOException{
    	File trainingDocFolder = new File(dir, PROJECT + trainingDocument.getProject().getId() + TRAIN
                + trainingDocument.getId()+SOURCE);
        FileUtils.forceMkdir(trainingDocFolder);
        return trainingDocFolder;
    }
    @Override
    @Transactional(noRollbackFor = NoResultException.class)
    public List<TrainingDocument> listTrainingDocuments(Project aProject)
    {
    	// both TAB_SEP and WebAnno training documents
        List<TrainingDocument> trainingDocuments = entityManager
                .createQuery("FROM TrainingDocument where project =:project", TrainingDocument.class)
                .setParameter("project", aProject).getResultList();
   /*     List<TrainingDocument> webAnnoTraiingDocuments = new ArrayList<TrainingDocument>();
        for (TrainingDocument trainingDocument : trainingDocuments) {
            if (trainingDocument.getFormat().equals(WebAnnoConst.TAB_SEP)) {
            	webAnnoTraiingDocuments.add(trainingDocument);
            }
        }*/
        return trainingDocuments;
    }
    
    @Override
    @Transactional(noRollbackFor = NoResultException.class)
    public TrainingDocument getTrainingDocument(Project aProject, String aDocumentName)
    {
        return entityManager
                .createQuery("FROM TrainingDocument WHERE name = :name AND project =:project",
                		TrainingDocument.class).setParameter("name", aDocumentName)
                .setParameter("project", aProject).getSingleResult();
    }
    
    @Override
    @Transactional
    public void removeTrainingDocument(TrainingDocument aDocument)
        throws IOException
    {       
        entityManager.remove(aDocument);

        String path = dir.getAbsolutePath() + PROJECT + aDocument.getProject().getId() + TRAIN
                + aDocument.getId();
        // remove from file both source and related annotation file
        if (new File(path).exists()) {
            FileUtils.forceDelete(new File(path));
        }

        try (MDC.MDCCloseable closable = MDC.putCloseable(Logging.KEY_PROJECT_ID,
                String.valueOf(aDocument.getProject().getId()))) {
            Project project = aDocument.getProject();
            log.info("Removed source document [{}]({}) from project [{}]({})", aDocument.getName(),
                    aDocument.getId(), project.getName(), project.getId());
        }
    }

    @Override
    @Transactional
    public JCas readTrainingAnnotationCas(TrainingDocument aTrainingAnnotationDocument)
        throws IOException
    {
        // If there is no CAS yet for the annotation document, create one.
        JCas jcas = null;
        if (!existsCas(aTrainingAnnotationDocument)) {
            // Convert the source file into an annotation CAS
            try {
                if (!existsInitialCas(aTrainingAnnotationDocument)) {
                    jcas = createInitialCas(aTrainingAnnotationDocument);
                }

                // Ok, so at this point, we either have the lazily converted CAS already loaded
                // or we know that we can load the existing initial CAS.
                if (jcas == null) {
                    jcas = readInitialCas(aTrainingAnnotationDocument);
                }
            }
            catch (Exception e) {
                log.error("The reader for format [" + aTrainingAnnotationDocument.getFormat()
                        + "] is unable to digest data", e);
                throw new IOException("The reader for format [" + aTrainingAnnotationDocument.getFormat()
                        + "] is unable to digest data" + e.getMessage());
            }
            casStorageService.writeCas(aTrainingAnnotationDocument, jcas);
        }
        else {
            // Read existing CAS
            // We intentionally do not upgrade the CAS here because in general the IDs
            // must remain stable. If an upgrade is required the caller should do it
            jcas = casStorageService.readCas(aTrainingAnnotationDocument);
        }

        return jcas;
    }
    
    
    @Override
    @Transactional
    public void createTrainingDocument(TrainingDocument aDocument)
        throws IOException
    {
        if (aDocument.getId() == 0) {
            entityManager.persist(aDocument);
        }
        else {
            entityManager.merge(aDocument);
        }
    }
    
    @Override
    public boolean existsInitialCas(TrainingDocument aDocument)
        throws IOException
    {
        return existsCas(aDocument);
    }
    
    @Override
    @Transactional
    public boolean existsCas(TrainingDocument aTrainingDocument)
        throws IOException
    {
        return new File(casStorageService.getAutomationFolder(aTrainingDocument), aTrainingDocument.getName() + ".ser")
                .exists();
    }
    

	@Override
	public JCas createInitialCas(TrainingDocument aDocument) 
			throws UIMAException, IOException, ClassNotFoundException {
        JCas jcas = importExportService.importCasFromFile(getTrainingDocumentFile(aDocument),
                aDocument.getProject(), aDocument.getFormat());
        casStorageService.analyzeAndRepair(aDocument, jcas.getCas());
        CasPersistenceUtils.writeSerializedCas(jcas,
                getCasFile(aDocument));
        
        return jcas;
	}
	
	@Override
	public File getTrainingDocumentFile(TrainingDocument aDocument) {
		File documentUri = new File(
				dir.getAbsolutePath() + PROJECT + aDocument.getProject().getId() + 
				TRAIN + aDocument.getId() + SOURCE);
		return new File(documentUri, aDocument.getName());
	}

	@Override
	public JCas readInitialCas(TrainingDocument aDocument)
			throws CASException, ResourceInitializationException, IOException {
				JCas jcas = CasCreationUtils.createCas((TypeSystemDescription) null, null, null).getJCas();
	        
				CasPersistenceUtils.readSerializedCas(jcas, getCasFile(aDocument));
	        
				casStorageService.analyzeAndRepair(aDocument, jcas.getCas());
	        
	        return jcas;
	}

	@Override
	public JCas createOrReadInitialCas(TrainingDocument aDocument)
			throws IOException, UIMAException, ClassNotFoundException {
		   if (existsInitialCas(aDocument)) {
	            return readInitialCas(aDocument);
	        }
	        else {
	            return createInitialCas(aDocument);
	        }
	}

	@Override
    public File getCasFile(TrainingDocument aDocument)
    {
        File documentUri = new File(dir.getAbsolutePath() + PROJECT
                + aDocument.getProject().getId() + TRAIN + aDocument.getId() + ANNOTATION);
        return new File(documentUri, FilenameUtils.removeExtension(aDocument.getName()) + ".ser");
    }

	@Override
	public void afterProjectCreate(Project aProject) throws Exception {
		 // Nothing at the moment
		
	}

	@Override
	public void beforeProjectRemove(Project aProject) throws Exception {
		  for (TrainingDocument document : listTrainingDocuments(aProject)) {
	            removeTrainingDocument(document);
	        }
		  for(MiraTemplate template: listMiraTemplates(aProject)){
			  removeMiraTemplate(template);
		  }
		
	}

	@Override
	public void onProjectImport(ZipFile zip, de.tudarmstadt.ukp.clarin.webanno.export.model.Project aExportedProject,
			Project aProject) throws Exception {
		 // Nothing at the moment
		
	}


        
}
