/*
 * Copyright 2018
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
package de.tudarmstadt.ukp.clarin.webanno.export.exporers;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipFile;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.export.ProjectExportRequest;
import de.tudarmstadt.ukp.clarin.webanno.export.ProjectImportRequest;
import de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedProject;
import de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedTag;
import de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedTagSet;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.Tag;
import de.tudarmstadt.ukp.clarin.webanno.model.TagSet;

@Component
public class TagSetExporter
    implements ProjectExporter
{
    private @Autowired AnnotationSchemaService annotationService;

    @Override
    public void exportData(ProjectExportRequest aRequest, ExportedProject aExProject, File aStage)
        throws Exception
    {
        List<ExportedTagSet> extTagSets = new ArrayList<>();
        for (TagSet tagSet : annotationService.listTagSets(aRequest.getProject())) {
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

        aExProject.setTagSets(extTagSets);
    }
    
    @Override
    public void importData(ProjectImportRequest aRequest, Project aProject,
            ExportedProject aExProject, ZipFile aZip)
        throws Exception
    {
        for (ExportedTagSet exTagSet : aExProject.getTagSets()) {
            importTagSet(new TagSet(), exTagSet, aProject);
        }
    }
    
    private void importTagSet(TagSet aTagSet, ExportedTagSet aExTagSet, Project aProject)
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
}
