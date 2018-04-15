package de.tudarmstadt.ukp.clarin.webanno.export.exporers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.uima.cas.CAS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst;
import de.tudarmstadt.ukp.clarin.webanno.automation.model.MiraTemplate;
import de.tudarmstadt.ukp.clarin.webanno.automation.service.AutomationService;
import de.tudarmstadt.ukp.clarin.webanno.export.LegacyProjectInitializer;
import de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedAnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedAnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedAnnotationFeatureReference;
import de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedAnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedAnnotationLayerReference;
import de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedMiraTemplate;
import de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedProject;
import de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedProjectPermission;
import de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedSourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedTag;
import de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedTagSet;
import de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedTrainingDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.ScriptDirection;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.Tag;
import de.tudarmstadt.ukp.clarin.webanno.model.TagSet;
import de.tudarmstadt.ukp.clarin.webanno.model.TrainingDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;

public class ProjectSettingsExporter
{
    private final Logger log = LoggerFactory.getLogger(getClass());
    
    private @Autowired AnnotationSchemaService annotationService;
    private @Autowired DocumentService documentService;
    private @Autowired ProjectService projectService;
    private @Autowired UserDao userRepository;
    private @Autowired(required = false) AutomationService automationService;
    
    public ExportedProject exportProjectSettings(Project aProject)
    {
        ExportedProject exProjekt = new ExportedProject();
        exProjekt.setDescription(aProject.getDescription());
        exProjekt.setName(aProject.getName());
        // In older versions of WebAnno, the mode was an enum which was serialized as upper-case
        // during export but as lower-case in the database. This is compensating for this case.
        exProjekt.setMode(StringUtils.upperCase(aProject.getMode(), Locale.US));
        exProjekt.setScriptDirection(aProject.getScriptDirection());
        exProjekt.setVersion(aProject.getVersion());
        exProjekt.setDisableExport(aProject.isDisableExport());
        exProjekt.setCreated(aProject.getCreated());
        exProjekt.setUpdated(aProject.getUpdated());

        exportPermissions(aProject, exProjekt);
        
        exportLayers(aProject, exProjekt);
        
        exportTagsets(aProject, exProjekt);

        exportDocuments(aProject, exProjekt);
        
        exportAutomationDocuments(aProject, exProjekt);
        
        exportAutomationMiraTemplates(aProject, exProjekt);
        
        return exProjekt;
    }
    
    private void exportPermissions(Project aProject, ExportedProject exProject)
    {
        // add project permissions to the project
        List<ExportedProjectPermission> projectPermissions = new ArrayList<>();
        for (User user : projectService.listProjectUsersWithPermissions(aProject)) {
            for (de.tudarmstadt.ukp.clarin.webanno.model.ProjectPermission permission : 
                    projectService.listProjectPermissionLevel(user, aProject)) {
                ExportedProjectPermission permissionToExport = new ExportedProjectPermission();
                permissionToExport.setLevel(permission.getLevel());
                permissionToExport.setUser(user.getUsername());
                projectPermissions.add(permissionToExport);
            }
        }
        exProject.setProjectPermissions(projectPermissions);
    }

    private void exportLayers(Project aProject, ExportedProject exProject)
    {
        List<ExportedAnnotationLayer> exLayers = new ArrayList<>();
        
        // Store map of layer and its equivalent exLayer so that the attach type is attached later
        Map<AnnotationLayer, ExportedAnnotationLayer> layerToExLayers = new HashMap<>();
        
        // Store map of feature and its equivalent exFeature so that the attach feature is attached
        // later
        Map<AnnotationFeature, ExportedAnnotationFeature> featureToExFeatures = new HashMap<>();
        for (AnnotationLayer layer : annotationService.listAnnotationLayer(aProject)) {
            exLayers.add(exportLayerDetails(layerToExLayers, featureToExFeatures, layer));
        }

        // add the attach-type and attach-feature to the exported layers and exported feature
        for (AnnotationLayer layer : layerToExLayers.keySet()) {
            if (layer.getAttachType() != null) {
                layerToExLayers.get(layer).setAttachType(
                        new ExportedAnnotationLayerReference(layer.getAttachType().getName()));
            }
            
            if (layer.getAttachFeature() != null) {
                layerToExLayers.get(layer).setAttachFeature(
                        new ExportedAnnotationFeatureReference(layer.getAttachFeature().getName()));
            }
        }
        
        exProject.setLayers(exLayers);
    }
    
