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
package de.tudarmstadt.ukp.clarin.webanno.api.dao.export.exporters;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipFile;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExportRequest;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExporter;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectImportRequest;
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
        // this is projects prior to version 2.0
        if (aExProject.getVersion() == 0) {
            importTagSetsV0(aProject, aExProject);
        }
        else {
            for (ExportedTagSet exTagSet : aExProject.getTagSets()) {
                importTagSet(new TagSet(), exTagSet, aProject);
            }
        }
    }
    
    /**
     * Import tagsets from projects prior to WebAnno 2.0.
     */
    private void importTagSetsV0(Project aProject, ExportedProject aExProject)
        throws IOException
    {
        List<ExportedTagSet> importedTagSets = aExProject.getTagSets();
        
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
