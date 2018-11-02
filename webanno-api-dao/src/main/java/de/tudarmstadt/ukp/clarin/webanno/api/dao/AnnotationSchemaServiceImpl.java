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
package de.tudarmstadt.ukp.clarin.webanno.api.dao;

import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.CHAIN_TYPE;
import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.RELATION_TYPE;
import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.SPAN_TYPE;
import static java.util.Arrays.asList;
import static java.util.Objects.isNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;

import org.apache.commons.collections4.SetUtils;
import org.apache.commons.lang3.ClassUtils;
import org.apache.uima.UIMAException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.cas.impl.CASCompleteSerializer;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.impl.Serialization;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.factory.TypeSystemDescriptionFactory;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.FeatureDescription;
import org.apache.uima.resource.metadata.TypeDescription;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.resource.metadata.impl.TypeSystemDescription_impl;
import org.apache.uima.util.CasCreationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.adapter.TypeAdapter;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureSupport;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.layer.LayerSupportRegistry;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.initializers.ProjectInitializer;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.LinkMode;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.Tag;
import de.tudarmstadt.ukp.clarin.webanno.model.TagSet;
import de.tudarmstadt.ukp.clarin.webanno.support.logging.Logging;

/**
 * Implementation of methods defined in the {@link AnnotationSchemaService} interface
 */