    private void exportTagsets(Project aProject, ExportedProject exProject)
    {
        List<ExportedTagSet> extTagSets = new ArrayList<>();
        for (TagSet tagSet : annotationService.listTagSets(aProject)) {
            ExportedTagSet exTagSet = new ExportedTagSet();
            exTagSet.setCreateTag(tagSet.isCreateTag());
            exTagSet.setDescription(tagSet.getDescription());
            exTagSet.setLanguage(tagSet.getLanguage());
            exTagSet.setName(tagSet.getName());
            
            List<ExportedTag> exTags = new ArrayList<>();
            for (Tag tag : annotationService.listTags(tagSet)) {
                ExportedTag exTag = new ExportedTag();
                exTag.setDescription(tag.getDescription());
                exTag.setName(tag.getName());
                exTags.add(exTag);
            }
            exTagSet.setTags(exTags);
            extTagSets.add(exTagSet);
        }

        exProject.setTagSets(extTagSets);
    }
    
    private void exportDocuments(Project aProject, ExportedProject exProject)
    {
        List<ExportedSourceDocument> sourceDocuments = new ArrayList<>();
        List<ExportedAnnotationDocument> annotationDocuments = new ArrayList<>();

        // add source documents to a project
        List<SourceDocument> documents = documentService.listSourceDocuments(aProject);
        for (SourceDocument sourceDocument : documents) {
            ExportedSourceDocument exDocument = new ExportedSourceDocument();
            exDocument.setFormat(sourceDocument.getFormat());
            exDocument.setName(sourceDocument.getName());
            exDocument.setState(sourceDocument.getState());
            exDocument.setTimestamp(sourceDocument.getTimestamp());
            exDocument.setSentenceAccessed(sourceDocument.getSentenceAccessed());
            exDocument.setCreated(sourceDocument.getCreated());
            exDocument.setUpdated(sourceDocument.getUpdated());

            // add annotation document to Project
            for (AnnotationDocument annotationDocument : documentService
                    .listAnnotationDocuments(sourceDocument)) {
                ExportedAnnotationDocument annotationDocumentToExport = 
                        new ExportedAnnotationDocument();
                annotationDocumentToExport.setName(annotationDocument.getName());
                annotationDocumentToExport.setState(annotationDocument.getState());
                annotationDocumentToExport.setUser(annotationDocument.getUser());
                annotationDocumentToExport.setTimestamp(annotationDocument.getTimestamp());
                annotationDocumentToExport
                        .setSentenceAccessed(annotationDocument.getSentenceAccessed());
                annotationDocumentToExport.setCreated(annotationDocument.getCreated());
                annotationDocumentToExport.setUpdated(annotationDocument.getUpdated());
                annotationDocuments.add(annotationDocumentToExport);
            }
            sourceDocuments.add(exDocument);

        }

        exProject.setSourceDocuments(sourceDocuments);
        exProject.setAnnotationDocuments(annotationDocuments);
    }
    
    private void exportAutomationDocuments(Project aProject, ExportedProject exProject)
    {
        if (automationService != null) {
            List<ExportedTrainingDocument> trainDocuments = new ArrayList<>();
            List<TrainingDocument> trainingDocuments = automationService
                    .listTrainingDocuments(aProject);
            
            for (TrainingDocument trainingDocument : trainingDocuments) {
                ExportedTrainingDocument exDocument = new ExportedTrainingDocument();
                exDocument.setFormat(trainingDocument.getFormat());
                exDocument.setName(trainingDocument.getName());
                exDocument.setState(trainingDocument.getState());
                exDocument.setTimestamp(trainingDocument.getTimestamp());
                exDocument.setSentenceAccessed(trainingDocument.getSentenceAccessed());
                // During imported, we only really use the name of the feature to look up the
                // actual AnnotationFeature in the project
                if (trainingDocument.getFeature() != null) {
                    exDocument.setFeature(new ExportedAnnotationFeatureReference(
                            trainingDocument.getFeature().getName()));
                }
                trainDocuments.add(exDocument);
            }
            
            exProject.setTrainingDocuments(trainDocuments);
        }
        else {
            exProject.setTrainingDocuments(new ArrayList<>());
        }
    }
    
