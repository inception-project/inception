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
package de.tudarmstadt.ukp.clarin.webanno.automation;

import static org.apache.commons.io.IOUtils.copyLarge;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;

import de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AutomationStatus;
import de.tudarmstadt.ukp.clarin.webanno.model.MiraTemplate;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;

public class MiraAutomationServiceImpl
    implements AutomationService
{
    private static final String PROJECT = "/project/";
    private static final String MIRA = "/mira/";
    private static final String MIRA_TEMPLATE = "/template/";

    @Value(value = "${webanno.repository}")
    private File dir;

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
    public void removeTemplate(Project aProject, String aFileName, String username)
        throws IOException
    {
        FileUtils.forceDelete(new File(dir.getAbsolutePath() + PROJECT + aProject.getId() + MIRA
                + MIRA_TEMPLATE + aFileName));
        createLog(aProject, username).info(
                " Removed Template file from [" + aProject.getName() + "] with ID ["
                        + aProject.getId() + "]");
        createLog(aProject, username).removeAllAppenders();
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

        createLog(aProject, aUsername).info(
                " Created Template file[ " + aFileName + "] for Project [" + aProject.getName()
                        + "] with ID [" + aProject.getId() + "]");
        createLog(aProject, aUsername).removeAllAppenders();
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
            SourceDocument aDocument)
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
    public List<SourceDocument> listTabSepDocuments(Project aProject)
    {
        List<SourceDocument> sourceDocuments = entityManager
                .createQuery("FROM SourceDocument where project =:project", SourceDocument.class)
                .setParameter("project", aProject).getResultList();
        List<SourceDocument> tabSepDocuments = new ArrayList<SourceDocument>();
        for (SourceDocument sourceDocument : sourceDocuments) {
            if (sourceDocument.getFormat().equals(WebAnnoConst.TAB_SEP)) {
                tabSepDocuments.add(sourceDocument);
            }
        }
        return tabSepDocuments;
    }

    private Logger createLog(Project aProject, String aUser)
        throws IOException
    {
        Logger logger = Logger.getLogger(MiraAutomationServiceImpl.class);
        String targetLog = dir.getAbsolutePath() + PROJECT + "project-" + aProject.getId() + ".log";
        FileAppender apndr = new FileAppender(new PatternLayout("%d [" + aUser + "] %m%n"),
                targetLog, true);
        logger.addAppender(apndr);
        logger.setLevel(Level.ALL);
        return logger;
    }
}
