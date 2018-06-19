package de.tudarmstadt.ukp.inception.kb.exporter;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipFile;

import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.repository.config.RepositoryImplConfig;
import org.eclipse.rdf4j.repository.sparql.config.SPARQLRepositoryConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExportRequest;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExporter;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectImportRequest;
import de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedProject;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.inception.kb.KnowledgeBaseService;
import de.tudarmstadt.ukp.inception.kb.RepositoryType;
import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;
import de.tudarmstadt.ukp.inception.kb.reification.Reification;

@Component
public class KnowledgeBaseExporter implements ProjectExporter
{
    private static final String KEY = "knowledgeBases";
    private static final Logger LOG = LoggerFactory.getLogger(KnowledgeBaseExporter.class);
    
    private final KnowledgeBaseService kbService;
    
    @Autowired
    public KnowledgeBaseExporter(KnowledgeBaseService knowledgeBaseService)
    {
        kbService = knowledgeBaseService;
    }
    
    @Override
    public void exportData(ProjectExportRequest aRequest, ExportedProject aExProject, File aFile)
        throws Exception
    {
        Project project = aRequest.getProject();
        List<ExportedKnowledgeBase> exportedKnowledgeBases = new ArrayList<>();
        for (KnowledgeBase kb: kbService.getKnowledgeBases(project)) {
            ExportedKnowledgeBase exportedKB = new ExportedKnowledgeBase();
            exportedKB.setName(kb.getName());
            exportedKB.setType(kb.getType().toString());
            exportedKB.setClassIri(kb.getClassIri().stringValue());
            exportedKB.setSubclassIri(kb.getSubclassIri().stringValue());
            exportedKB.setTypeIri(kb.getTypeIri().stringValue());
            exportedKB.setDescriptionIri(kb.getDescriptionIri().stringValue());
            exportedKB.setLabelIri(kb.getLabelIri().stringValue());
            exportedKB.setPropertyTypeIri(kb.getPropertyTypeIri().stringValue());
            exportedKB.setReadOnly(kb.isReadOnly());
            exportedKB.setEnabled(kb.isEnabled());
            exportedKB.setReification(kb.getReification().toString());
            exportedKB.setSupportConceptLinking(kb.isSupportConceptLinking());
            exportedKB.setBasePrefix(kb.getBasePrefix());
            exportedKnowledgeBases.add(exportedKB);
            
            // set url for remote KB for local KB the value is just null
            if (kb.getType() == RepositoryType.REMOTE) {
                RepositoryImplConfig cfg = kbService.getKnowledgeBaseConfig(kb);
                String url = ((SPARQLRepositoryConfig) cfg).getQueryEndpointUrl();
                exportedKB.setRemoteURL(url);
            }
        }
        
        aExProject.setProperty(KEY, exportedKnowledgeBases);
        int n = exportedKnowledgeBases.size();
        LOG.info("Exported [{}] knowledge bases for project [{}]", n, project.getName());
    }

    @Override
    public void importData(ProjectImportRequest aRequest, Project aProject,
            ExportedProject aExProject, ZipFile aZip)
        throws Exception
    {
        ExportedKnowledgeBase[] knowledgeBases = aExProject
                .getArrayProperty(KEY, ExportedKnowledgeBase.class);
        ValueFactory vf = SimpleValueFactory.getInstance();
        for (ExportedKnowledgeBase exportedKB : knowledgeBases) {
            KnowledgeBase kb = new KnowledgeBase();
            kb.setName(exportedKB.getName());
            kb.setType(RepositoryType.valueOf(exportedKB.getType()));
            kb.setClassIri(vf.createIRI(exportedKB.getClassIri()));
            kb.setSubclassIri(vf.createIRI(exportedKB.getSubclassIri()));
            kb.setTypeIri(vf.createIRI(exportedKB.getTypeIri()));
            kb.setDescriptionIri(vf.createIRI(exportedKB.getDescriptionIri()));
            kb.setLabelIri(vf.createIRI(exportedKB.getLabelIri()));
            kb.setPropertyTypeIri(vf.createIRI(exportedKB.getPropertyTypeIri()));
            kb.setReadOnly(exportedKB.isReadOnly());
            kb.setEnabled(exportedKB.isEnabled());
            kb.setReification(Reification.valueOf(exportedKB.getReification()));
            kb.setSupportConceptLinking(exportedKB.isSupportConceptLinking());
            kb.setBasePrefix(exportedKB.getBasePrefix());
            kb.setProject(aProject);
            
            // Get config and register knowledge base
            RepositoryImplConfig cfg;
            switch (kb.getType()) {
            case LOCAL:
                cfg = kbService.getNativeConfig();
                kbService.registerKnowledgeBase(kb, cfg);
                break;
            case REMOTE:
                cfg = kbService.getRemoteConfig(exportedKB.getRemoteURL());
                kbService.registerKnowledgeBase(kb, cfg);
                break;
            default:
                throw new IllegalStateException();
            }
        }
        int n = knowledgeBases.length;
        LOG.info("Imported [{}] knowledge bases for project [{}]", n, aProject.getName());
    }
    
}
