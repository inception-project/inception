/*
 * Licensed to the Technische Universität Darmstadt under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The Technische Universität Darmstadt 
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.
 *  
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.clarin.webanno.api.dao;

import static de.tudarmstadt.ukp.clarin.webanno.api.AttachedAnnotation.Direction.INCOMING;
import static de.tudarmstadt.ukp.clarin.webanno.api.AttachedAnnotation.Direction.LOOP;
import static de.tudarmstadt.ukp.clarin.webanno.api.AttachedAnnotation.Direction.OUTGOING;
import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.RELATION_TYPE;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.getAddr;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.getRealCas;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.isNativeUimaType;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.isSame;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.selectByAddr;
import static java.util.Arrays.asList;
import static java.util.Objects.isNull;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.stream.Collectors.toList;
import static org.apache.uima.cas.impl.Serialization.deserializeCASComplete;
import static org.apache.uima.cas.impl.Serialization.serializeCASComplete;
import static org.apache.uima.cas.impl.Serialization.serializeWithCompression;
import static org.apache.uima.fit.factory.TypeSystemDescriptionFactory.createTypeSystemDescription;
import static org.apache.uima.util.CasCreationUtils.mergeTypeSystems;
import static org.hibernate.annotations.QueryHints.CACHEABLE;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;

import org.apache.uima.UIMAException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.cas.impl.CASCompleteSerializer;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.factory.CasFactory;
import org.apache.uima.fit.util.CasUtil;
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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.AttachedAnnotation;
import de.tudarmstadt.ukp.clarin.webanno.api.CasUpgradeMode;
import de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.adapter.RelationAdapter;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.adapter.SpanAdapter;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.adapter.TypeAdapter;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.layer.LayerSupport;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.layer.LayerSupportRegistry;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.LinkWithRoleModel;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.TypeSystemAnalysis;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.TypeSystemAnalysis.RelationDetails;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.casstorage.CasStorageSession;
import de.tudarmstadt.ukp.clarin.webanno.api.event.TagCreatedEvent;
import de.tudarmstadt.ukp.clarin.webanno.api.event.TagDeletedEvent;
import de.tudarmstadt.ukp.clarin.webanno.api.event.TagUpdatedEvent;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.ImmutableTag;
import de.tudarmstadt.ukp.clarin.webanno.model.LinkMode;
import de.tudarmstadt.ukp.clarin.webanno.model.MultiValueMode;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.ReorderableTag;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.Tag;
import de.tudarmstadt.ukp.clarin.webanno.model.TagSet;
import de.tudarmstadt.ukp.clarin.webanno.support.logging.Logging;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;

/**
 * Implementation of methods defined in the {@link AnnotationSchemaService} interface
 */