    private void exportAutomationMiraTemplates(Project aProject, ExportedProject exProject)
    {
        // export automation Mira template
        if (automationService != null) {
            List<ExportedMiraTemplate> exTemplates =
                    new ArrayList<>();
            for (MiraTemplate template : automationService.listMiraTemplates(aProject)) {
                ExportedMiraTemplate exTemplate = new ExportedMiraTemplate();
                exTemplate.setAnnotateAndPredict(template.isAnnotateAndRepeat());
                exTemplate.setAutomationStarted(template.isAutomationStarted());
                exTemplate.setCurrentLayer(template.isCurrentLayer());
                exTemplate.setResult(template.getResult());
                exTemplate.setTrainFeature(new ExportedAnnotationFeatureReference(
                        template.getTrainFeature().getName()));
    
                if (template.getOtherFeatures().size() > 0) {
                    Set<ExportedAnnotationFeatureReference>
                            exOtherFeatures = new HashSet<>();
                    for (AnnotationFeature feature : template.getOtherFeatures()) {
                        exOtherFeatures
                                .add(new ExportedAnnotationFeatureReference(feature.getName()));
                    }
                    exTemplate.setOtherFeatures(exOtherFeatures);
                }
                exTemplates.add(exTemplate);
            }
    
            exProject.setMiraTemplates(exTemplates);
        }
        else {
            exProject.setMiraTemplates(new ArrayList<>());
        }
    }
        
    public ExportedAnnotationLayer exportLayerDetails(
            Map<AnnotationLayer, ExportedAnnotationLayer> aLayerToExLayer,
            Map<AnnotationFeature, ExportedAnnotationFeature> aFeatureToExFeature,
            AnnotationLayer aLayer)
    {
        ExportedAnnotationLayer exLayer = new ExportedAnnotationLayer();
        exLayer.setAllowStacking(aLayer.isAllowStacking());
        exLayer.setBuiltIn(aLayer.isBuiltIn());
        exLayer.setReadonly(aLayer.isReadonly());
        exLayer.setCrossSentence(aLayer.isCrossSentence());
        exLayer.setDescription(aLayer.getDescription());
        exLayer.setEnabled(aLayer.isEnabled());
        exLayer.setLockToTokenOffset(aLayer.isLockToTokenOffset());
        exLayer.setMultipleTokens(aLayer.isMultipleTokens());
        exLayer.setLinkedListBehavior(aLayer.isLinkedListBehavior());
        exLayer.setName(aLayer.getName());
        exLayer.setProjectName(aLayer.getProject().getName());
        exLayer.setType(aLayer.getType());
        exLayer.setUiName(aLayer.getUiName());

        if (aLayerToExLayer != null) {
            aLayerToExLayer.put(aLayer, exLayer);
        }

        // Export features
        List<ExportedAnnotationFeature> exFeatures = new ArrayList<>();
        for (AnnotationFeature feature : annotationService.listAnnotationFeature(aLayer)) {
            ExportedAnnotationFeature exFeature = exportFeatureDetails(feature);
            exFeatures.add(exFeature);
            
            if (aFeatureToExFeature != null) {
                aFeatureToExFeature.put(feature, exFeature);
            }
        }
        exLayer.setFeatures(exFeatures);
        
        return exLayer;
    }    
    
