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

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.uima.UIMAException;
import org.apache.uima.cas.CASException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

import de.tudarmstadt.ukp.clarin.webanno.automation.model.AutomationStatus;
import de.tudarmstadt.ukp.clarin.webanno.automation.model.MiraTemplate;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.TrainingDocument;

public interface AutomationService
{
    String SERVICE_NAME = "automationService";
    
    /**
     * Return list of training documents that are in the TOKEN TAB FEAURE formats
     *
     * @param aProject
     *            the project.
     * @return the source documents.
     */
    List<TrainingDocument> listTabSepDocuments(Project aProject);
    
    boolean existsTrainingDocument(Project project, String fileName);
    
    /**
     * Get the directory of this {@link TrainingDocument} usually to read the content of the
     * document.
     *
     * @param trainingDocument
     *            the Training document.
     * @return the training document folder.
     * @throws IOException
     *             if an I/O error occurs.
     */
    File getDocumentFolder(TrainingDocument trainingDocument)
        throws IOException;
    
    /**
     * Return list of WebAnno supported training documents
     * @param aProject
     * @return
     */
    List<TrainingDocument> listTrainingDocuments(Project aProject);

    TrainingDocument getTrainingDocument(Project aProject, String aDocumentName);
    
    /**
     * Remove {@link TrainingDocument}
     * 
     * @throws IOException
     */
    void removeTrainingDocument(TrainingDocument aDocument)
        throws IOException;
    
    JCas readTrainingAnnotationCas(TrainingDocument trainingAnnotationDocument)
            throws IOException;
    
    void createTrainingDocument(TrainingDocument document) throws IOException;
    
    /**
     * List MIRA template files
     * 
     * @param project
     *            the project.
     * @return the templates.
     */
    List<String> listTemplates(Project project);

    /**
     * Remove an MIRA template
     * 
     * @param project
     *            the project.
     * @param fileName
     *            the filename.
     * @param username
     *            the username.
     * @throws IOException
     *             if an I/O error occurs.
     */
    void removeTemplate(Project project, String fileName, String username)
        throws IOException;

    /**
     * Create a MIRA template and save the configurations in a database
     * 
     * @param template
     *            the template.
     */
    void createTemplate(MiraTemplate template);

    void createTemplate(Project project, File content, String fileName, String username)
            throws IOException;

    /**
     * Get the MIRA template (and hence the template configuration) for a given layer
     * 
     * @param feature
     *            the feature.
     * @return the template.
     */
    MiraTemplate getMiraTemplate(AnnotationFeature feature);

    /**
     * Check if a MIRA template is already created for this layer
     * 
     * @param feature
     *            the feature.
     * @return if a template exists.
     */
    boolean existsMiraTemplate(AnnotationFeature feature);

    /**
     * List all the MIRA templates created, hence know which layer do have a training conf already!
     * 
     * @param project the project.
     * @return the templates.
     */
    List<MiraTemplate> listMiraTemplates(Project project);

    /**
     * Get the a model for a given automation layer or other layers used as feature for the
     * automation layer. model will be generated per layer
     * 
     * @param feature
     *            the feature.
     * @param otherLayer
     *            if this is a primary or secondary feature.
     * @param document
     *            the source document.
     * @return the model.
     */
    File getMiraModel(AnnotationFeature feature, boolean otherLayer, TrainingDocument document);

    /**
     * Get the MIRA director where models, templates and training data will be stored
     * 
     * @param feature
     *            the feature.
     * @return the directory.
     */
    File getMiraDir(AnnotationFeature feature);
    File getCasFile(TrainingDocument aDocument);
    void removeMiraTemplate(MiraTemplate template);

    void removeAutomationStatus(AutomationStatus status);

    void createAutomationStatus(AutomationStatus status);

    boolean existsAutomationStatus(MiraTemplate template);  
    AutomationStatus getAutomationStatus(MiraTemplate template);
    
    
    JCas createInitialCas(TrainingDocument aDocument)
        throws UIMAException, IOException, ClassNotFoundException;
    File getTrainingDocumentFile(TrainingDocument aDocument);
    JCas readInitialCas(TrainingDocument aDocument)
        throws CASException, ResourceInitializationException, IOException;
    
    JCas createOrReadInitialCas(TrainingDocument aDocument)
        throws IOException, UIMAException, ClassNotFoundException;
    boolean existsInitialCas(TrainingDocument aDocument)
            throws IOException;
    boolean existsCas(TrainingDocument aDocument) throws IOException;
    
    void uploadTrainingDocument(File aFile, TrainingDocument aDocument)
            throws IOException;
}
