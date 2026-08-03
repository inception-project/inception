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

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toCollection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import io.swagger.v3.oas.annotations.extensions.Extension;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * @param id
 *            numeric project identifier.
 * @param name
 *            deprecated - carries the project slug. Use {@code slug} instead.
 * @param slug
 *            URL slug of the project.
 * @param title
 *            human-readable name of the project.
 * @param roles
 *            roles held in this project.
 */
public record RProject( //
        @Schema(description = """
                Numeric project identifier. Can be used in place of the slug when addressing the
                project in API paths.
                """) //
        long id, //

        @Deprecated //
        @Schema(description = """
                URL slug of the project. Deprecated - use `slug` instead. Note that this field
                carries the slug and not the human-readable project name which is available as
                `title`.
                """) //
        String name, //

        @Schema(description = """
                URL slug of the project. Can be used in place of the numeric ID when addressing
                the project in API paths.
                """) //
        String slug, //

        @Schema(description = """
                Human-readable name of the project.
                """) //
        String title, //

        @Schema(description = """
                Roles held in this project (non-AERO). When listing or reading projects, these are the
                roles of the authenticated user - never those of any other user. When creating or
                importing a project, these are instead the roles of the user the project was created
                for, since an administrator acting on behalf of somebody else does not receive any
                roles. The list may be empty, e.g. for an administrator who can access a project
                without holding any explicitly assigned roles in it.
                """, //
                allowableValues = {
                        "ANNOTATOR", "CURATOR", "MANAGER" }, //
                extensions = @Extension( //
                        properties = @ExtensionProperty(name = "x-aero", value = "false"))) //
        List<String> roles) {
    /**
     * Sorts by the natural order of the enum so that the roles are reported in a stable order
     * irrespective of how they were assigned.
     */
    private static List<String> toRoleNames(Collection<PermissionLevel> aRoles)
    {
        if (aRoles == null) {
            return emptyList();
        }

        return aRoles.stream() //
                .sorted() //
                .map(PermissionLevel::name) //
                .collect(toCollection(ArrayList::new));
    }

    public static Builder builder()
    {
        return new Builder();
    }

    public static final class Builder
    {
        private long id;
        private String name;
        private String slug;
        private String title;
        private List<String> roles = new ArrayList<>();

        private Builder()
        {
        }

        public Builder withProject(Project aProject)
        {
            id = aProject.getId();
            name = aProject.getSlug();
            slug = aProject.getSlug();
            title = aProject.getName();
            return this;
        }

        public Builder withId(long aId)
        {
            id = aId;
            return this;
        }

        public Builder withName(String aName)
        {
            name = aName;
            return this;
        }

        public Builder withSlug(String aSlug)
        {
            slug = aSlug;
            return this;
        }

        public Builder withTitle(String aTitle)
        {
            title = aTitle;
            return this;
        }

        public Builder withRoles(List<String> aRoles)
        {
            roles.clear();
            if (aRoles != null) {
                roles.addAll(aRoles);
            }

            return this;
        }

        /**
         * Set the roles from the permission levels held in the project. The roles are reported in a
         * stable order irrespective of the order in which they were assigned.
         */
        public Builder withRoles(Collection<PermissionLevel> aRoles)
        {
            return withRoles(toRoleNames(aRoles));
        }

        public RProject build()
        {
            return new RProject(id, name, slug, title, roles);
        }
    }
}
