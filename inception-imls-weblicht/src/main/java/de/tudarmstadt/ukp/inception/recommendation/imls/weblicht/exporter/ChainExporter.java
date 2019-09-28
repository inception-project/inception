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
package de.tudarmstadt.ukp.inception.recommendation.imls.weblicht.exporter;

import static java.util.Arrays.asList;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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
import de.tudarmstadt.ukp.inception.recommendation.imls.weblicht.chains.WeblichtChainService;
import de.tudarmstadt.ukp.inception.recommendation.imls.weblicht.model.WeblichtChain;

@Component
public class ChainExporter
    implements ProjectExporter
{
    private static final Logger LOG = LoggerFactory.getLogger(ChainExporter.class);

    private static final String KEY = "weblicht-chains";
    
    private static final String CHAINS_FOLDER = "weblicht-chains";
    

    private final RecommendationService recommendationService;
    private final WeblichtChainService chainService;
    
    @Autowired
    public ChainExporter(RecommendationService aRecommendationService,
            WeblichtChainService aChainService)
    {
        recommendationService = aRecommendationService;
        chainService = aChainService;
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
        
        List<ExportedWeblichtChain> exportedChains = new ArrayList<>();
        for (Recommender recommender : recommendationService.listRecommenders(project)) {
            Optional<WeblichtChain> optChain = chainService.getChain(recommender);
            if (optChain.isPresent()) {
                WeblichtChain chain = optChain.get();
                ExportedWeblichtChain exportedChain = new ExportedWeblichtChain();
                exportedChain.setId(chain.getId());
                exportedChain.setName(chain.getName());
                exportedChain.setRecommender(recommender.getName());
                exportedChains.add(exportedChain);
                
                File targetFolder = new File(aStage, CHAINS_FOLDER);
                targetFolder.mkdirs();
                
                Files.copy(chainService.getChainFile(chain).toPath(),
                        new File(targetFolder, chain.getId() + ".xml").toPath());
            }
        }
        
        aExProject.setProperty(KEY, exportedChains);
        
        LOG.info("Exported [{}] chains for project [{}]", exportedChains.size(),
                project.getName());
    }

    @Override
    public void importData(ProjectImportRequest aRequest, Project aProject,
            ExportedProject aExProject, ZipFile aZip)
        throws Exception
    {
        ExportedWeblichtChain[] exportedChains = aExProject.getArrayProperty(KEY,
                ExportedWeblichtChain.class);
        
        for (ExportedWeblichtChain exportedChain : exportedChains) {
            Recommender recommender = recommendationService
                    .getRecommender(aProject, exportedChain.getRecommender()).get();
            
            WeblichtChain chain = new WeblichtChain();
            chain.setName(exportedChain.getName());
            chain.setRecommender(recommender);
            chainService.createOrUpdateChain(chain);
            
            ZipEntry entry = aZip
                    .getEntry(CHAINS_FOLDER + "/" + exportedChain.getId() + ".xml");
            try (InputStream is = aZip.getInputStream(entry)) {
                chainService.importChainFile(chain, is);
            }
        }
        
        LOG.info("Imported [{}] chains for project [{}]", exportedChains.length,
                aProject.getName());
    }
}
