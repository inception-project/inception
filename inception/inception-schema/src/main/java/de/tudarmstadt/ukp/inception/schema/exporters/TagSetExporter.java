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
package de.tudarmstadt.ukp.inception.schema.exporters;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.zip.ZipFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.api.export.FullProjectExportRequest;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExportTaskMonitor;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExporter;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectImportRequest;
import de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedProject;
import de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedTag;
import de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedTagSet;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.Tag;
import de.tudarmstadt.ukp.clarin.webanno.model.TagSet;
import de.tudarmstadt.ukp.clarin.webanno.support.WebAnnoConst;
import de.tudarmstadt.ukp.inception.schema.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.schema.config.AnnotationSchemaServiceAutoConfiguration;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@link AnnotationSchemaServiceAutoConfiguration#tagSetExporter}.
 * </p>
 */
public class TagSetExporter
    implements ProjectExporter
{
    private static final Logger LOG = LoggerFactory.getLogger(TagSetExporter.class);

    private final AnnotationSchemaService annotationService;

    public TagSetExporter(AnnotationSchemaService aAnnotationService)
    {
        annotationService = aAnnotationService;
    }

    @Override
    public void exportData(FullProjectExportRequest aRequest, ProjectExportTaskMonitor aMonitor,
            ExportedProject aExProject, File aStage)
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

        LOG.info("Exported [{}] tagsets for project [{}]", extTagSets.size(),
                aRequest.getProject().getName());
    }

    @Override
    public void importData(ProjectImportRequest aRequest, Project aProject,
            ExportedProject aExProject, ZipFile aZip)
        throws Exception
    {
        // this is projects prior to version 2.0
        if (aExProject.getVersion() == 0) {
            importTagSetsV0(aProject, aExProject);
            return;
        }

        for (ExportedTagSet exTagSet : aExProject.getTagSets()) {
            importTagSet(new TagSet(), exTagSet, aProject);
        }
    }

    /**
     * Import tagsets from projects prior to WebAnno 2.0.
     */
    @Deprecated
    private void importTagSetsV0(Project aProject, ExportedProject aExProject) throws IOException
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

        Set<String> existingTags = annotationService.listTags(aTagSet).stream() //
                .map(Tag::getName) //
                .collect(Collectors.toSet());

        List<Tag> tags = new ArrayList<>();
        for (ExportedTag exTag : aExTagSet.getTags()) {
            // do not duplicate tag
            if (existingTags.contains(exTag.getName())) {
                continue;
            }

            Tag tag = new Tag();
            tag.setTagSet(aTagSet);
            tag.setName(exTag.getName());
            tag.setDescription(exTag.getDescription());
            tag.setRank(tags.size());
            tags.add(tag);
        }

        annotationService.createTags(tags.stream().toArray(Tag[]::new));
    }
}