@Component(AnnotationSchemaService.SERVICE_NAME)
public class AnnotationSchemaServiceImpl
    implements AnnotationSchemaService
{
    private final Logger log = LoggerFactory.getLogger(getClass());

    private @PersistenceContext EntityManager entityManager;

    private final ApplicationEventPublisher applicationEventPublisher;
    private final LayerSupportRegistry layerSupportRegistry;
    private final FeatureSupportRegistry featureSupportRegistry;
    private final LoadingCache<TagSet, List<ImmutableTag>> immutableTagsCache;
    private final TypeSystemDescription builtInTypes;

    @Autowired
    public AnnotationSchemaServiceImpl(LayerSupportRegistry aLayerSupportRegistry,
            FeatureSupportRegistry aFeatureSupportRegistry,
            ApplicationEventPublisher aApplicationEventPublisher)
    {
        layerSupportRegistry = aLayerSupportRegistry;
        featureSupportRegistry = aFeatureSupportRegistry;
        applicationEventPublisher = aApplicationEventPublisher;

        immutableTagsCache = Caffeine.newBuilder().expireAfterAccess(5, MINUTES)
                .maximumSize(10 * 1024).build(this::loadImmutableTags);

        try {
            builtInTypes = createTypeSystemDescription();
        }
        catch (ResourceInitializationException e) {
            throw new IllegalStateException("Unable to initialize built-in type system", e);
        }
    }

    public AnnotationSchemaServiceImpl()
    {
        this(null, null, (EntityManager) null);
    }

    public AnnotationSchemaServiceImpl(LayerSupportRegistry aLayerSupportRegistry,
            FeatureSupportRegistry aFeatureSupportRegistry, EntityManager aEntityManager)
    {
        this(aLayerSupportRegistry, aFeatureSupportRegistry, (ApplicationEventPublisher) null);
        entityManager = aEntityManager;
    }

    @Override
    @Transactional
    public void createTag(Tag aTag)
    {
        boolean created = createTagNoLog(aTag);

        flushImmutableTagCache(aTag.getTagSet());

        try (MDC.MDCCloseable closable = MDC.putCloseable(Logging.KEY_PROJECT_ID,
                String.valueOf(aTag.getTagSet().getProject().getId()))) {
            TagSet tagset = aTag.getTagSet();
            Project project = tagset.getProject();
            log.info("{} tag [{}]({}) in tagset [{}]({}) in project [{}]({})",
                    created ? "Created" : "Updated", aTag.getName(), aTag.getId(), tagset.getName(),
                    tagset.getId(), project.getName(), project.getId());
        }
    }

    @Override
    @Transactional
    public void createTags(Tag... aTags)
    {
        if (aTags == null || aTags.length == 0) {
            return;
        }

        TagSet tagset = aTags[0].getTagSet();
        Project project = tagset.getProject();

        int createdCount = 0;
        int updatedCount = 0;
        for (Tag tag : aTags) {
            boolean created = createTagNoLog(tag);
            if (created) {
                createdCount++;
            }
            else {
                updatedCount++;
            }
        }

        try (MDC.MDCCloseable closable = MDC.putCloseable(Logging.KEY_PROJECT_ID,
                String.valueOf(project.getId()))) {
            log.info("Created {} tags and updated {} tags in tagset [{}]({}) in project [{}]({})",
                    createdCount, updatedCount, tagset.getName(), tagset.getId(), project.getName(),
                    project.getId());
        }
    }

    private boolean createTagNoLog(Tag aTag)
    {
        if (isNull(aTag.getId())) {
            entityManager.persist(aTag);

            if (applicationEventPublisher != null) {
                applicationEventPublisher.publishEvent(new TagCreatedEvent(this, aTag));
            }

            return true;
        }
        else {
            entityManager.merge(aTag);

            if (applicationEventPublisher != null) {
                applicationEventPublisher.publishEvent(new TagUpdatedEvent(this, aTag));
            }

            return false;
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
    public void createOrUpdateLayer(AnnotationLayer aLayer)
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
            log.info("Created layer [{}]({}) in project [{}]({})", aLayer.getName(), aLayer.getId(),
                    project.getName(), project.getId());
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

        try (MDC.MDCCloseable closable = MDC.putCloseable(Logging.KEY_PROJECT_ID,
                String.valueOf(aFeature.getProject().getId()))) {
            Project project = aFeature.getProject();
            log.info("Created feature [{}]({}) in project [{}]({})", aFeature.getName(),
                    aFeature.getId(), project.getName(), project.getId());
        }
    }

    @Override
    @Transactional
    public Optional<Tag> getTag(long aId)
    {
        return Optional.ofNullable(entityManager.find(Tag.class, aId));
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
                            TagSet.class)
                    .setParameter("name", aName).setParameter("project", aProject)
                    .getSingleResult();
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
                    .createQuery("FROM AnnotationLayer WHERE name = :name AND project = :project",
                            AnnotationLayer.class)
                    .setParameter("name", aName).setParameter("project", aProject)
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
            entityManager.createQuery(
                    "FROM AnnotationLayer WHERE name = :name AND type = :type AND project = :project",
                    AnnotationLayer.class).setParameter("name", aName).setParameter("type", aType)
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
                            AnnotationFeature.class)
                    .setParameter("name", aName) //
                    .setParameter("layer", aLayer) //
                    .getSingleResult();
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
                .setParameter("name", aName) //
                .setParameter("project", aProject) //
                .getSingleResult();
    }

    @Override
    @Transactional
    public TagSet getTagSet(long aId)
    {
        return entityManager.find(TagSet.class, aId);
    }

    @Override
    @Transactional
    public AnnotationLayer getLayer(long aId)
    {
        return entityManager.find(AnnotationLayer.class, aId);
    }

    @Override
    @Transactional(noRollbackFor = NoResultException.class)
    public Optional<AnnotationLayer> getLayer(Project aProject, long aLayerId)
    {
        AnnotationLayer layer = getLayer(aLayerId);

        // Check that the layer actually belongs to the project
        if (layer != null && !Objects.equals(layer.getProject().getId(), aProject.getId())) {
            return Optional.empty();
        }

        return Optional.ofNullable(layer);
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
        String query = String.join("\n", //
                "FROM AnnotationLayer ", //
                "WHERE name = :name AND project = :project");

        return entityManager.createQuery(query, AnnotationLayer.class) //
                .setParameter("name", aName) //
                .setParameter("project", aProject) //
                .setHint(CACHEABLE, true) //
                .getResultStream() //
                .findFirst();
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
                        AnnotationFeature.class)
                .setParameter("name", aName).setParameter("layer", aLayer).getSingleResult();
    }

    @Override
    @Transactional(noRollbackFor = NoResultException.class)
    public boolean existsType(String aName, String aType)
    {
        try {
            entityManager
                    .createQuery("From AnnotationLayer where name = :name AND type = :type",
                            AnnotationLayer.class)
                    .setParameter("name", aName).setParameter("type", aType).getSingleResult();
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

        int createdCount = 0;
        int updatedCount = 0;
        int i = 0;
        for (String tagName : aTags) {
            Tag tag = new Tag();
            tag.setTagSet(tagSet);
            tag.setDescription(aTagDescription[i]);
            tag.setName(tagName);
            boolean created = createTagNoLog(tag);
            if (created) {
                createdCount++;
            }
            else {
                updatedCount++;
            }
            i++;
        }

        try (MDC.MDCCloseable closable = MDC.putCloseable(Logging.KEY_PROJECT_ID,
                String.valueOf(aProject.getId()))) {
            log.info("Created {} tags and updated {} tags in tagset [{}]({}) in project [{}]({})",
                    createdCount, updatedCount, tagSet.getName(), tagSet.getId(),
                    aProject.getName(), aProject.getId());
        }

        return tagSet;
    }

    @Override
    @Transactional
    public List<AnnotationLayer> listAnnotationLayer(Project aProject)
    {
        String query = String.join("\n", //
                "FROM AnnotationLayer", //
                "WHERE project = :project ", //
                "ORDER BY uiName");

        return entityManager.createQuery(query, AnnotationLayer.class)
                .setParameter("project", aProject) //
                .setHint(CACHEABLE, true) //
                .getResultList();
    }

    @Override
    @Transactional
    public List<AnnotationLayer> listAttachedRelationLayers(AnnotationLayer aLayer)
    {
        String query = String.join("\n",
                "SELECT l FROM AnnotationLayer l LEFT JOIN l.attachFeature f ", //
                "WHERE l.type        = :type AND ", //
                "      l.project     = :project AND ", //
                "      (l.attachType = :attachType OR f.type = :attachTypeName) ", //
                "ORDER BY l.uiName");

        return entityManager.createQuery(query, AnnotationLayer.class)
                .setParameter("type", RELATION_TYPE) //
                .setParameter("attachType", aLayer) //
                .setParameter("attachTypeName", aLayer.getName())
                // Checking for project is necessary because type match is string-based
                .setParameter("project", aLayer.getProject()) //
                .setHint(CACHEABLE, true) //
                .getResultList();
    }

    @Transactional
    public List<AnnotationFeature> listAttachingFeatures(AnnotationLayer aLayer)
    {
        String query = String.join("\n", //
                "FROM AnnotationFeature ", //
                "WHERE type    = :type AND", //
                "      project = :project", //
                "ORDER BY uiName");

        // This should not be cached because we do not have a proper foreign key relation to
        // the type.
        return entityManager.createQuery(query, AnnotationFeature.class)
                .setParameter("type", aLayer.getName()) //
                // Checking for project is necessary because type match is string-based
                .setParameter("project", aLayer.getProject()) //
                .getResultList();
    }

    @Override
    @Transactional
    public List<AnnotationFeature> listAttachedLinkFeatures(AnnotationLayer aLayer)
    {
        String query = String.join("\n", //
                "FROM AnnotationFeature", //
                "WHERE linkMode in (:modes) AND ", //
                "      project = :project AND ", //
                "      type in (:attachType) ", //
                "ORDER BY uiName");

        return entityManager.createQuery(query, AnnotationFeature.class)
                .setParameter("modes", asList(LinkMode.SIMPLE, LinkMode.WITH_ROLE))
                .setParameter("attachType", asList(aLayer.getName(), CAS.TYPE_NAME_ANNOTATION))
                // Checking for project is necessary because type match is string-based
                .setParameter("project", aLayer.getProject()) //
                .setHint(CACHEABLE, true) //
                .getResultList();
    }

    @Override
    @Transactional
    public List<AnnotationFeature> listAnnotationFeature(AnnotationLayer aLayer)
    {
        if (isNull(aLayer) || isNull(aLayer.getId())) {
            return new ArrayList<>();
        }

        String query = String.join("\n", //
                "FROM AnnotationFeature", //
                "WHERE layer = :layer", //
                "ORDER BY uiName");

        return entityManager.createQuery(query, AnnotationFeature.class)
                .setParameter("layer", aLayer) //
                .setHint(CACHEABLE, true) //
                .getResultList();
    }

    @Override
    @Transactional
    public List<AnnotationFeature> listEnabledFeatures(AnnotationLayer aLayer)
    {
        if (isNull(aLayer) || isNull(aLayer.getId())) {
            return new ArrayList<>();
        }

        String query = String.join("\n", //
                "FROM AnnotationFeature", //
                "WHERE layer = :layer", //
                "AND enabled = true", //
                "ORDER BY uiName"); //

        return entityManager.createQuery(query, AnnotationFeature.class)
                .setParameter("layer", aLayer) //
                .setHint(CACHEABLE, true) //
                .getResultList();
    }

    @Override
    @Transactional
    public List<AnnotationFeature> listAnnotationFeature(Project aProject)
    {
        return entityManager.createQuery(
                "FROM AnnotationFeature f WHERE project =:project ORDER BY f.layer.uiName, f.uiName",
                AnnotationFeature.class) //
                .setParameter("project", aProject) //
                .setHint(CACHEABLE, true) //
                .getResultList();
    }

    @Override
    @Transactional
    public List<Tag> listTags(TagSet aTagSet)
    {
        return entityManager
                .createQuery("FROM Tag WHERE tagSet = :tagSet ORDER BY name ASC", Tag.class)
                .setParameter("tagSet", aTagSet) //
                .getResultList();
    }

    private List<ImmutableTag> loadImmutableTags(TagSet aTagSet)
    {
        return listTags(aTagSet).stream().map(ImmutableTag::new).collect(toList());
    }

    private void flushImmutableTagCache(TagSet aTagSet)
    {
        immutableTagsCache.asMap().keySet()
                .removeIf(key -> Objects.equals(key.getId(), aTagSet.getId()));
    }

    @Override
    public List<ImmutableTag> listTagsImmutable(TagSet aTagSet)
    {
        if (aTagSet == null) {
            return Collections.emptyList();
        }

        return immutableTagsCache.get(aTagSet);
    }

    @Override
    @Transactional
    public List<ReorderableTag> listTagsReorderable(TagSet aTagSet)
    {
        return listTagsImmutable(aTagSet).stream().map(ReorderableTag::new).collect(toList());
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

        flushImmutableTagCache(aTag.getTagSet());

        if (applicationEventPublisher != null) {
            applicationEventPublisher.publishEvent(new TagDeletedEvent(this, aTag));
        }
    }

    @Override
    @Transactional
    public void removeTagSet(TagSet aTagSet)
    {
        // FIXME: Optimally, this should be cascade-on-delete in the DB
        for (Tag tag : listTags(aTagSet)) {
            entityManager.remove(tag);
        }

        flushImmutableTagCache(aTagSet);

        entityManager
                .remove(entityManager.contains(aTagSet) ? aTagSet : entityManager.merge(aTagSet));
    }

    @Override
    @Transactional
    public void removeFeature(AnnotationFeature aFeature)
    {
        entityManager.remove(
                entityManager.contains(aFeature) ? aFeature : entityManager.merge(aFeature));

        try (MDC.MDCCloseable closable = MDC.putCloseable(Logging.KEY_PROJECT_ID,
                String.valueOf(aFeature.getProject().getId()))) {
            Project project = aFeature.getProject();
            log.info("Removed feature [{}]({}) from project [{}]({})", aFeature.getName(),
                    aFeature.getId(), project.getName(), project.getId());
        }
    }

    @Override
    @Transactional
    public void removeLayer(AnnotationLayer aLayer)
    {
        AnnotationLayer layer = entityManager.contains(aLayer) ? aLayer
                : entityManager.merge(aLayer);

        // We must not rely on the DB-level CASCADE ON DELETE if Hibernate 2nd-level caching is
        // enabled because if we do, then Hibernate will not know that entries have gone from the
        // DB, will still try to re-hydrate them from the cache and will fail filling in gaps
        // from the DB. So we delete explicitly through Hibernate
        listAnnotationFeature(aLayer).forEach(this::removeFeature);

        // Remove all features in other layers that connect to the layer to be removed. This is
        // necessary so Hibernate cache knows they are gone and also because the relation is
        // modeled by name, so we couldn't use a DB-level CASCADE ON DELETE anyway.
        // It is also necessary to that e.g. the "pos" feature in the built-in "Token" layer gets
        // cleaned up - which the user could never do manually.
        listAttachingFeatures(aLayer).forEach(this::removeFeature);

        entityManager.remove(layer);

        try (MDC.MDCCloseable closable = MDC.putCloseable(Logging.KEY_PROJECT_ID,
                String.valueOf(aLayer.getProject().getId()))) {
            Project project = aLayer.getProject();
            log.info("Removed layer [{}]({}) from project [{}]({})", aLayer.getName(),
                    aLayer.getId(), project.getName(), project.getId());
        }
    }

    @Override
    @Transactional
    public void removeAllTags(TagSet aTagSet)
    {
        for (Tag tag : listTags(aTagSet)) {
            entityManager.remove(tag);
        }

        flushImmutableTagCache(aTagSet);
    }

    @Override
    @Transactional
    public List<AnnotationLayer> listSupportedLayers(Project aProject)
    {
        List<AnnotationLayer> supportedLayers = new ArrayList<>();

        for (AnnotationLayer l : listAnnotationLayer(aProject)) {
            try {
                layerSupportRegistry.getLayerSupport(l);
            }
            catch (IllegalArgumentException e) {
                // Skip unsupported layers
                continue;
            }

            // Add supported layers to the result
            supportedLayers.add(l);
        }

        return supportedLayers;

    }

    @Override
    @Transactional
    public List<AnnotationFeature> listSupportedFeatures(Project aProject)
    {
        List<AnnotationFeature> supportedFeatures = new ArrayList<>();

        for (AnnotationFeature f : listAnnotationFeature(aProject)) {
            try {
                featureSupportRegistry.findExtension(f);
            }
            catch (IllegalArgumentException e) {
                // Skip unsupported features
                continue;
            }

            // Add supported features to the result
            supportedFeatures.add(f);
        }

        return supportedFeatures;
    }

    @Override
    @Transactional
    public List<AnnotationFeature> listSupportedFeatures(AnnotationLayer aLayer)
    {
        List<AnnotationFeature> supportedFeatures = new ArrayList<>();

        for (AnnotationFeature f : listAnnotationFeature(aLayer)) {
            try {
                featureSupportRegistry.findExtension(f);
            }
            catch (IllegalArgumentException e) {
                // Skip unsupported features
                continue;
            }

            // Add supported features to the result
            supportedFeatures.add(f);
        }

        return supportedFeatures;
    }

    @Override
    public TypeSystemDescription getCustomProjectTypes(Project aProject)
    {
        // Create a new type system from scratch
        TypeSystemDescription tsd = new TypeSystemDescription_impl();

        List<AnnotationFeature> allFeaturesInProject = listSupportedFeatures(aProject);

        listSupportedLayers(aProject).stream().filter(layer -> !layer.isBuiltIn())
                .forEachOrdered(layer -> layerSupportRegistry.getLayerSupport(layer)
                        .generateTypes(tsd, layer, allFeaturesInProject));

        return tsd;
    }

    @Override
    public TypeSystemDescription getAllProjectTypes(Project aProject)
        throws ResourceInitializationException
    {
        List<AnnotationLayer> allLayersInProject = listSupportedLayers(aProject);
        List<AnnotationFeature> allFeaturesInProject = listSupportedFeatures(aProject);

        List<TypeSystemDescription> allTsds = new ArrayList<>();
        for (AnnotationLayer layer : allLayersInProject) {
            LayerSupport<?, ?> layerSupport = layerSupportRegistry.getLayerSupport(layer);

            // for built-in layers, we clone the information from the built-in type descriptors
            TypeSystemDescription tsd = new TypeSystemDescription_impl();
            if (layer.isBuiltIn()) {
                for (String typeName : layerSupport.getGeneratedTypeNames(layer)) {
                    exportBuiltInTypeDescription(builtInTypes, tsd, typeName);
                }
            }
            // for custom layers, we use the information from the project settings
            else {
                layerSupport.generateTypes(tsd, layer, allFeaturesInProject);
            }
            allTsds.add(tsd);
        }

        {
            // Explicitly add Token because the layer may not be declared in the project
            TypeSystemDescription tsd = new TypeSystemDescription_impl();
            exportBuiltInTypeDescription(builtInTypes, tsd, Token.class.getName());
            allTsds.add(tsd);
        }

        {
            // Explicitly add Sentence because the layer may not be declared in the project
            TypeSystemDescription tsd = new TypeSystemDescription_impl();
            exportBuiltInTypeDescription(builtInTypes, tsd, Sentence.class.getName());
            allTsds.add(tsd);
        }

        // The merging action here takes care of removing/conflating potential duplicate
        // declarations
        return CasCreationUtils.mergeTypeSystems(allTsds);
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
                if (feature.getElementType() != null && !isNativeUimaType(feature.getElementType())
                        && aTarget.getType(feature.getElementType()) == null) {
                    exportBuiltInTypeDescription(aSource, aTarget, feature.getElementType());
                }
                else if (feature.getRangeTypeName() != null
                        && !isNativeUimaType(feature.getRangeTypeName())
                        && aTarget.getType(feature.getRangeTypeName()) == null) {
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
        typeSystems.add(builtInTypes);

        if (aIncludeInternalTypes) {
            // Types internally used by WebAnno (which we intentionally exclude from being detected
            // by uimaFIT because we want to have an easy way to create a type system excluding
            // these types when we export files from the project
            typeSystems.add(CasMetadataUtils.getInternalTypeSystem());
        }

        // Types declared within the project
        typeSystems.add(getCustomProjectTypes(aProject));

        return mergeTypeSystems(typeSystems);
    }

    @Override
    public void upgradeCas(CAS aCas, AnnotationDocument aAnnotationDocument)
        throws UIMAException, IOException
    {
        upgradeCas(aCas, aAnnotationDocument.getDocument(), aAnnotationDocument.getUser());
    }

    @Override
    public void upgradeCas(CAS aCas, SourceDocument aSourceDocument, String aUser,
            CasUpgradeMode aMode)
        throws UIMAException, IOException
    {
        switch (aMode) {
        case NO_CAS_UPGRADE:
            return;
        case AUTO_CAS_UPGRADE: {
            boolean upgraded = upgradeCasIfRequired(aCas, aSourceDocument);
            if (!upgraded) {
                try (MDC.MDCCloseable closable = MDC.putCloseable(Logging.KEY_PROJECT_ID,
                        String.valueOf(aSourceDocument.getProject().getId()))) {
                    log.debug(
                            "CAS of user [{}] for document [{}]({}) in project [{}]({}) is already "
                                    + "compatible with project type system - skipping upgrade",
                            aUser, aSourceDocument.getName(), aSourceDocument.getId(),
                            aSourceDocument.getProject().getName(),
                            aSourceDocument.getProject().getId());
                }
            }
            return;
        }
        case FORCE_CAS_UPGRADE:
            upgradeCas(aCas, aSourceDocument, aUser);
            return;
        }
    }

    @Override
    public void upgradeCas(CAS aCas, SourceDocument aSourceDocument, String aUser)
        throws UIMAException, IOException
    {
        upgradeCas(aCas, aSourceDocument.getProject());

        try (MDC.MDCCloseable closable = MDC.putCloseable(Logging.KEY_PROJECT_ID,
                String.valueOf(aSourceDocument.getProject().getId()))) {
            Project project = aSourceDocument.getProject();
            log.info("Upgraded CAS of user [{}] for " + "document [{}]({}) in project [{}]({})",
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
    public boolean upgradeCasIfRequired(CAS aCas, AnnotationDocument aAnnotationDocument)
        throws UIMAException, IOException
    {
        return upgradeCasIfRequired(asList(aCas), aAnnotationDocument.getProject());
    }

    @Override
    public boolean upgradeCasIfRequired(CAS aCas, SourceDocument aSourceDocument)
        throws UIMAException, IOException
    {
        return upgradeCasIfRequired(asList(aCas), aSourceDocument.getProject());
    }

    @Override
    public boolean upgradeCasIfRequired(Iterable<CAS> aCasIter, Project aProject)
        throws UIMAException, IOException
    {
        TypeSystemDescription ts = getFullProjectTypeSystem(aProject);

        // Check if the current CAS already contains the required type system
        boolean upgradePerformed = false;
        nextCas: for (CAS cas : aCasIter) {
            if (cas == null) {
                continue nextCas;
            }

            // When we get here, we must be willing and ready to write to the CAS - even if we
            // eventually figure out that no upgrade is required.
            CasStorageSession.get().assertWritingPermitted(cas);

            if (isUpgradeRequired(cas, ts)) {
                upgradeCas(cas, ts);
                upgradePerformed = true;
            }
        }

        return upgradePerformed;
    }

    @Override
    public TypeSystemDescription getTypeSystemForExport(Project aProject)
        throws ResourceInitializationException
    {
        return getFullProjectTypeSystem(aProject, false);
    }

    @Override
    public void prepareCasForExport(CAS aSourceCas, CAS aTargetCas, SourceDocument aSourceDocument,
            TypeSystemDescription aFullProjectTypeSystem)
        throws ResourceInitializationException, UIMAException, IOException
    {
        TypeSystemDescription tsd = aFullProjectTypeSystem;
        if (tsd == null) {
            tsd = getTypeSystemForExport(aSourceDocument.getProject());
        }

        upgradeCas(aSourceCas, aTargetCas, tsd);
    }

    @Override
    public void upgradeCas(CAS aCas, TypeSystemDescription aTargetTypeSystem)
        throws UIMAException, IOException
    {
        upgradeCas(aCas, aCas, aTargetTypeSystem);
    }

    /**
     * Load the contents from the source CAS, upgrade it to the target type system and write the
     * results to the target CAS. An in-place upgrade can be achieved by using the same CAS as
     * source and target.
     */
    @Override
    public void upgradeCas(CAS aSourceCas, CAS aTargetCas, TypeSystemDescription aTargetTypeSystem)
        throws UIMAException, IOException
    {
        CasStorageSession.get().assertWritingPermitted(aTargetCas);

        // Save source CAS type system (do this early since we might do an in-place upgrade)
        TypeSystem sourceTypeSystem = aSourceCas.getTypeSystem();

        // Save source CAS contents
        ByteArrayOutputStream serializedCasContents = new ByteArrayOutputStream();
        CAS realSourceCas = getRealCas(aSourceCas);
        // UIMA-6162 Workaround: synchronize CAS during de/serialization
        synchronized (((CASImpl) realSourceCas).getBaseCAS()) {
            serializeWithCompression(realSourceCas, serializedCasContents, sourceTypeSystem);
        }

        // Re-initialize the target CAS with new type system
        CAS realTargetCas = getRealCas(aTargetCas);
        // UIMA-6162 Workaround: synchronize CAS during de/serialization
        synchronized (((CASImpl) realTargetCas).getBaseCAS()) {
            CAS tempCas = CasFactory.createCas(aTargetTypeSystem);
            CASCompleteSerializer serializer = serializeCASComplete((CASImpl) tempCas);
            deserializeCASComplete(serializer, (CASImpl) realTargetCas);

            // Leniently load the source CAS contents into the target CAS
            CasIOUtils.load(new ByteArrayInputStream(serializedCasContents.toByteArray()),
                    getRealCas(aTargetCas), sourceTypeSystem);
        }
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
                log.debug("CAS update required: type {} does not exist", tdesc.getName());
                upgradeRequired = true;
                break nextType;
            }

            // Super-type does not match
            if (!Objects.equals(tdesc.getSupertypeName(), ts.getParent(t).getName())) {
                log.debug("CAS update required: supertypes of {} do not match: {} <-> {}",
                        tdesc.getName(), tdesc.getSupertypeName(), ts.getParent(t).getName());
                upgradeRequired = true;
                break nextType;
            }

            // Check features
            for (FeatureDescription fdesc : tdesc.getFeatures()) {
                Feature f = t.getFeatureByBaseName(fdesc.getName());

                // Feature does not exist
                if (f == null) {
                    log.debug("CAS update required: feature {} on type {} does not exist",
                            fdesc.getName(), tdesc.getName());
                    upgradeRequired = true;
                    break nextType;
                }

                // Range does not match
                if (CAS.TYPE_NAME_FS_ARRAY.equals(fdesc.getRangeTypeName())) {
                    if (!Objects.equals(fdesc.getElementType(),
                            f.getRange().getComponentType().getName())) {
                        log.debug(
                                "CAS update required: ranges of feature {} on type {} do not match: {} <-> {}",
                                fdesc.getName(), tdesc.getName(), fdesc.getRangeTypeName(),
                                f.getRange().getName());
                        upgradeRequired = true;
                        break nextType;
                    }
                }
                else {
                    if (!Objects.equals(fdesc.getRangeTypeName(), f.getRange().getName())) {
                        log.debug(
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

    // NOTE: Using @Transactional here would significantly slow down things because getAdapter() is
    // called rather often. It looks like listAnnotationFeature() works reasonably good also when
    // not called within a transaction. Should it turn out that we would need a @Transactional here,
    // then this should be refactored in some way. E.g. we keep the list of all project layers
    // in the AnnotatorState now - maybe we can use it from there when calling relevant methods
    // on the adapter.
    @Override
    public TypeAdapter getAdapter(AnnotationLayer aLayer)
    {
        return layerSupportRegistry.getLayerSupport(aLayer).createAdapter(aLayer,
                () -> listAnnotationFeature(aLayer));
    }

    @Override
    @Transactional(noRollbackFor = NoResultException.class)
    public TypeAdapter findAdapter(Project aProject, FeatureStructure aFS)
    {
        AnnotationLayer layer = findLayer(aProject, aFS.getType().getName());
        return getAdapter(layer);
    }

    @Override
    @Transactional
    public void importUimaTypeSystem(Project aProject, TypeSystemDescription aTSD)
        throws ResourceInitializationException
    {
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
                        attachLayer = findLayer(aProject, relDetails.getAttachLayer());
                    }
                    catch (NoResultException e) {
                        // If it does not exist in the project yet, then we create it
                        attachLayer = analysis.getLayer(relDetails.getAttachLayer());
                        attachLayer.setProject(aProject);
                        createOrUpdateLayer(attachLayer);
                    }

                    l.setAttachType(attachLayer);
                }

                createOrUpdateLayer(l);
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

    @Override
    @Transactional
    public List<AttachedAnnotation> getAttachedRels(AnnotationLayer aLayer, AnnotationFS aFs)
    {
        CAS cas = aFs.getCAS();
        List<AttachedAnnotation> result = new ArrayList<>();
        for (AnnotationLayer relationLayer : listAttachedRelationLayers(aLayer)) {
            RelationAdapter relationAdapter = (RelationAdapter) getAdapter(relationLayer);
            Type relationType = CasUtil.getType(cas, relationLayer.getName());
            Feature sourceFeature = relationType
                    .getFeatureByBaseName(relationAdapter.getSourceFeatureName());
            Feature targetFeature = relationType
                    .getFeatureByBaseName(relationAdapter.getTargetFeatureName());

            // This code is already prepared for the day that relations can go between
            // different layers and may have different attach features for the source and
            // target layers.
            Feature relationSourceAttachFeature = null;
            Feature relationTargetAttachFeature = null;
            if (relationAdapter.getAttachFeatureName() != null) {
                relationSourceAttachFeature = sourceFeature.getRange()
                        .getFeatureByBaseName(relationAdapter.getAttachFeatureName());
                relationTargetAttachFeature = targetFeature.getRange()
                        .getFeatureByBaseName(relationAdapter.getAttachFeatureName());
            }

            for (AnnotationFS relationFS : CasUtil.select(cas, relationType)) {
                if (!(relationFS instanceof AnnotationFS)) {
                    continue;
                }

                // Here we get the annotations that the relation is pointing to in the UI
                AnnotationFS sourceFS;
                if (relationSourceAttachFeature != null) {
                    sourceFS = (AnnotationFS) relationFS.getFeatureValue(sourceFeature)
                            .getFeatureValue(relationSourceAttachFeature);
                }
                else {
                    sourceFS = (AnnotationFS) relationFS.getFeatureValue(sourceFeature);
                }

                AnnotationFS targetFS;
                if (relationTargetAttachFeature != null) {
                    targetFS = (AnnotationFS) relationFS.getFeatureValue(targetFeature)
                            .getFeatureValue(relationTargetAttachFeature);
                }
                else {
                    targetFS = (AnnotationFS) relationFS.getFeatureValue(targetFeature);
                }

                if (sourceFS == null || targetFS == null) {
                    StringBuilder message = new StringBuilder();

                    message.append("Relation [" + relationAdapter.getLayer().getName()
                            + "] with id [" + getAddr(relationFS)
                            + "] has loose ends - cannot identify attached annotations.");
                    if (relationAdapter.getAttachFeatureName() != null) {
                        message.append("\nRelation [" + relationAdapter.getLayer().getName()
                                + "] attached to feature [" + relationAdapter.getAttachFeatureName()
                                + "].");
                    }
                    message.append("\nSource: " + sourceFS);
                    message.append("\nTarget: " + targetFS);
                    log.warn("{}", message.toString());
                    continue;
                }

                boolean isIncoming = isSame(targetFS, aFs);
                boolean isOutgoing = isSame(sourceFS, aFs);

                if (isIncoming && isOutgoing) {
                    result.add(new AttachedAnnotation(relationLayer, relationFS, sourceFS, LOOP));
                }
                else if (isIncoming) {
                    result.add(
                            new AttachedAnnotation(relationLayer, relationFS, sourceFS, INCOMING));
                }
                else if (isOutgoing) {
                    result.add(
                            new AttachedAnnotation(relationLayer, relationFS, targetFS, OUTGOING));
                }
            }
        }

        return result;
    }

    @Override
    @Transactional
    public List<AttachedAnnotation> getAttachedLinks(AnnotationLayer aLayer, AnnotationFS aFs)
    {
        CAS cas = aFs.getCAS();
        List<AttachedAnnotation> result = new ArrayList<>();
        TypeAdapter adapter = getAdapter(aLayer);
        if (adapter instanceof SpanAdapter) {
            for (AnnotationFeature linkFeature : listAttachedLinkFeatures(aLayer)) {
                if (MultiValueMode.ARRAY.equals(linkFeature.getMultiValueMode())
                        && LinkMode.WITH_ROLE.equals(linkFeature.getLinkMode())) {
                    // Fetch slot hosts that could link to the current FS and check if any of
                    // them actually links to the current FS
                    Type linkHost = CasUtil.getType(cas, linkFeature.getLayer().getName());
                    for (FeatureStructure linkFS : CasUtil.selectFS(cas, linkHost)) {
                        if (!(linkFS instanceof AnnotationFS)) {
                            continue;
                        }

                        List<LinkWithRoleModel> links = adapter.getFeatureValue(linkFeature,
                                linkFS);
                        for (int li = 0; li < links.size(); li++) {
                            LinkWithRoleModel link = links.get(li);
                            AnnotationFS linkTarget = selectByAddr(cas, AnnotationFS.class,
                                    link.targetAddr);
                            // If the current annotation fills a slot, then add the slot host to
                            // our list of attached links.
                            if (isSame(linkTarget, aFs)) {
                                result.add(new AttachedAnnotation(linkFeature.getLayer(),
                                        (AnnotationFS) linkFS, INCOMING));
                            }
                        }
                    }
                }
            }
        }
        return result;
    }
}
