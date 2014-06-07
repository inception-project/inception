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
package de.tudarmstadt.ukp.clarin.webanno.api;

import java.io.IOException;
import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.Tag;
import de.tudarmstadt.ukp.clarin.webanno.model.TagSet;
import de.tudarmstadt.ukp.clarin.webanno.model.User;

/**
 * This interface contains methods which are related to TagSet, Tag and Type for the annotation
 * project .
 *
 * @author Seid Muhie Yimam
 * @author Richard Eckart de Castilho
 *
 */
public interface AnnotationService
{
    /**
     * creates a {@link Tag} for a given {@link TagSet}. Combination of {@code tag name} and
     * {@code tagset name} should be unique
     *
     * @param tag
     * @param user
     *            The User who perform this operation
     * @throws IOException
     */
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_USER')")
    void createTag(Tag tag, User user)
        throws IOException;

    /**
     * creates a {@link TagSet} object in the database
     *
     * @param tagset
     * @param user
     *            The User who perform this operation
     * @throws IOException
     */
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_USER')")
    void createTagSet(TagSet tagset, User user)
        throws IOException;

    /**
     * creates a type which will be a span, chain, or arc(relation) type. Currently the annotation
     * types are highly highly tied with the tagsets, one tagset per type. POS, Names Entity, and
     * coreference links are span types while coreference chains and dependency parsings are
     * arc(relation) types.
     *
     * @param type
     * @throws IOException
     */
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_USER')")
    void createLayer(AnnotationLayer type, User user)
        throws IOException;

    void createFeature(AnnotationFeature feature);

    /**
     * gets a {@link Tag} using its name and a {@link TagSet}
     *
     * @param aTagName
     * @param tagSet
     * @return
     */
    Tag getTag(String tagName, TagSet tagSet);

    /**
     * Check if a tag with this name in the given tagset exists
     */
    boolean existsTag(String tagName, TagSet tagSet);

    /**
     * Check if a {@link TagSet} with this name exists
     */
    boolean existsTagSet(String name, Project project);

    /**
     * check if a {@link TagSet} in this {@link Project} exists
     */
    boolean existsTagSet(Project project);

    /**
     * check if an {@link AnnotationLayer} exists with this name and type in this {@link Project}
     */
    boolean existsLayer(String name, String type, Project project);

    /**
     *
     * Check if this {@link AnnotationFeature} already exists
     */
    boolean existsFeature(String name, AnnotationLayer type);

    /**
     * get a {@link TagSet} with this name in a {@link Project}
     *
     * @param tagName
     * @return {@link TagSet}
     */
    TagSet getTagSet(String name, Project project);

    /**
     * Get Tagset by its ID
     */
    TagSet getTagSet(long id);

    /**
     * Get an annotation layer using its id
     */
    AnnotationLayer getLayer(long id);

    /**
     * Get an {@link AnnotationLayer}
     */
    AnnotationLayer getLayer(String name, Project project);

    /**
     * Get a {@link AnnotationFeature} name using its ID. Used for updating annotations as it is
     * represented <id><type>
     *
     * @param id
     * @return
     */
    AnnotationFeature getFeature(long id);

    /**
     * Get an {@link AnnotationFeature} using its name
     */
    AnnotationFeature getFeature(String name, AnnotationLayer type);
    /**
     * Check if an {@link AnnotationLayer} already exists.
     */
    boolean existsType(String name, String type);

    /**
     * Initialize the project with default {@link AnnotationLayer}, {@link TagSet}s, and {@link Tag}
     * s. This is done per Project. For older projects, this method is used to import old tagsets
     * and convert to the new scheme.
     *
     * @param aProject
     * @throws IOException
     */
    void initializeTypesForProject(Project project, User user, String[] postags,
            String[] posTagDescriptions, String[] depTags, String[] depTagDescriptions,
            String[] neTags, String[] neTagDescriptions, String[] corefTypeTags,
            String[] corefRelTags)
        throws IOException;

    /**
     * list all {@link AnnotationLayer} in the system
     *
     * @return {@link List<AnnotationType>}
     */
    List<AnnotationLayer> listAnnotationType();

    /**
     * List all annotation types in a project
     */
    List<AnnotationLayer> listAnnotationLayer(Project project);

    /**
     * List all the features in a {@link AnnotationLayer} for this {@link Project}
     */
    List<AnnotationFeature> listAnnotationFeature(AnnotationLayer type);

    /**
     * List all features in the project
     */
    List<AnnotationFeature> listAnnotationFeature(Project project);

    /**
     * list all {@link Tag} in the system
     *
     * @return
     */
    List<Tag> listTags();

    /**
     * list all {@link Tag} in a {@link TagSet}
     *
     * @param tag
     * @return
     */
    List<Tag> listTags(TagSet tag);

    /**
     * list all {@link TagSet} in the system
     *
     * @return
     */
    List<TagSet> listTagSets();


    /**
     * List all {@link TagSet }s in a {@link Project}
     *
     * @param project
     * @return
     */
    List<TagSet> listTagSets(Project project);

    /**
     * Removes a {@link Tag} from the database
     *
     * @param tag
     */
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_USER')")
    void removeTag(Tag tag);

    /**
     * removes a {@link TagSet } from the database
     *
     * @param tagset
     */
    void removeTagSet(TagSet tagset);

    /**
     *
     * Should be called with care. Only when a project hosting this feature is removed
     */
    void removeAnnotationFeature(AnnotationFeature feature);

    /**
     *
     * Should be called with care. Only when a project hosting this layer is removed
     */
    void removeAnnotationLayer(AnnotationLayer type);

    void createPOSLayer(Project project, User user, String[] postags, String[] posTagDescriptions)
        throws IOException;

    void createDepLayer(Project project, User user, String[] depTags, String[] depTagDescriptions)
        throws IOException;

    void createNeLayer(Project project, User user, String[] neTags, String[] neTagDescriptions)
        throws IOException;

    void createCorefLayer(Project project, User user, String[] corefTypeTags, String[] corefRelTags)
        throws IOException;

    void createLemmaLayer(Project project, User user)
        throws IOException;
}
