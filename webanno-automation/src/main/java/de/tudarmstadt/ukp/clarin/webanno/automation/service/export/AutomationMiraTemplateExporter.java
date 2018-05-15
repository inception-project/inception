/*
 * Copyright 2018
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
package de.tudarmstadt.ukp.clarin.webanno.automation.service.export;

import static java.util.Arrays.asList;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipFile;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.export.exporters.LayerExporter;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExportRequest;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExporter;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectImportRequest;
import de.tudarmstadt.ukp.clarin.webanno.automation.model.MiraTemplate;
import de.tudarmstadt.ukp.clarin.webanno.automation.service.AutomationService;
import de.tudarmstadt.ukp.clarin.webanno.automation.service.export.model.ExportedMiraTemplate;
import de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedAnnotationFeatureReference;
import de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedProject;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;

@Component
public class AutomationMiraTemplateExporter
    implements ProjectExporter
{
    private static final String MIRA_TEMPLATES = "mira_templates";
    
    private @Autowired AnnotationSchemaService annotationService;
    private @Autowired AutomationService automationService;

    @Override
    public List<Class<? extends ProjectExporter>> getImportDependencies()
    {
        return asList(LayerExporter.class);
    }
    
    @Override
    public void exportData(ProjectExportRequest aRequest, ExportedProject aExProject, File aStage)
        throws Exception
    {
        List<ExportedMiraTemplate> exTemplates = new ArrayList<>();
        for (MiraTemplate template : automationService.listMiraTemplates(aRequest.getProject())) {
            ExportedMiraTemplate exTemplate = new ExportedMiraTemplate();
            exTemplate.setAnnotateAndPredict(template.isAnnotateAndRepeat());
            exTemplate.setAutomationStarted(template.isAutomationStarted());
            exTemplate.setCurrentLayer(template.isCurrentLayer());
            exTemplate.setResult(template.getResult());
            exTemplate.setTrainFeature(
                    new ExportedAnnotationFeatureReference(template.getTrainFeature()));

            if (template.getOtherFeatures().size() > 0) {
                Set<ExportedAnnotationFeatureReference> exOtherFeatures = new HashSet<>();
                for (AnnotationFeature feature : template.getOtherFeatures()) {
                    exOtherFeatures.add(new ExportedAnnotationFeatureReference(feature));
                }
                exTemplate.setOtherFeatures(exOtherFeatures);
            }
            exTemplates.add(exTemplate);
        }

        aExProject.setProperty(MIRA_TEMPLATES, exTemplates);
    }
    
    @Override
    public void importData(ProjectImportRequest aRequest, Project aProject,
            ExportedProject aExProject, ZipFile aZip)
        throws Exception
    {
        ExportedMiraTemplate[] templates = aExProject.getArrayProperty(MIRA_TEMPLATES,
                ExportedMiraTemplate.class);
        
        for (ExportedMiraTemplate exTemplate : templates) {
            MiraTemplate template = new MiraTemplate();
            template.setAnnotateAndRepeat(exTemplate.isAnnotateAndPredict());
            template.setAutomationStarted(false);
            template.setCurrentLayer(exTemplate.isCurrentLayer());
            template.setResult("---");
            AnnotationLayer trainingLayer = annotationService
                    .getLayer(exTemplate.getTrainFeature().getLayer(), aProject);
            AnnotationFeature trainingFeature = annotationService
                    .getFeature(exTemplate.getTrainFeature().getName(), trainingLayer);
            template.setTrainFeature(trainingFeature);
            Set<AnnotationFeature> otherFeatures = new HashSet<>();
            if (exTemplate.getOtherFeatures() != null) {
                for (ExportedAnnotationFeatureReference other : exTemplate.getOtherFeatures()) {
                    AnnotationLayer layer = annotationService.getLayer(other.getLayer(), aProject);
                    AnnotationFeature feature = annotationService.getFeature(other.getName(),
                            layer);
                    otherFeatures.add(feature);
                }
                template.setOtherFeatures(otherFeatures);
            }
            automationService.createTemplate(template);
        }
    }
}
