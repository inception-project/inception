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
package de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.aero.model;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import io.swagger.v3.oas.annotations.media.Schema;

public class RProject
{
    @Schema(description = """
            Numeric project identifier. Can be used in place of the slug when addressing the
            project in API paths.
            """)
    public long id;

    /**
     * @deprecated Use {@link #slug} instead. This field carries the project slug, not the
     *             human-readable project name - the latter is available as {@link #title}.
     */
    @Deprecated
    @Schema(description = """
            URL slug of the project. Deprecated - use `slug` instead. Note that this field
            carries the slug and not the human-readable project name which is available as
            `title`.
            """)
    public String name;

    @Schema(description = """
            URL slug of the project. Can be used in place of the numeric ID when addressing
            the project in API paths.
            """)
    public String slug;

    @Schema(description = """
            Human-readable name of the project.
            """)
    public String title;

    public RProject(Project aProject)
    {
        id = aProject.getId();
        name = aProject.getSlug();
        slug = aProject.getSlug();
        title = aProject.getName();
    }

    public RProject(long aId, String aTitle, String aSlug)
    {
        super();
        id = aId;
        name = aSlug;
        slug = aSlug;
        title = aTitle;
    }
}
