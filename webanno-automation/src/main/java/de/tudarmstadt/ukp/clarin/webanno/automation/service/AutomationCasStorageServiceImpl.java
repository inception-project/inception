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

import static de.tudarmstadt.ukp.clarin.webanno.api.ProjectService.ANNOTATION_FOLDER;
import static de.tudarmstadt.ukp.clarin.webanno.api.ProjectService.PROJECT_FOLDER;
import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.TRAIN;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.uima.UIMAException;
import org.apache.uima.cas.CAS;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.util.CasCreationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.clarin.webanno.diag.CasDoctor;
import de.tudarmstadt.ukp.clarin.webanno.diag.CasDoctorException;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.TrainingDocument;
import de.tudarmstadt.ukp.clarin.webanno.support.logging.Logging;
import de.tudarmstadt.ukp.dkpro.core.api.metadata.type.DocumentMetaData;

@Component(AutomationCasStorageService.SERVICE_NAME)
public class AutomationCasStorageServiceImpl
    implements AutomationCasStorageService
{
    private final Logger log = LoggerFactory.getLogger(getClass());

    private final Object lock = new Object();

    @Value(value = "${repository.path}")
    private File dir;
    
    private @Autowired CasDoctor casDoctor;
    
    public AutomationCasStorageServiceImpl()
    {
        // Nothing to do
    }

    /**
     * Creates a CAS for the Automation documents
     *
     * @param aDocument
     *            the {@link TrainingDocument}
     * @param aJcas
     *            The annotated CAS object
     */
    @Override
    public void writeCas(TrainingDocument aDocument, JCas aJcas)
        throws IOException
    {
        File annotationFolder = getAutomationFolder(aDocument);
        File targetPath = getAutomationFolder(aDocument);
        writeCas(aDocument.getProject(), aDocument.getName(), aDocument.getId(), aJcas,
                annotationFolder, targetPath);
    }
    
    private void writeCas(Project aProject, String aDocumentName, long aDocumentId, JCas aJcas,
            File aAnnotationFolder, File aTargetPath)
        throws IOException
    {
        log.debug("Writing automation cas for document [{}]({})  in project [{}]({})",
                aDocumentName, aDocumentId, aProject.getName(), aProject.getId());

        try {
            casDoctor.analyze(aProject, aJcas.getCas());
        }
        catch (CasDoctorException e) {
            StringBuilder detailMsg = new StringBuilder();
            detailMsg.append("CAS Doctor found problems in train document [").append(aDocumentName)
                    .append("] (").append(aDocumentId).append(") in project[")
                    .append(aProject.getName()).append("] (").append(aProject.getId())
                    .append(")\n");
            e.getDetails().forEach(m -> 
                    detailMsg.append(String.format("- [%s] %s%n", m.level, m.message)));

            throw new DataRetrievalFailureException(detailMsg.toString());
        }
        catch (Exception e) {
            throw new DataRetrievalFailureException("Error analyzing CAS  in train document ["
                    + aDocumentName + "] (" + aDocumentId + ") in project [" + aProject.getName()
                    + "] (" + aProject.getId() + ")", e);
        }
        synchronized (lock) {
            FileUtils.forceMkdir(aAnnotationFolder);
            // Save CAS of the training document
            {
                DocumentMetaData md;
                try {
                    md = DocumentMetaData.get(aJcas);
                }
                catch (IllegalArgumentException e) {
                    md = DocumentMetaData.create(aJcas);
                }
                md.setDocumentId(aDocumentName);
                CasPersistenceUtils.writeSerializedCas(aJcas,
                        new File(aTargetPath, aDocumentName + ".ser"));

                try (MDC.MDCCloseable closable = MDC.putCloseable(Logging.KEY_PROJECT_ID,
                        String.valueOf(aProject.getId()))) {
                    log.info("Updated annotations on document [{}]({}) in project [{}]({})",
                            aDocumentName, aDocumentId, aProject.getName(), aProject.getId());
                }
            }
        }
    }
    
    @Override
    public JCas readCas(TrainingDocument aDocument)
        throws IOException
    {
        log.debug("Reading CAs for Automation document [{}] ({}) in project [{}] ({})",
                aDocument.getName(), aDocument.getId(), aDocument.getProject().getName(),
                aDocument.getProject().getId());

        // DebugUtils.smallStack();

        synchronized (lock) {
            File annotationFolder = getAutomationFolder(aDocument);

            String file = aDocument.getName() + ".ser";

            try {
                File serializedCasFile = new File(annotationFolder, file);
                if (!serializedCasFile.exists()) {
                    throw new FileNotFoundException("Annotation document of  Training document "
                            + "[" + aDocument.getName() + "] (" + aDocument.getId()
                            + ") not found in project[" + aDocument.getProject().getName() + "] ("
                            + aDocument.getProject().getId() + ")");
                }

                CAS cas = CasCreationUtils.createCas((TypeSystemDescription) null, null, null);
                CasPersistenceUtils.readSerializedCas(cas.getJCas(), serializedCasFile);

                analyzeAndRepair(aDocument, cas);

                return cas.getJCas();
            }
            catch (UIMAException e) {
                throw new DataRetrievalFailureException("Unable to parse annotation", e);
            }
        }
    }
    
    @Override
    public void analyzeAndRepair(TrainingDocument aDocument, CAS aCas)
    {
        analyzeAndRepair(aDocument.getProject(), aDocument.getName(), aDocument.getId(),
                "Automation", aCas);
    }

    private void analyzeAndRepair(Project aProject, String aDocumentName, long aDocumentId,
            String aUsername, CAS aCas)
    {
        // Check if repairs are active - if this is the case, we only need to run the repairs
        // because the repairs do an analysis as a pre- and post-condition. 
        if (casDoctor.isRepairsActive()) {
            try {
                casDoctor.repair(aProject, aCas);
            }
            catch (Exception e) {
                throw new DataRetrievalFailureException("Error repairing CAS of user ["
                        + aUsername + "] for document ["
                        + aDocumentName + "] (" + aDocumentId + ") in project["
                        + aProject.getName() + "] ("
                        + aProject.getId() + ")", e);
            }
        }
        // If the repairs are not active, then we run the analysis explicitly
        else {
            try {
                casDoctor.analyze(aProject, aCas);
            }
            catch (CasDoctorException e) {
                StringBuilder detailMsg = new StringBuilder();
                detailMsg.append("CAS Doctor found problems for user [").append(aUsername)
                    .append("] in document [")
                    .append(aDocumentName).append("] (").append(aDocumentId)
                    .append(") in project[")
                    .append(aProject.getName()).append("] (").append(aProject.getId()).append(")\n");
                e.getDetails().forEach(m -> detailMsg.append(
                        String.format("- [%s] %s%n", m.level, m.message)));
                
                throw new DataRetrievalFailureException(detailMsg.toString());
            }
            catch (Exception e) {
                throw new DataRetrievalFailureException("Error analyzing CAS of user ["
                        + aUsername + "] in document [" + aDocumentName + "] ("
                        + aDocumentId + ") in project["
                        + aProject.getName() + "] ("
                        + aProject.getId() + ")", e);
            }
        }
    }
    
    /**
     * Get the folder where the Automation document annotations are stored. Creates the folder if
     * necessary.
     *
     * @throws IOException
     *             if the folder cannot be created.
     */
    @Override
    public File getAutomationFolder(TrainingDocument aDocument)
        throws IOException
    {
        File annotationFolder = new File(dir,
                "/" + PROJECT_FOLDER + "/" + aDocument.getProject().getId() + TRAIN
                        + aDocument.getId() + "/" + ANNOTATION_FOLDER);
        FileUtils.forceMkdir(annotationFolder);
        return annotationFolder;
    }
    
    /**
     * Renames a file.
     *
     * @throws IOException
     *             if the file cannot be renamed.
     * @return the target file.
     */
    private static File renameFile(File aFrom, File aTo)
        throws IOException
    {
        if (!aFrom.renameTo(aTo)) {
            throw new IOException("Cannot renamed file [" + aFrom + "] to [" + aTo + "]");
        }

        // We are not sure if File is mutable. This makes sure we get a new file
        // in any case.
        return new File(aTo.getPath());
    }
}
