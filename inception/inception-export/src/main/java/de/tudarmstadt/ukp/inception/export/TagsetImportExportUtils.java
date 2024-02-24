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
package de.tudarmstadt.ukp.inception.export;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.Tag;
import de.tudarmstadt.ukp.clarin.webanno.model.TagSet;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;

public class TagsetImportExportUtils
{
    public static TagSet importTagsetFromTabSeparated(AnnotationSchemaService annotationService,
            Project project, InputStream aInputStream, boolean aOverwrite)
        throws IOException
    {
        var text = IOUtils.toString(aInputStream, UTF_8);
        var tabbedTagsetFromFile = LayerImportExportUtils.getTagSetFromFile(text);

        var listOfTagsFromFile = tabbedTagsetFromFile.keySet();
        var i = 0;
        var tagSetName = "";
        var tagSetDescription = "";
        var tagsetLanguage = "";
        de.tudarmstadt.ukp.clarin.webanno.model.TagSet tagSet = null;
        for (String key : listOfTagsFromFile) {
            // the first key is the tagset name and its
            // description
            if (i == 0) {
                tagSetName = key;
                tagSetDescription = tabbedTagsetFromFile.get(key);
            }
            // the second key is the tagset language
            else if (i == 1) {
                tagsetLanguage = key;
                // remove and replace the tagset if it
                // exist
                if (annotationService.existsTagSet(tagSetName, project)) {
                    // If overwrite is enabled
                    if (aOverwrite) {
                        tagSet = annotationService.getTagSet(tagSetName, project);
                        annotationService.removeAllTags(tagSet);
                    }
                    else {
                        tagSet = new TagSet();
                        tagSet.setName(JsonImportUtil.copyTagSetName(annotationService, tagSetName,
                                project));
                    }
                }
                else {
                    tagSet = new TagSet();
                    tagSet.setName(tagSetName);
                }
                tagSet.setDescription(tagSetDescription.replace("\\n", "\n"));
                tagSet.setLanguage(tagsetLanguage);
                tagSet.setProject(project);
                annotationService.createTagSet(tagSet);
            }
            // otherwise it is a tag entry, add the tag to the tagset
            else {
                Tag tag = new Tag();
                tag.setName(key);
                tag.setDescription(tabbedTagsetFromFile.get(key).replace("\\n", "\n"));
                tag.setRank(i);
                tag.setTagSet(tagSet);
                annotationService.createTag(tag);
            }
            i++;
        }

        return tagSet;
    }

    public static TagSet importTagsetFromJson(AnnotationSchemaService annotationService,
            Project project, InputStream aInputStream, boolean aOverwrite)
        throws IOException
    {
        if (aOverwrite) {
            return JsonImportUtil.importTagSetFromJsonWithOverwrite(project, aInputStream,
                    annotationService);
        }
        else {
            return JsonImportUtil.importTagSetFromJson(project, aInputStream, annotationService);
        }
    }
}
