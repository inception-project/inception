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

import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.RELATION_TYPE;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.createCas;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.isNativeUimaType;
import static java.util.Arrays.asList;
import static java.util.Objects.isNull;
import static org.apache.uima.fit.factory.TypeSystemDescriptionFactory.createTypeSystemDescription;
import static org.apache.uima.util.CasCreationUtils.mergeTypeSystems;

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
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.FeatureDescription;
import org.apache.uima.resource.metadata.TypeDescription;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.resource.metadata.impl.TypeSystemDescription_impl;
import org.apache.uima.util.CasCreationUtils;
import org.apache.uima.util.CasIOUtils;
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
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.layer.LayerSupport;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.layer.LayerSupportRegistry;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.TypeSystemAnalysis;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.TypeSystemAnalysis.RelationDetails;
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
        String query = String.join("\n",
                "FROM AnnotationLayer",
                "WHERE id = :id");
        
        return entityManager.createQuery(query, AnnotationLayer.class)
                .setParameter("id", aId)
                .getSingleResult();
    }

    @Override
    @Transactional(noRollbackFor = NoResultException.class)
    public Optional<AnnotationLayer> getLayer(Project aProject, long aLayerId)
    {
        String query = String.join("\n",
                "FROM AnnotationLayer",
                "WHERE id = :id",
                "AND project = :project");
        
        return entityManager.createQuery(query, AnnotationLayer.class)
                .setParameter("id", aLayerId)
                .setParameter("project", aProject)
                .getResultStream().findFirst();
    }
    
    @Override
    @Transactional(noRollbackFor = NoResultException.class)
    public AnnotationLayer findLayer(Project aProject, String aName)
    {
        // If there is a layer definition for the given name, then return it immediately
        Optional<AnnotationLayer> layer = getLayerInternal(aName, aProject);
        if (layer.isPresent()) {
            return layer.get();
        }
        
        TypeSystemDescription tsd;
        try {
            tsd = getFullProjectTypeSystem(aProject);
        }
        catch (ResourceInitializationException e) {
            throw new RuntimeException(e);
        }

        TypeDescription type = tsd.getType(aName);
        // If the super-type is not covered by the type system, then it is most likely a
        // UIMA built-in type. In this case we can stop the search since we do not have
        // layer definitions for UIMA built-in types.
        if (type == null) {
            throw new NoResultException("Type [" + aName + "] not found in the type system");
        }
        
        // If there is no layer definition for the given type name, try using the type system
        // definition to determine a suitable layer definition for a super type of the given type.
        while (true) {
            // If there is no super type, then we cannot find a suitable layer definition
            if (type.getSupertypeName() == null) {
                throw new NoResultException(
                        "No more super-types - no suitable layer definition found for type ["
                                + aName + "]");
            }

            // If there is a super-type then see if there is layer definition for it
            type = tsd.getType(type.getSupertypeName());

            // If the super-type is not covered by the type system, then it is most likely a
            // UIMA built-in type. In this case we can stop the search since we do not have
            // layer definitions for UIMA built-in types.
            if (type == null) {
                throw new NoResultException(
                        "Super-type not in type system - no suitable layer definition found for type ["
                                + aName + "]");
            }

            layer = getLayerInternal(type.getName(), aProject);
            
            // If the a layer definition of the given type was found, return it
            if (layer.isPresent()) {
                return layer.get();
            }
            
            // Otherwise attempt going one level higher in the inheritance hierarchy
        }
    }
    
    private Optional<AnnotationLayer> getLayerInternal(String aName, Project aProject)
    {
        String query = String.join("\n",
                "FROM AnnotationLayer ",
                "WHERE name = :name AND project = :project");
        
        return entityManager.createQuery(query, AnnotationLayer.class)
                .setParameter("name", aName)
                .setParameter("project", aProject)
                .getResultStream().findFirst();
    }
    
    @Override
    @Transactional(noRollbackFor = NoResultException.class)
    public AnnotationLayer findLayer(Project aProject, FeatureStructure aFS)
    {
        String layerName = aFS.getType().getName();
        AnnotationLayer layer;
        try {
            layer = findLayer(aProject, layerName);
        }
        catch (NoResultException e) {
            if (layerName.endsWith("Chain")) {
                layerName = layerName.substring(0, layerName.length() - 5);
            }
            if (layerName.endsWith("Link")) {
                layerName = layerName.substring(0, layerName.length() - 4);
            }
            layer = findLayer(aProject, layerName);
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
    public TypeSystemDescription getCustomProjectTypes(Project aProject)
    {
        // Create a new type system from scratch
        TypeSystemDescription tsd = new TypeSystemDescription_impl();

        listAnnotationLayer(aProject).stream()
                .filter(layer -> !layer.isBuiltIn())
                .forEachOrdered(layer -> layerSupportRegistry.getLayerSupport(layer)
                        .generateTypes(tsd, layer));

        return tsd;
    }

    @Override
    public TypeSystemDescription getAllProjectTypes(Project aProject)
        throws ResourceInitializationException
    {
        // Create a new type system from scratch
        TypeSystemDescription tsd = new TypeSystemDescription_impl();

        TypeSystemDescription builtInTypes = createTypeSystemDescription();
        
        List<AnnotationLayer> layers = listAnnotationLayer(aProject);
        
        for (AnnotationLayer layer : layers) {
            LayerSupport<?> layerSupport = layerSupportRegistry.getLayerSupport(layer);
            
            // for built-in layers, we clone the information from the built-in type descriptors
            if (layer.isBuiltIn()) {
                for (String typeName : layerSupport.getGeneratedTypeNames(layer)) {
                    exportBuiltInTypeDescription(builtInTypes, tsd, typeName);
                }
            }
            // for custom layers, we use the information from the project settings
            else {
                layerSupport.generateTypes(tsd, layer);
            }
        }

        return tsd;
    }
    
    private void exportBuiltInTypeDescription(TypeSystemDescription aSource,
            TypeSystemDescription aTarget, String aType)
    {
        TypeDescription builtInType = aSource.getType(aType);
        
        if (builtInType == null) {
            throw new IllegalArgumentException(
                    "No type description found for type [" + aType + "]");
        }
        
        TypeDescription clonedType = aTarget.addType(builtInType.getName(),
                builtInType.getDescription(), builtInType.getSupertypeName());
        
        if (builtInType.getFeatures() != null) {
            for (FeatureDescription feature : builtInType.getFeatures()) {
                clonedType.addFeature(feature.getName(), feature.getDescription(),
                        feature.getRangeTypeName(), feature.getElementType(),
                        feature.getMultipleReferencesAllowed());
                
                // Export types referenced by built-in types also as built-in types. Note that
                // it is conceptually impossible for built-in types to refer to custom types, so
                // this is cannot lead to a custom type being exported as a built-in type.
                if (
                        feature.getElementType() != null && 
                        !isNativeUimaType(feature.getElementType()) &&
                        aTarget.getType(feature.getElementType()) == null
                ) {
                    exportBuiltInTypeDescription(aSource, aTarget, feature.getElementType());
                }
                else if (
                        feature.getRangeTypeName() != null && 
                        !isNativeUimaType(feature.getRangeTypeName()) &&
                        aTarget.getType(feature.getRangeTypeName()) == null
                ) {
                    exportBuiltInTypeDescription(aSource, aTarget, feature.getRangeTypeName());
                }
            }
        }
    }

    @Override
    public TypeSystemDescription getFullProjectTypeSystem(Project aProject)
        throws ResourceInitializationException
    {
        return getFullProjectTypeSystem(aProject, true);
    }

    @Override
    public TypeSystemDescription getFullProjectTypeSystem(Project aProject,
            boolean aIncludeInternalTypes)
        throws ResourceInitializationException
    {
        List<TypeSystemDescription> typeSystems = new ArrayList<>();
        
        // Types detected by uimaFIT
        typeSystems.add(createTypeSystemDescription());
        
        if (aIncludeInternalTypes) {
            // Types internally used by WebAnno (which we intentionally exclude from being detected
            // by uimaFIT because we want to have an easy way to create a type system excluding
            // these types when we export files from the project
            typeSystems.add(CasMetadataUtils.getInternalTypeSystem());
        }

        // Types declared within the project
        typeSystems.add(getCustomProjectTypes(aProject));

        return (TypeSystemDescription_impl) mergeTypeSystems(typeSystems);
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
        upgradeCas(aCas, aSourceDocument.getProject());
        
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
    public void upgradeCas(CAS aCas, Project aProject) throws UIMAException, IOException
    {
        TypeSystemDescription ts = getFullProjectTypeSystem(aProject);
        upgradeCas(aCas, ts);
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
    
    @Override
    public CAS prepareCasForExport(CAS aCas, SourceDocument aSourceDocument)
        throws ResourceInitializationException, UIMAException, IOException
    {
        CAS exportCas = CasCreationUtils.createCas((TypeSystemDescription) null, null, null);
        upgradeCas(aCas, exportCas, getFullProjectTypeSystem(aSourceDocument.getProject(), false));
        return exportCas;
    }
    
    /**
     * In-place upgrade of the given CAS to the target type system.
     */
    private void upgradeCas(CAS aCas, TypeSystemDescription aTargetTypeSystem)
        throws UIMAException, IOException
    {
        upgradeCas(aCas, aCas, aTargetTypeSystem);
    }

    /**
     * Load the contents from the source CAS, upgrade it to the target type system and write the
     * results to the target CAS. An in-place upgrade can be achieved by using the same CAS as
     * source and target.
     */
    private void upgradeCas(CAS aSourceCas, CAS aTargetCas, TypeSystemDescription aTargetTypeSystem)
        throws UIMAException, IOException
    {
        // Save source CAS type system (do this early since we might do an in-place upgrade)
        TypeSystem sourceTypeSystem = aSourceCas.getTypeSystem();

        // Save source CAS contents
        ByteArrayOutputStream serializedCasContents = new ByteArrayOutputStream();
        Serialization.serializeWithCompression(aSourceCas, serializedCasContents, sourceTypeSystem);

        // Re-initialize the target CAS with new type system
        CAS tempCas = createCas(aTargetTypeSystem);
        CASCompleteSerializer serializer = Serialization.serializeCASComplete((CASImpl) tempCas);
        Serialization.deserializeCASComplete(serializer, (CASImpl) aTargetCas);

        // Leniently load the source CAS contents into the target CAS
        CasIOUtils.load(new ByteArrayInputStream(serializedCasContents.toByteArray()), aTargetCas,
                sourceTypeSystem);
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
    
    @Override
    @Transactional
    public void importUimaTypeSystem(Project aProject, TypeSystemDescription aTSD)
        throws ResourceInitializationException
    {
        TypeSystemDescription builtInTypes = createTypeSystemDescription();
        
        TypeSystemAnalysis analysis = TypeSystemAnalysis.of(aTSD);
        for (AnnotationLayer l : analysis.getLayers()) {
            // Modifications/imports of built-in layers are not supported
            if (builtInTypes.getType(l.getName()) != null) {
                continue;
            }
            
            // If a custom layer does not exist yet, create it
            if (!existsLayer(l.getName(), aProject)) {
                l.setProject(aProject);

                // Need to set the attach type
                if (WebAnnoConst.RELATION_TYPE.equals(l.getType())) {
                    RelationDetails relDetails = analysis.getRelationDetails(l.getName());

                    AnnotationLayer attachLayer;
                    try {
                        // First check if this type is already in the project
                        attachLayer = findLayer(aProject,
                                relDetails.getAttachLayer());
                    }
                    catch (NoResultException e) {
                        // If it does not exist in the project yet, then we create it
                        attachLayer = analysis.getLayer(relDetails.getAttachLayer());
                        attachLayer.setProject(aProject);
                        createLayer(attachLayer);
                    }

                    l.setAttachType(attachLayer);
                }

                createLayer(l);
            }

            // Import the features for the layer except if the layer is a built-in layer.
            // We must not touch the built-in layers because WebAnno may rely on their
            // structure. This is a conservative measure for now any may be relaxed in the
            // future.
            AnnotationLayer persistedLayer = findLayer(aProject, l.getName());
            if (!persistedLayer.isBuiltIn()) {
                for (AnnotationFeature f : analysis.getFeatures(l.getName())) {
                    if (!existsFeature(f.getName(), persistedLayer)) {
                        f.setProject(aProject);
                        f.setLayer(persistedLayer);
                        createFeature(f);
                    }
                }
            }
        }
    }
}
