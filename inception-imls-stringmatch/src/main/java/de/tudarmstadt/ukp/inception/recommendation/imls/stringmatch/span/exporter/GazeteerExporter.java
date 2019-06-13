/*
 * Copyright 2019
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.inception.recommendation.imls.stringmatch.span.exporter;

import static java.util.Arrays.asList;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExportRequest;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExporter;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectImportRequest;
import de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedProject;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.inception.recommendation.api.RecommendationService;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.exporter.RecommenderExporter;
import de.tudarmstadt.ukp.inception.recommendation.imls.stringmatch.span.gazeteer.GazeteerService;
import de.tudarmstadt.ukp.inception.recommendation.imls.stringmatch.span.model.Gazeteer;

@Component
public class GazeteerExporter
    implements ProjectExporter
{
    private static final Logger LOG = LoggerFactory.getLogger(GazeteerExporter.class);

    private static final String KEY = "gazeteers";
    
    private static final String GAZETEERS_FOLDER = "gazeteers";
    

    private final RecommendationService recommendationService;
    private final GazeteerService gazeteerService;
    
    @Autowired
    public GazeteerExporter(RecommendationService aRecommendationService,
            GazeteerService aGazeteerService)
    {
        recommendationService = aRecommendationService;
        gazeteerService = aGazeteerService;
    }

    @Override
    public List<Class<? extends ProjectExporter>> getImportDependencies() {
        return asList(RecommenderExporter.class);
    }

    @Override
    public void exportData(ProjectExportRequest aRequest, ExportedProject aExProject, File aStage)
        throws Exception
    {
        Project project = aRequest.getProject();
        
        List<ExportedGazeteer> exportedGazeteers = new ArrayList<>();
        for (Recommender recommender : recommendationService.listRecommenders(project)) {
            for (Gazeteer gazeteer : gazeteerService.listGazeteers(recommender)) {
                ExportedGazeteer exportedGazeteer = new ExportedGazeteer();
                exportedGazeteer.setId(gazeteer.getId());
                exportedGazeteer.setName(gazeteer.getName());
                exportedGazeteer.setRecommender(recommender.getName());
                exportedGazeteers.add(exportedGazeteer);
                
                File targetFolder = new File(aStage, GAZETEERS_FOLDER);
                targetFolder.mkdirs();
                
                Files.copy(gazeteerService.getGazeteerFile(gazeteer).toPath(),
                        new File(targetFolder, gazeteer.getId() + ".txt").toPath());
            }
        }
        
        aExProject.setProperty(KEY, exportedGazeteers);
        
        LOG.info("Exported [{}] gazeteers for project [{}]", exportedGazeteers.size(),
                project.getName());
    }

    @Override
    public void importData(ProjectImportRequest aRequest, Project aProject,
            ExportedProject aExProject, ZipFile aZip)
        throws Exception
    {
        ExportedGazeteer[] gazeteers = aExProject.getArrayProperty(KEY, ExportedGazeteer.class);
        
        for (ExportedGazeteer exportedGazeteer : gazeteers) {
            Recommender recommender = recommendationService
                    .getRecommender(aProject, exportedGazeteer.getRecommender()).get();
            
            Gazeteer gazeteer = new Gazeteer();
            gazeteer.setName(exportedGazeteer.getName());
            gazeteer.setRecommender(recommender);
            gazeteerService.createOrUpdateGazeteer(gazeteer);
            
            ZipEntry entry = aZip
                    .getEntry(GAZETEERS_FOLDER + "/" + exportedGazeteer.getId() + ".txt");
            try (InputStream is = aZip.getInputStream(entry)) {
                gazeteerService.importGazeteerFile(gazeteer, is);
            }
        }
        
        LOG.info("Imported [{}] gazeteeres for project [{}]", gazeteers.length, aProject.getName());
    }
}
