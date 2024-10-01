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

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

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
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.schema.config.AnnotationSchemaServiceAutoConfiguration;
import de.tudarmstadt.ukp.inception.support.WebAnnoConst;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@link AnnotationSchemaServiceAutoConfiguration#tagSetExporter}.
 * </p>
 */
public class TagSetExporter
    implements ProjectExporter
{
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final AnnotationSchemaService schemaService;

    public TagSetExporter(AnnotationSchemaService aSchemaService)
    {
        schemaService = aSchemaService;
    }

    @Override
    public void exportData(FullProjectExportRequest aRequest, ProjectExportTaskMonitor aMonitor,
            ExportedProject aExProject, ZipOutputStream aStage)
    {
        var extTagSets = new ArrayList<ExportedTagSet>();
        for (var tagSet : schemaService.listTagSets(aRequest.getProject())) {
            var exTagSet = new ExportedTagSet();
            exTagSet.setCreateTag(tagSet.isCreateTag());
            exTagSet.setDescription(tagSet.getDescription());
            exTagSet.setLanguage(tagSet.getLanguage());
            exTagSet.setName(tagSet.getName());

            var exTags = new ArrayList<ExportedTag>();
            for (var tag : schemaService.listTags(tagSet)) {
                var exTag = new ExportedTag();
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

        for (var exTagSet : aExProject.getTagSets()) {
            importTagSet(new TagSet(), exTagSet, aProject);
        }
    }

    /**
     * Import tagsets from projects prior to WebAnno 2.0.
     */
    @Deprecated
    private void importTagSetsV0(Project aProject, ExportedProject aExProject) throws IOException
    {
        var importedTagSets = aExProject.getTagSets();

        var posTags = new ArrayList<String>();
        var depTags = new ArrayList<String>();
        var neTags = new ArrayList<String>();
        var posTagDescriptions = new ArrayList<String>();
        var depTagDescriptions = new ArrayList<String>();
        var neTagDescriptions = new ArrayList<String>();
        var corefTypeTags = new ArrayList<String>();
        var corefRelTags = new ArrayList<String>();
        for (var tagSet : importedTagSets) {
            switch (tagSet.getTypeName()) {
            case WebAnnoConst.POS:
                for (var tag : tagSet.getTags()) {
                    posTags.add(tag.getName());
                    posTagDescriptions.add(tag.getDescription());
                }
                break;
            case WebAnnoConst.DEPENDENCY:
                for (var tag : tagSet.getTags()) {
                    depTags.add(tag.getName());
                    depTagDescriptions.add(tag.getDescription());
                }
                break;
            case WebAnnoConst.NAMEDENTITY:
                for (var tag : tagSet.getTags()) {
                    neTags.add(tag.getName());
                    neTagDescriptions.add(tag.getDescription());
                }
                break;
            case WebAnnoConst.COREFRELTYPE:
                for (var tag : tagSet.getTags()) {
                    corefTypeTags.add(tag.getName());
                }
                break;
            case WebAnnoConst.COREFERENCE:
                for (var tag : tagSet.getTags()) {
                    corefRelTags.add(tag.getName());
                }
                break;
            }
        }

        new LegacyProjectInitializer(schemaService).initialize(aProject,
                posTags.toArray(String[]::new), posTagDescriptions.toArray(String[]::new),
                depTags.toArray(String[]::new), depTagDescriptions.toArray(String[]::new),
                neTags.toArray(String[]::new), neTagDescriptions.toArray(String[]::new),
                corefTypeTags.toArray(String[]::new), corefRelTags.toArray(String[]::new));
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
        schemaService.createTagSet(aTagSet);

        var existingTags = schemaService.listTags(aTagSet).stream() //
                .map(Tag::getName) //
                .collect(Collectors.toSet());

        var tags = new ArrayList<Tag>();
        for (var exTag : aExTagSet.getTags()) {
            // do not duplicate tag
            if (existingTags.contains(exTag.getName())) {
                continue;
            }

            var tag = new Tag();
            tag.setTagSet(aTagSet);
            tag.setName(exTag.getName());
            tag.setDescription(exTag.getDescription());
            tag.setRank(tags.size());
            tags.add(tag);
        }

        schemaService.createTags(tags.stream().toArray(Tag[]::new));
    }
}
