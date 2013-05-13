/*******************************************************************************
 * Copyright 2012
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
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

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationType;
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
     *            TODO
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
     *            TODO
     * @throws IOException
     */
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_USER')")
    void createTagSet(TagSet tagset, User user)
        throws IOException;

    /**
     * creates a type which will be a span or arc(relation) type. Currently the annotation types
     * are highly highly tied with the tagsets, one tagset per type. POS, Names Entity, and
     * coreference links are span types while coreference chains and dependency parsings are
     * arc(relation) types.
     *
     * @param type
     */
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_USER')")
    void createType(AnnotationType type);

    /**
     * gets a {@link Tag} using its name and a {@link TagSet}
     *
     * @param aTagName
     * @param tagSet
     * @return
     */
    Tag getTag(String tagName, TagSet tagSet);

    /**
     * Get a a {@link TagSet} for a given {@link AnnotationType}. One Tagset per annotation type
     *
     * @param aType
     * @return
     */
   boolean  existTagSet(AnnotationType type, Project project);

    /**
     * get a {@link TagSet} by its type and its project
     *
     * @param tagName
     * @return {@link TagSet}
     */
    TagSet getTagSet(AnnotationType type, Project project);

    /**
     * Get Tagset by its ID
     */
    TagSet getTagSet(long id);

    /**
     * Get an {@link AnnotationType}
     */
    AnnotationType getType(String name, String type);
    /**
    * Check if an {@link AnnotationType} already exists.
    */
   boolean existsType(String name, String type);


    /**
     * Initialize the project with default {@link AnnotationType}, {@link TagSet}s, and {@link Tag}
     * s. This is done per Project
     *
     * @param aProject
     * @throws IOException
     */


    void initializeTypesForProject(Project project, User user)
        throws IOException;

    /**
     * list all {@link AnnotationType} in the system
     *
     * @return {@link List<AnnotationType>}
     */
    List<AnnotationType> listAnnotationType();

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
     * List all {@link TagSet }s in a project
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
}