    public ExportedAnnotationFeature exportFeatureDetails(AnnotationFeature feature)
    {
        ExportedAnnotationFeature exFeature = new ExportedAnnotationFeature();
        exFeature.setDescription(feature.getDescription());
        exFeature.setEnabled(feature.isEnabled());
        exFeature.setRemember(feature.isRemember());
        exFeature.setRequired(feature.isRequired());
        exFeature.setHideUnconstraintFeature(feature.isHideUnconstraintFeature());
        exFeature.setName(feature.getName());
        exFeature.setProjectName(feature.getProject().getName());
        exFeature.setType(feature.getType());
        exFeature.setUiName(feature.getUiName());
        exFeature.setVisible(feature.isVisible());
        exFeature.setMultiValueMode(feature.getMultiValueMode());
        exFeature.setLinkMode(feature.getLinkMode());
        exFeature.setLinkTypeName(feature.getLinkTypeName());
        exFeature.setLinkTypeRoleFeatureName(feature.getLinkTypeRoleFeatureName());
        exFeature.setLinkTypeTargetFeatureName(feature.getLinkTypeTargetFeatureName());
        
        if (feature.getTagset() != null) {
            TagSet tagSet = feature.getTagset();
            ExportedTagSet exTagSet = new ExportedTagSet();
            exTagSet.setDescription(tagSet.getDescription());
            exTagSet.setLanguage(tagSet.getLanguage());
            exTagSet.setName(tagSet.getName());
            exTagSet.setCreateTag(tagSet.isCreateTag());

            List<ExportedTag> exportedTags = new ArrayList<>();
            for (Tag tag : annotationService.listTags(tagSet)) {
                ExportedTag exTag = new ExportedTag();
                exTag.setDescription(tag.getDescription());
                exTag.setName(tag.getName());
                exportedTags.add(exTag);
            }
            exTagSet.setTags(exportedTags);
            exFeature.setTagSet(exTagSet);
        }
        
        return exFeature;
    }

    /**
     * create new {@link Project} from the {@link ExportedProject} model
     * 
     * @param aProject
     *            the project
     * @return the project.
     * @throws IOException
     *             if an I/O error occurs.
     */
    public Project importProject(ExportedProject aProject) throws IOException
    {
        Project project = new Project();
        
        // If the name of the project is already taken, generate a new name
        String projectName = aProject.getName();
        if (projectService.existsProject(projectName)) {
            projectName = copyProjectName(projectName);
        }
        project.setName(projectName);
        
        project.setDescription(aProject.getDescription());
        // In older versions of WebAnno, the mode was an enum which was serialized as upper-case
        // during export but as lower-case in the database. This is compensating for this case.
        project.setMode(StringUtils.lowerCase(aProject.getMode(), Locale.US));
        project.setDisableExport(aProject.isDisableExport());
        project.setCreated(aProject.getCreated());
        project.setUpdated(aProject.getUpdated());
        
        // Set default to LTR on import from old WebAnno versions
        if (aProject.getScriptDirection() == null) {
            project.setScriptDirection(ScriptDirection.LTR);
        }
        else {
            project.setScriptDirection(aProject.getScriptDirection());
        }
        
        projectService.createProject(project);
        return project;
    }
    
    /**
     * Create a {@link TagSet} for the imported project,
     * 
     * @param aProject
     *            a project.
     * @param aImportedProjectSetting
     *            the settings.
     * @return hum?
     * @throws IOException
     *             if an I/O error occurs.
     */
    public Map<String, AnnotationFeature> importLayers(Project aProject,
            ExportedProject aImportedProjectSetting)
        throws IOException
    {
        User user = userRepository.getCurrentUser();
        
        // this is projects prior to version 2.0
        if (aImportedProjectSetting.getVersion() == 0) {
            return importLayersV0(aProject, aImportedProjectSetting, user);
        }
        else {
            return importLayersV1(aProject, aImportedProjectSetting, user);
        }
    }
    
    /**
     * Import tagsets from projects prior to WebAnno 2.0.
     */
    private Map<String, AnnotationFeature> importLayersV0(Project aProject,
            ExportedProject aImportedProjectSetting, User user)
        throws IOException
    {
        List<ExportedTagSet> importedTagSets = aImportedProjectSetting.getTagSets();
        
        List<String> posTags = new ArrayList<>();
        List<String> depTags = new ArrayList<>();
        List<String> neTags = new ArrayList<>();
        List<String> posTagDescriptions = new ArrayList<>();
        List<String> depTagDescriptions = new ArrayList<>();
        List<String> neTagDescriptions = new ArrayList<>();
        List<String> corefTypeTags = new ArrayList<>();
        List<String> corefRelTags = new ArrayList<>();
        for (ExportedTagSet tagSet : importedTagSets) {
            switch (tagSet.getTypeName()) {
            case WebAnnoConst.POS:
                for (ExportedTag tag : tagSet.getTags()) {
                    posTags.add(tag.getName());
                    posTagDescriptions.add(tag.getDescription());
                }
                break;
            case WebAnnoConst.DEPENDENCY:
                for (ExportedTag tag : tagSet.getTags()) {
                    depTags.add(tag.getName());
                    depTagDescriptions.add(tag.getDescription());
                }
                break;
            case WebAnnoConst.NAMEDENTITY:
                for (ExportedTag tag : tagSet.getTags()) {
                    neTags.add(tag.getName());
                    neTagDescriptions.add(tag.getDescription());
                }
                break;
            case WebAnnoConst.COREFRELTYPE:
                for (ExportedTag tag : tagSet.getTags()) {
                    corefTypeTags.add(tag.getName());
                }
                break;
            case WebAnnoConst.COREFERENCE:
                for (ExportedTag tag : tagSet.getTags()) {
                    corefRelTags.add(tag.getName());
                }
                break;
            }
        }
        
        new LegacyProjectInitializer(annotationService).initialize(aProject,
                posTags.toArray(new String[0]), posTagDescriptions.toArray(new String[0]),
                depTags.toArray(new String[0]), depTagDescriptions.toArray(new String[0]),
                neTags.toArray(new String[0]), neTagDescriptions.toArray(new String[0]),
                corefTypeTags.toArray(new String[0]), corefRelTags.toArray(new String[0]));
        
        return new HashMap<>();
    }
    