@Component(AnnotationSchemaService.SERVICE_NAME)
public class AnnotationSchemaServiceImpl
    implements AnnotationSchemaService
{
    private final Logger log = LoggerFactory.getLogger(getClass());

    @Value(value = "${repository.path}")
    private File dir;

    private @PersistenceContext EntityManager entityManager;
    private @Autowired FeatureSupportRegistry featureSupportRegistry;
    private @Autowired ApplicationEventPublisher applicationEventPublisher;
    private @Autowired LayerSupportRegistry layerSupportRegistry;
    private @Lazy @Autowired(required = false) List<ProjectInitializer> initializerProxy;
    private List<ProjectInitializer> initializers;

    public AnnotationSchemaServiceImpl()
    {
        // Nothing to do
    }

    @EventListener
    public void onContextRefreshedEvent(ContextRefreshedEvent aEvent)
    {
        init();
    }
    
    /* package private */ void init()
    {
        List<ProjectInitializer> inits = new ArrayList<>();

        if (initializerProxy != null) {
            inits.addAll(initializerProxy);
            AnnotationAwareOrderComparator.sort(inits);
        
            Set<Class<? extends ProjectInitializer>> initializerClasses = new HashSet<>();
            for (ProjectInitializer init : inits) {
                if (initializerClasses.add(init.getClass())) {
                    log.info("Found project initializer: {}",
                            ClassUtils.getAbbreviatedName(init.getClass(), 20));
                }
                else {
                    throw new IllegalStateException("There cannot be more than once instance "
                            + "of each project initializer class! Duplicate instance of class: "
                                    + init.getClass());
                }
            }
        }
        
        initializers = Collections.unmodifiableList(inits);
    }

    @Override
    @Transactional
    public void createTag(Tag aTag)
    {
        if (isNull(aTag.getId())) {
            entityManager.persist(aTag);
        }
        else {
            entityManager.merge(aTag);
        }

        try (MDC.MDCCloseable closable = MDC.putCloseable(Logging.KEY_PROJECT_ID,
                String.valueOf(aTag.getTagSet().getProject().getId()))) {
            TagSet tagset = aTag.getTagSet();
            Project project = tagset.getProject();
            log.info("Created tag [{}]({}) in tagset [{}]({}) in project [{}]({})", aTag.getName(),
                    aTag.getId(), tagset.getName(), tagset.getId(), project.getName(),
                    project.getId());
        }
    }

    @Override
    @Transactional
    public void createTagSet(TagSet aTagSet)
    {
        if (isNull(aTagSet.getId())) {
            entityManager.persist(aTagSet);
        }
        else {
            entityManager.merge(aTagSet);
        }
        
        try (MDC.MDCCloseable closable = MDC.putCloseable(Logging.KEY_PROJECT_ID,
                String.valueOf(aTagSet.getProject().getId()))) {
            Project project = aTagSet.getProject();
            log.info("Created tagset [{}]({}) in project [{}]({})", aTagSet.getName(),
                    aTagSet.getId(), project.getName(), project.getId());
        }
    }

    @Override
    @Transactional
    public void createLayer(AnnotationLayer aLayer)
    {
        if (isNull(aLayer.getId())) {
            entityManager.persist(aLayer);
        }
        else {
            entityManager.merge(aLayer);
        }
        
        try (MDC.MDCCloseable closable = MDC.putCloseable(Logging.KEY_PROJECT_ID,
                String.valueOf(aLayer.getProject().getId()))) {
            Project project = aLayer.getProject();
            log.info("Created layer [{}]({}) in project [{}]({})", aLayer.getName(),
                    aLayer.getId(), project.getName(), project.getId());
        }
    }

    @Override
    @Transactional
    public void createFeature(AnnotationFeature aFeature)
    {
        if (isNull(aFeature.getId())) {
            entityManager.persist(aFeature);
        }
        else {
            entityManager.merge(aFeature);
        }
    }

    @Override
    @Transactional
    public Optional<Tag> getTag(long aId)
    {
        try {
            final String query = "FROM Tag WHERE id = :id";
            return Optional.of(entityManager.createQuery(query, Tag.class)
                    .setParameter("id", aId)
                    .getSingleResult());
        }
        catch (NoResultException e) {
            return Optional.empty();
        }
    }
    
    @Override
    @Transactional
    public Tag getTag(String aTagName, TagSet aTagSet)
    {
        return entityManager
                .createQuery("FROM Tag WHERE name = :name AND" + " tagSet =:tagSet", Tag.class)
                .setParameter("name", aTagName).setParameter("tagSet", aTagSet).getSingleResult();
    }

    @Override
    public boolean existsTag(String aTagName, TagSet aTagSet)
    {

        try {
            getTag(aTagName, aTagSet);
            return true;
        }
        catch (NoResultException e) {
            return false;
        }
    }

    @Override
    @Transactional(noRollbackFor = NoResultException.class)
    public boolean existsTagSet(String aName, Project aProject)
    {
        try {
            entityManager
                    .createQuery("FROM TagSet WHERE name = :name AND project = :project",
                            TagSet.class).setParameter("name", aName)
                    .setParameter("project", aProject).getSingleResult();
            return true;
        }
        catch (NoResultException e) {
            return false;

        }
    }

    @Override
    @Transactional(noRollbackFor = NoResultException.class)
    public boolean existsTagSet(Project aProject)
    {
        try {
            entityManager.createQuery("FROM TagSet WHERE  project = :project", TagSet.class)
                    .setParameter("project", aProject).getSingleResult();
            return true;
        }
        catch (NoResultException e) {
            return false;

        }
    }

    @Override
    @Transactional(noRollbackFor = NoResultException.class)
    public boolean existsLayer(String aName, Project aProject)
    {
        try {
            entityManager
                    .createQuery(
                            "FROM AnnotationLayer WHERE name = :name AND project = :project",
                            AnnotationLayer.class)
                    .setParameter("name", aName)
                    .setParameter("project", aProject)
                    .getSingleResult();
            return true;
        }
        catch (NoResultException e) {
            return false;
        }
    }

    @Override
    @Transactional(noRollbackFor = NoResultException.class)
    public boolean existsLayer(String aName, String aType, Project aProject)
    {
        try {
            entityManager
                    .createQuery(
                            "FROM AnnotationLayer WHERE name = :name AND type = :type AND project = :project",
                            AnnotationLayer.class)
                    .setParameter("name", aName).setParameter("type", aType)
                    .setParameter("project", aProject).getSingleResult();
            return true;
        }
        catch (NoResultException e) {
            return false;

        }
    }

    @Override
    public boolean existsFeature(String aName, AnnotationLayer aLayer)
    {

        try {
            entityManager
                    .createQuery("FROM AnnotationFeature WHERE name = :name AND layer = :layer",
                            AnnotationFeature.class).setParameter("name", aName)
                    .setParameter("layer", aLayer).getSingleResult();
            return true;
        }
        catch (NoResultException e) {
            return false;

        }
    }

    @Override
    @Transactional
    public TagSet getTagSet(String aName, Project aProject)
    {
        return entityManager
                .createQuery("FROM TagSet WHERE name = :name AND project =:project", TagSet.class)
                .setParameter("name", aName).setParameter("project", aProject).getSingleResult();
    }

    @Override
    @Transactional
    public TagSet getTagSet(long aId)
    {
        return entityManager.createQuery("FROM TagSet WHERE id = :id", TagSet.class)
                .setParameter("id", aId).getSingleResult();
    }

    @Override
    @Transactional
    public AnnotationLayer getLayer(long aId)
    {
        return entityManager
                .createQuery("FROM AnnotationLayer WHERE id = :id", AnnotationLayer.class)
                .setParameter("id", aId).getSingleResult();
    }

    @Override
    @Transactional(noRollbackFor = NoResultException.class)
    public AnnotationLayer getLayer(String aName, Project aProject)
    {
        return entityManager
                .createQuery("From AnnotationLayer where name = :name AND project =:project",
                        AnnotationLayer.class).setParameter("name", aName)
                .setParameter("project", aProject).getSingleResult();
    }
    
    @Override
    @Transactional(noRollbackFor = NoResultException.class)
    public AnnotationLayer getLayer(Project aProject, FeatureStructure aFS)
    {
        String layerName = aFS.getType().getName();
        AnnotationLayer layer;
        try {
            layer = getLayer(layerName, aProject);
        }
        catch (NoResultException e) {
            if (layerName.endsWith("Chain")) {
                layerName = layerName.substring(0, layerName.length() - 5);
            }
            if (layerName.endsWith("Link")) {
                layerName = layerName.substring(0, layerName.length() - 4);
            }
            layer = getLayer(layerName, aProject);
        }

        return layer;
    }

    @Override
    @Transactional(noRollbackFor = NoResultException.class)
    public AnnotationFeature getFeature(long aId)
    {
        return entityManager
                .createQuery("From AnnotationFeature where id = :id", AnnotationFeature.class)
                .setParameter("id", aId).getSingleResult();
    }

    @Override
    @Transactional(noRollbackFor = NoResultException.class)
    public AnnotationFeature getFeature(String aName, AnnotationLayer aLayer)
    {
        return entityManager
                .createQuery("From AnnotationFeature where name = :name AND layer = :layer",
                        AnnotationFeature.class).setParameter("name", aName)
                .setParameter("layer", aLayer).getSingleResult();
    }

    @Override
    @Transactional(noRollbackFor = NoResultException.class)
    public boolean existsType(String aName, String aType)
    {
        try {
            entityManager
                    .createQuery("From AnnotationLayer where name = :name AND type = :type",
                            AnnotationLayer.class).setParameter("name", aName)
                    .setParameter("type", aType).getSingleResult();
            return true;
        }
        catch (NoResultException e) {
            return false;
        }
    }

    @Override
    @Transactional
    public TagSet createTagSet(String aDescription, String aTagSetName, String aLanguage,
            String[] aTags, String[] aTagDescription, Project aProject)
                throws IOException
    {
        TagSet tagSet = new TagSet();
        tagSet.setDescription(aDescription);
        tagSet.setLanguage(aLanguage);
        tagSet.setName(aTagSetName);
        tagSet.setProject(aProject);

        createTagSet(tagSet);

        int i = 0;
        for (String tagName : aTags) {
            Tag tag = new Tag();
            tag.setTagSet(tagSet);
            tag.setDescription(aTagDescription[i]);
            tag.setName(tagName);
            createTag(tag);
            i++;
        }
        
        return tagSet;
    }

    @Override
    @Transactional
    public void initializeProject(Project aProject)
        throws IOException
    {
        Deque<ProjectInitializer> deque = new LinkedList<>(initializers);
        Set<Class<? extends ProjectInitializer>> initsSeen = new HashSet<>();
        Set<ProjectInitializer> initsDeferred = SetUtils.newIdentityHashSet();

        Set<Class<? extends ProjectInitializer>> allInits = new HashSet<>();

        for (ProjectInitializer initializer : deque) {
            allInits.add(initializer.getClass());
        }
        
        while (!deque.isEmpty()) {
            ProjectInitializer initializer = deque.pop();

            if (!allInits.containsAll(initializer.getDependencies())) {
                throw new IllegalStateException(
                        "Missing dependencies of " + initializer + " initializer from " + deque);
            }

            if (initsDeferred.contains(initializer)) {
                throw new IllegalStateException("Circular initializer dependencies in "
                        + initsDeferred + " via " + initializer);
            }
            
            if (initsSeen.containsAll(initializer.getDependencies())) {
                log.debug("Applying project initializer: {}", initializer);
                initializer.configure(aProject);
                initsSeen.add(initializer.getClass());
                initsDeferred.clear();
            }
            else {
                log.debug(
                        "Deferring project initializer as dependencies are not yet fulfilled: [{}]",
                        initializer);
                deque.add(initializer);
                initsDeferred.add(initializer);
            }
        }
    }

    @Override
    @Transactional
    public List<AnnotationLayer> listAnnotationType()
    {
        return entityManager.createQuery("FROM AnnotationLayer ORDER BY name",
                AnnotationLayer.class).getResultList();
    }

    @Override
    @Transactional
    public List<AnnotationLayer> listAnnotationLayer(Project aProject)
    {
        return entityManager
                .createQuery("FROM AnnotationLayer WHERE project =:project ORDER BY uiName",
                        AnnotationLayer.class).setParameter("project", aProject).getResultList();
    }

    @Override
    @Transactional
    public List<AnnotationLayer> listAttachedRelationLayers(AnnotationLayer aLayer)
    {
        return entityManager
                .createQuery(
                        "SELECT l FROM AnnotationLayer l LEFT JOIN l.attachFeature f "
                        + "WHERE l.type = :type AND l.project = :project AND "
                        + "(l.attachType = :attachType OR f.type = :attachTypeName) "
                        + "ORDER BY l.uiName",
                        AnnotationLayer.class).setParameter("type", RELATION_TYPE)
                .setParameter("attachType", aLayer)
                .setParameter("attachTypeName", aLayer.getName())
                // Checking for project is necessary because type match is string-based
                .setParameter("project", aLayer.getProject()).getResultList();
    }

    @Override
    @Transactional
    public List<AnnotationFeature> listAttachedLinkFeatures(AnnotationLayer aLayer)
    {
        return entityManager
                .createQuery(
                        "FROM AnnotationFeature WHERE linkMode in (:modes) AND project = :project AND "
                                + "type in (:attachType) ORDER BY uiName", AnnotationFeature.class)
                .setParameter("modes", asList(LinkMode.SIMPLE, LinkMode.WITH_ROLE))
                .setParameter("attachType", asList(aLayer.getName(), CAS.TYPE_NAME_ANNOTATION))
                // Checking for project is necessary because type match is string-based
                .setParameter("project", aLayer.getProject()).getResultList();
    }

    @Override
    @Transactional
    public List<AnnotationFeature> listAnnotationFeature(AnnotationLayer aLayer)
    {
        if (isNull(aLayer) || isNull(aLayer.getId())) {
            return new ArrayList<>();
        }

        return entityManager
                .createQuery("FROM AnnotationFeature  WHERE layer =:layer ORDER BY uiName",
                        AnnotationFeature.class).setParameter("layer", aLayer).getResultList();
    }

    @Override
    @Transactional
    public List<AnnotationFeature> listAnnotationFeature(Project aProject)
    {
        return entityManager
                .createQuery(
                        "FROM AnnotationFeature f WHERE project =:project ORDER BY f.layer.uiName, f.uiName",
                        AnnotationFeature.class).setParameter("project", aProject).getResultList();
    }

    @Override
    @Transactional
    public List<Tag> listTags()
    {
        return entityManager.createQuery("From Tag ORDER BY name", Tag.class).getResultList();
    }

    @Override
    @Transactional
    public List<Tag> listTags(TagSet aTagSet)
    {
        return entityManager
                .createQuery("FROM Tag WHERE tagSet = :tagSet ORDER BY name ASC", Tag.class)
                .setParameter("tagSet", aTagSet).getResultList();
    }

    @Override
    @Transactional
    public List<TagSet> listTagSets()
    {
        return entityManager.createQuery("FROM TagSet ORDER BY name ASC", TagSet.class)
                .getResultList();
    }

    @Override
    @Transactional(noRollbackFor = NoResultException.class)
    public List<TagSet> listTagSets(Project aProject)
    {
        return entityManager
                .createQuery("FROM TagSet where project = :project ORDER BY name ASC", TagSet.class)
                .setParameter("project", aProject).getResultList();
    }

    @Override
    @Transactional
    public void removeTag(Tag aTag)
    {
        entityManager.remove(entityManager.contains(aTag) ? aTag : entityManager.merge(aTag));
    }

    @Override
    @Transactional
    public void removeTagSet(TagSet aTagSet)
    {
        for (Tag tag : listTags(aTagSet)) {
            entityManager.remove(tag);
        }
        entityManager
                .remove(entityManager.contains(aTagSet) ? aTagSet : entityManager.merge(aTagSet));
    }

    @Override
    @Transactional
    public void removeAnnotationFeature(AnnotationFeature aFeature)
    {
        entityManager.remove(
                entityManager.contains(aFeature) ? aFeature : entityManager.merge(aFeature));
    }

    @Override
    @Transactional
    public void removeAnnotationLayer(AnnotationLayer aLayer)
    {
        entityManager.remove(aLayer);
    }

    @Override
    @Transactional
    public void removeAllTags(TagSet aTagSet)
    {
        for (Tag tag : listTags(aTagSet)) {
            entityManager.remove(tag);
        }
    }

    @Override
    public TypeSystemDescription getProjectTypes(Project aProject)
    {
        // Create a new type system from scratch
        TypeSystemDescription tsd = new TypeSystemDescription_impl();
        for (AnnotationLayer type : listAnnotationLayer(aProject)) {
            if (type.getType().equals(SPAN_TYPE) && !type.isBuiltIn()) {
                TypeDescription td = tsd.addType(type.getName(), "", CAS.TYPE_NAME_ANNOTATION);
                
                generateFeatures(tsd, td, type);
            }
            else if (type.getType().equals(RELATION_TYPE) && !type.isBuiltIn()) {
                TypeDescription td = tsd.addType(type.getName(), "", CAS.TYPE_NAME_ANNOTATION);
                AnnotationLayer attachType = type.getAttachType();

                td.addFeature(WebAnnoConst.FEAT_REL_TARGET, "", attachType.getName());
                td.addFeature(WebAnnoConst.FEAT_REL_SOURCE, "", attachType.getName());

                generateFeatures(tsd, td, type);
            }
            else if (type.getType().equals(CHAIN_TYPE) && !type.isBuiltIn()) {
                TypeDescription tdChains = tsd.addType(type.getName() + "Chain", "",
                        CAS.TYPE_NAME_ANNOTATION_BASE);
                tdChains.addFeature("first", "", type.getName() + "Link");
                
                // Custom features on chain layers are currently not supported
                // generateFeatures(tsd, tdChains, type);
                
                TypeDescription tdLink = tsd.addType(type.getName() + "Link", "",
                        CAS.TYPE_NAME_ANNOTATION);
                tdLink.addFeature("next", "", type.getName() + "Link");
                tdLink.addFeature("referenceType", "", CAS.TYPE_NAME_STRING);
                tdLink.addFeature("referenceRelation", "", CAS.TYPE_NAME_STRING);
            }
        }

        return tsd;
    }
    
    private void generateFeatures(TypeSystemDescription aTSD, TypeDescription aTD,
            AnnotationLayer aLayer)
    {
        List<AnnotationFeature> features = listAnnotationFeature(aLayer);
        for (AnnotationFeature feature : features) {
            FeatureSupport fs = featureSupportRegistry.getFeatureSupport(feature);
            fs.generateFeature(aTSD, aTD, feature);
        }
    }

    @Override
    public void upgradeCas(CAS aCas, AnnotationDocument aAnnotationDocument)
        throws UIMAException, IOException
    {
        upgradeCas(aCas, aAnnotationDocument.getDocument(), aAnnotationDocument.getUser());
    }

    @Override
    public void upgradeCas(CAS aCas, SourceDocument aSourceDocument, String aUser)
        throws UIMAException, IOException
    {
        TypeSystemDescription ts = getFullProjectTypeSystem(aSourceDocument.getProject());

        upgradeCas(aCas, ts);

        try (MDC.MDCCloseable closable = MDC.putCloseable(
                Logging.KEY_PROJECT_ID,
                String.valueOf(aSourceDocument.getProject().getId()))) {
            Project project = aSourceDocument.getProject();
            log.info(
                    "Upgraded CAS of user [{}] for "
                            + "document [{}]({}) in project [{}]({})",
                    aUser, aSourceDocument.getName(), aSourceDocument.getId(), project.getName(),
                    project.getId());
        }
    }
    
    @Override
    public void upgradeCasIfRequired(CAS aCas, AnnotationDocument aAnnotationDocument)
        throws UIMAException, IOException
    {
        upgradeCasIfRequired(aCas, aAnnotationDocument.getDocument(),
                aAnnotationDocument.getUser());
    }
    
    @Override
    public void upgradeCasIfRequired(CAS aCas, SourceDocument aSourceDocument, String aUser)
        throws UIMAException, IOException
    {
        TypeSystemDescription ts = getFullProjectTypeSystem(aSourceDocument.getProject());
        
        // Check if the current CAS already contains the required type system
        if (!isUpgradeRequired(aCas, ts)) {
            log.debug(
                    "CAS of user [{}] for document [{}]({}) in project [{}]({}) is already "
                            + "compatible with project type system - skipping upgrade",
                    aUser, aSourceDocument.getName(), aSourceDocument.getId(),
                    aSourceDocument.getProject().getName(), aSourceDocument.getProject().getId());
            return;
        }

        upgradeCas(aCas, ts);
    }
    
    private void upgradeCas(CAS aCas, TypeSystemDescription aTargetTypeSystem)
        throws UIMAException, IOException
    {
        // Prepare template for new CAS
        CAS newCas = JCasFactory.createJCas(aTargetTypeSystem).getCas();
        CASCompleteSerializer serializer = Serialization.serializeCASComplete((CASImpl) newCas);

        // Save old type system
        TypeSystem oldTypeSystem = aCas.getTypeSystem();

        // Save old CAS contents
        ByteArrayOutputStream os2 = new ByteArrayOutputStream();
        Serialization.serializeWithCompression(aCas, os2, oldTypeSystem);

        // Prepare CAS with new type system
        Serialization.deserializeCASComplete(serializer, (CASImpl) aCas);

        // Restore CAS data to new type system
        Serialization.deserializeCAS(aCas, new ByteArrayInputStream(os2.toByteArray()),
                oldTypeSystem, null);

        // Make sure JCas is properly initialized too
        aCas.getJCas();
    }
    
    private TypeSystemDescription getFullProjectTypeSystem(Project aProject)
        throws ResourceInitializationException
    {
        TypeSystemDescription builtInTypes = TypeSystemDescriptionFactory
                .createTypeSystemDescription();
        TypeSystemDescription projectTypes = getProjectTypes(aProject);
        TypeSystemDescription_impl allTypes = (TypeSystemDescription_impl) CasCreationUtils
                .mergeTypeSystems(asList(projectTypes, builtInTypes));
        return allTypes;
    }
    
    /**
     * Check if the current CAS already contains the required type system.
     */
    private boolean isUpgradeRequired(CAS aCas, TypeSystemDescription aTargetTypeSystem)
    {
        TypeSystem ts = aCas.getTypeSystem();
        boolean upgradeRequired = false;
        nextType: for (TypeDescription tdesc : aTargetTypeSystem.getTypes()) {
            Type t = ts.getType(tdesc.getName());
            
            // Type does not exist
            if (t == null) {
                log.info("CAS update required: type {} does not exist", tdesc.getName());
                upgradeRequired = true;
                break nextType;
            }
            
            // Super-type does not match
            if (!Objects.equals(tdesc.getSupertypeName(), ts.getParent(t).getName())) {
                log.info("CAS update required: supertypes of {} do not match: {} <-> {}",
                        tdesc.getName(), tdesc.getSupertypeName(), ts.getParent(t).getName());
                upgradeRequired = true;
                break nextType;
            }
            
            // Check features
            for (FeatureDescription fdesc : tdesc.getFeatures()) {
                Feature f = t.getFeatureByBaseName(fdesc.getName());
                
                // Feature does not exist
                if (f == null) {
                    log.info("CAS update required: feature {} on type {} does not exist",
                            fdesc.getName(), tdesc.getName());
                    upgradeRequired = true;
                    break nextType;
                }
                
                // Range does not match
                if (CAS.TYPE_NAME_FS_ARRAY.equals(fdesc.getRangeTypeName())) {
                    if (!Objects.equals(fdesc.getElementType(),
                            f.getRange().getComponentType().getName())) {
                        log.info(
                                "CAS update required: ranges of feature {} on type {} do not match: {} <-> {}",
                                fdesc.getName(), tdesc.getName(), fdesc.getRangeTypeName(),
                                f.getRange().getName());
                        upgradeRequired = true;
                        break nextType;
                    }
                }
                else {
                    if (!Objects.equals(fdesc.getRangeTypeName(), f.getRange().getName())) {
                        log.info(
                                "CAS update required: ranges of feature {} on type {} do not match: {} <-> {}",
                                fdesc.getName(), tdesc.getName(), fdesc.getRangeTypeName(),
                                f.getRange().getName());
                        upgradeRequired = true;
                        break nextType;
                    }
                }
            }
        }
        
        return upgradeRequired;
    }

    @Override
    @Transactional
    public TypeAdapter getAdapter(AnnotationLayer aLayer)
    {
        return layerSupportRegistry.getLayerSupport(aLayer).createAdapter(aLayer);
    }
}
