/*
 * Copyright 2016
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

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedTag;
import de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedTagSet;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.Tag;
import de.tudarmstadt.ukp.clarin.webanno.model.TagSet;
import de.tudarmstadt.ukp.clarin.webanno.support.JSONUtil;

public class JsonImportUtil
{
    /**
     * Works for scenarios with overwrite enabled Checks if tagset already exists, then overwrites
     * otherwise works normally
     */
    public static TagSet importTagSetFromJsonWithOverwrite(Project project,
            InputStream tagInputStream, AnnotationSchemaService aAnnotationService)
        throws IOException
    {
        String text = IOUtils.toString(tagInputStream, "UTF-8");

        ExportedTagSet importedTagSet = JSONUtil.getObjectMapper().readValue(text,
                ExportedTagSet.class);

        if (aAnnotationService.existsTagSet(importedTagSet.getName(), project)) {
            // A tagset exists so we'll have to replace it
            return replaceTagSet(project, importedTagSet, aAnnotationService);
        }
        else {
            // Proceed normally
            return createTagSet(project, importedTagSet, aAnnotationService);
        }
    }

    private static TagSet replaceTagSet(Project project,
            ExportedTagSet importedTagSet,
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
        for (ExportedTag tag : importedTagSet.getTags()) {
            Tag newTag = new Tag();
            newTag.setDescription(tag.getDescription());
            newTag.setName(tag.getName());
            newTag.setTagSet(tagsetInUse);
            aAnnotationService.createTag(newTag);
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
    
    public static TagSet createTagSet(Project project,
            ExportedTagSet importedTagSet,
            AnnotationSchemaService aAnnotationService)
        throws IOException
    {
        String importedTagSetName = importedTagSet.getName();
        if (aAnnotationService.existsTagSet(importedTagSetName, project)) {
            // aAnnotationService.removeTagSet(aAnnotationService.getTagSet(
            // importedTagSet.getName(), project));
            // Rename Imported TagSet instead of deleting the old one.
            importedTagSetName = copyTagSetName(aAnnotationService, importedTagSetName, project);
        }

        TagSet newTagSet = new TagSet();
        newTagSet.setDescription(importedTagSet.getDescription());
        newTagSet.setName(importedTagSetName);
        newTagSet.setLanguage(importedTagSet.getLanguage());
        newTagSet.setProject(project);
        newTagSet.setCreateTag(importedTagSet.isCreateTag());
        aAnnotationService.createTagSet(newTagSet);
        for (ExportedTag tag : importedTagSet.getTags()) {
            Tag newTag = new Tag();
            newTag.setDescription(tag.getDescription());
            newTag.setName(tag.getName());
            newTag.setTagSet(newTagSet);
            aAnnotationService.createTag(newTag);
        }
        
        return newTagSet;
    }

    /**
     * Provides a new name if TagSet already exists.
     */
    public static String copyTagSetName(AnnotationSchemaService aAnnotationService,
            String importedTagSetName, Project project)
    {
        String betterTagSetName = "copy_of_" + importedTagSetName;
        int i = 1;
        while (true) {
            if (aAnnotationService.existsTagSet(betterTagSetName, project)) {
                betterTagSetName = "copy_of_" + importedTagSetName + "(" + i + ")";
                i++;
            }
            else {
                return betterTagSetName;
            }
    
        }
    }
}