    private Map<String, AnnotationFeature> importLayersV1(Project aProject,
            ExportedProject aImportedProjectSetting, User aUser)
        throws IOException
    {
        Map<String, AnnotationFeature> featuresMap = new HashMap<>();
        Map<ExportedAnnotationLayer, AnnotationLayer>
            layersMap = new HashMap<>();
        for (ExportedAnnotationLayer exLayer :
                aImportedProjectSetting.getLayers()) {
            if (annotationService.existsLayer(exLayer.getName(), exLayer.getType(), aProject)) {
                AnnotationLayer layer = annotationService.getLayer(exLayer.getName(), aProject);
                importLayer(layer, exLayer, aProject, aUser);
                layersMap.put(exLayer, layer);
                for (ExportedAnnotationFeature exfeature : exLayer.getFeatures()) {
                    if (annotationService.existsFeature(exfeature.getName(), layer)) {
                        AnnotationFeature feature = annotationService.getFeature(
                                exfeature.getName(), layer);
                        importFeature(feature, exfeature, aProject, aUser);
                        featuresMap.put(exfeature.getName(), feature);
                        continue;
                    }
                    AnnotationFeature feature = new AnnotationFeature();
                    feature.setLayer(layer);
                    importFeature(feature, exfeature, aProject, aUser);
                    featuresMap.put(exfeature.getName(), feature);
                }
            }
            else {
                AnnotationLayer layer = new AnnotationLayer();
                importLayer(layer, exLayer, aProject, aUser);
                layersMap.put(exLayer, layer);
                for (ExportedAnnotationFeature exfeature : exLayer.getFeatures()) {
                    AnnotationFeature feature = new AnnotationFeature();
                    feature.setLayer(layer);
                    importFeature(feature, exfeature, aProject, aUser);
                    featuresMap.put(exfeature.getName(), feature);
                }
            }
        }

        for (ExportedTagSet exTagSet : aImportedProjectSetting.getTagSets()) {
            importTagSet(new TagSet(), exTagSet, aProject, aUser);
        }

        for (ExportedAnnotationLayer exLayer :
                aImportedProjectSetting.getLayers()) {
            if (exLayer.getAttachType() != null) {
                AnnotationLayer layer = annotationService.getLayer(exLayer.getName(), aProject);
                AnnotationLayer attachLayer = annotationService.getLayer(exLayer.getAttachType()
                        .getName(), aProject);
                layer.setAttachType(attachLayer);
                annotationService.createLayer(layersMap.get(exLayer));
            }
            if (exLayer.getAttachFeature() != null) {
                layersMap.get(exLayer)
                        .setAttachFeature(featuresMap.get(exLayer.getAttachFeature().getName()));
                annotationService.createLayer(layersMap.get(exLayer));
            }

            for (ExportedAnnotationFeature eXFeature : exLayer.getFeatures()) {
                if (eXFeature.getTagSet() != null) {
                    featuresMap.get(eXFeature.getName())
                            .setTagset(
                                    annotationService.getTagSet(eXFeature.getTagSet().getName(),
                                            aProject));
                }
            }
        }
        return featuresMap;
    }
    
