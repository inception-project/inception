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
package de.tudarmstadt.ukp.clarin.webanno.project.initializers;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;

import de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedTag;
import de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedTagSet;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.Tag;
import de.tudarmstadt.ukp.clarin.webanno.model.TagSet;
import de.tudarmstadt.ukp.clarin.webanno.support.JSONUtil;
import de.tudarmstadt.ukp.inception.schema.AnnotationSchemaService;

public class JsonImportUtil
{
    /**
     * Works for scenarios with overwrite enabled Checks if tagset already exists, then overwrites
     * otherwise works normally
     * 
     * @param aProject
     *            the project to import the tags into
     * @param aInputStream
     *            the stream to read the JSON tagset from
     * @param aAnnotationService
     *            the annotation service to use for the import
     * @return the imported tag set
     * @throws IOException
     *             if there was an I/O-level problem
     */
    public static TagSet importTagSetFromJsonWithOverwrite(Project aProject,
            InputStream aInputStream, AnnotationSchemaService aAnnotationService)
        throws IOException
    {
        ExportedTagSet importedTagSet = JSONUtil.fromJsonStream(ExportedTagSet.class, aInputStream);

        if (aAnnotationService.existsTagSet(importedTagSet.getName(), aProject)) {
            // A tagset exists so we'll have to replace it
            return replaceTagSet(aProject, importedTagSet, aAnnotationService);
        }
        else {
            // Proceed normally
            return createTagSet(aProject, importedTagSet, aAnnotationService);
        }
    }

    private static TagSet replaceTagSet(Project project, ExportedTagSet importedTagSet,
            AnnotationSchemaService aAnnotationService)
        throws IOException
    {
        String importedTagSetName = importedTagSet.getName();
        de.tudarmstadt.ukp.clarin.webanno.model.TagSet tagsetInUse = aAnnotationService
                .getTagSet(importedTagSetName, project);
        // Remove all tags associated with Tagset
        aAnnotationService.removeAllTags(tagsetInUse);
        // Copy and update TagSet Information from imported tagset
        tagsetInUse.setDescription(importedTagSet.getDescription());
        tagsetInUse.setName(importedTagSetName);
        tagsetInUse.setLanguage(importedTagSet.getLanguage());
        tagsetInUse.setProject(project);
        aAnnotationService.createTagSet(tagsetInUse);

        // Add all tags from imported tagset
        int rank = 0;
        for (ExportedTag tag : importedTagSet.getTags()) {
            Tag newTag = new Tag();
            newTag.setDescription(tag.getDescription());
            newTag.setName(tag.getName());
            newTag.setRank(rank);
            newTag.setTagSet(tagsetInUse);
            aAnnotationService.createTag(newTag);
            rank++;
        }

        return tagsetInUse;
    }

    public static TagSet importTagSetFromJson(Project project, InputStream tagInputStream,
            AnnotationSchemaService aAnnotationService)
        throws IOException
    {
        String text = IOUtils.toString(tagInputStream, "UTF-8");

        ExportedTagSet importedTagSet = JSONUtil.getObjectMapper().readValue(text,
                ExportedTagSet.class);
        return createTagSet(project, importedTagSet, aAnnotationService);
    }

    public static TagSet createTagSet(Project project, ExportedTagSet aExTagSet,
            AnnotationSchemaService aAnnotationService)
        throws IOException
    {
        String exTagSetName = aExTagSet.getName();
        if (aAnnotationService.existsTagSet(exTagSetName, project)) {
            exTagSetName = copyTagSetName(aAnnotationService, exTagSetName, project);
        }

        TagSet newTagSet = new TagSet();
        newTagSet.setDescription(aExTagSet.getDescription());
        newTagSet.setName(exTagSetName);
        newTagSet.setLanguage(aExTagSet.getLanguage());
        newTagSet.setProject(project);
        newTagSet.setCreateTag(aExTagSet.isCreateTag());
        aAnnotationService.createTagSet(newTagSet);

        List<Tag> tags = new ArrayList<>();
        for (ExportedTag exTag : aExTagSet.getTags()) {
            Tag tag = new Tag();
            tag.setDescription(exTag.getDescription());
            tag.setTagSet(newTagSet);
            tag.setName(exTag.getName());
            tag.setRank(tags.size());
            tags.add(tag);
        }

        aAnnotationService.createTags(tags.stream().toArray(Tag[]::new));

        return newTagSet;
    }

    /**
     * Provides a new name if TagSet already exists.
     * 
     * @param aAnnotationService
     *            annotation service to look up existing tagsets
     * @param aName
     *            the suggested name
     * @param aProject
     *            the project into which to import the tagset
     * @return a unique tag set name
     */
    public static String copyTagSetName(AnnotationSchemaService aAnnotationService, String aName,
            Project aProject)
    {
        String betterTagSetName = "copy_of_" + aName;
        int i = 1;
        while (true) {
            if (aAnnotationService.existsTagSet(betterTagSetName, aProject)) {
                betterTagSetName = "copy_of_" + aName + "(" + i + ")";
                i++;
            }
            else {
                return betterTagSetName;
            }
        }
    }
}