    public void importLayer(AnnotationLayer aLayer, ExportedAnnotationLayer aExLayer,
            Project aProject, User aUser)
        throws IOException
    {
        aLayer.setAllowStacking(aExLayer.isAllowStacking());
        aLayer.setBuiltIn(aExLayer.isBuiltIn());
        aLayer.setReadonly(aExLayer.isReadonly());
        aLayer.setCrossSentence(aExLayer.isCrossSentence());
        aLayer.setDescription(aExLayer.getDescription());
        aLayer.setEnabled(aExLayer.isEnabled());
        aLayer.setLockToTokenOffset(aExLayer.isLockToTokenOffset());
        aLayer.setMultipleTokens(aExLayer.isMultipleTokens());
        aLayer.setLinkedListBehavior(aExLayer.isLinkedListBehavior());
        aLayer.setUiName(aExLayer.getUiName());
        aLayer.setName(aExLayer.getName());
        aLayer.setProject(aProject);
        aLayer.setType(aExLayer.getType());
        annotationService.createLayer(aLayer);
    }

    public void importFeature(AnnotationFeature aFeature, ExportedAnnotationFeature aExFeature,
            Project aProject, User aUser)
    {
        aFeature.setDescription(aExFeature.getDescription());
        aFeature.setEnabled(aExFeature.isEnabled());
        aFeature.setVisible(aExFeature.isVisible());
        aFeature.setUiName(aExFeature.getUiName());
        aFeature.setProject(aProject);
        aFeature.setLayer(aFeature.getLayer());
        boolean isItChainedLayer = aFeature.getLayer().getType().equals(WebAnnoConst.CHAIN_TYPE);
        if (isItChainedLayer && (aExFeature.getName().equals(WebAnnoConst.COREFERENCE_TYPE_FEATURE)
                || aExFeature.getName().equals(WebAnnoConst.COREFERENCE_RELATION_FEATURE))) {
            aFeature.setType(CAS.TYPE_NAME_STRING);
        }
        else {
            aFeature.setType(aExFeature.getType());
        }
        aFeature.setName(aExFeature.getName());
        aFeature.setRemember(aExFeature.isRemember());
        aFeature.setRequired(aExFeature.isRequired());
        aFeature.setHideUnconstraintFeature(aExFeature.isHideUnconstraintFeature());
        aFeature.setMode(aExFeature.getMultiValueMode());
        aFeature.setLinkMode(aExFeature.getLinkMode());
        aFeature.setLinkTypeName(aExFeature.getLinkTypeName());
        aFeature.setLinkTypeRoleFeatureName(aExFeature.getLinkTypeRoleFeatureName());
        aFeature.setLinkTypeTargetFeatureName(aExFeature.getLinkTypeTargetFeatureName());

        annotationService.createFeature(aFeature);
    }
    
    public void importTagSet(TagSet aTagSet, ExportedTagSet aExTagSet, Project aProject, User aUser)
        throws IOException
    {
        // aTagSet is a parameter because we want to use this also in the project settings
        // panel and have the ability there to merge imported tags into an existing tagset
        aTagSet.setCreateTag(aExTagSet.isCreateTag());
        aTagSet.setDescription(aExTagSet.getDescription());
        aTagSet.setLanguage(aExTagSet.getLanguage());
        aTagSet.setName(aExTagSet.getName());
        aTagSet.setProject(aProject);
        annotationService.createTagSet(aTagSet);

        for (ExportedTag exTag : aExTagSet.getTags()) {
            // do not duplicate tag
            if (annotationService.existsTag(exTag.getName(), aTagSet)) {
                continue;
            }
            Tag tag = new Tag();
            tag.setDescription(exTag.getDescription());
            tag.setTagSet(aTagSet);
            tag.setName(exTag.getName());
            annotationService.createTag(tag);
        }
    }
    
    /**
     * Get a project name to be used when importing. Use the prefix, copy_of_...+ i to avoid
     * conflicts
     */
    private String copyProjectName(String aProjectName)
    {
        String projectName = "copy_of_" + aProjectName;
        int i = 1;
        while (true) {
            if (projectService.existsProject(projectName)) {
                projectName = "copy_of_" + aProjectName + "(" + i + ")";
                i++;
            }
            else {
                return projectName;
            }
        }
    }
}
