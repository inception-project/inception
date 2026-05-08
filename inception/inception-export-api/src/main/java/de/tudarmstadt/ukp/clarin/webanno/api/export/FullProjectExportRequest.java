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
package de.tudarmstadt.ukp.clarin.webanno.api.export;

import de.tudarmstadt.ukp.clarin.webanno.api.format.FormatSupport;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;

public class FullProjectExportRequest
    extends ProjectExportRequest_ImplBase
{
    private static final long serialVersionUID = -7010995651575991241L;

    private static final String FILENAME_PREFIX = "project";
    public static final String FORMAT_AUTO = "AUTO";

    private String title;
    private String format;
    private boolean includeInProgress;
    private boolean obfuscate;

    private FullProjectExportRequest(Builder aBuilder)
    {
        super(aBuilder.project);
        title = aBuilder.title;
        format = aBuilder.format;
        includeInProgress = aBuilder.includeInProgress;
        obfuscate = aBuilder.obfuscate;
    }

    /**
     * @param aFormat
     *            the ID of the export format.
     * 
     * @see FormatSupport#getId()
     */
    public void setFormat(String aFormat)
    {
        format = aFormat;
    }

    /**
     * @return the ID of the export format.
     * 
     * @see FormatSupport#getId()
     */
    public String getFormat()
    {
        return format;
    }

    public boolean isIncludeInProgress()
    {
        return includeInProgress;
    }

    public boolean isObfuscate()
    {
        return obfuscate;
    }

    @Override
    public String getFilenamePrefix()
    {
        return FILENAME_PREFIX;
    }

    @Override
    public String getTitle()
    {
        var sb = new StringBuilder(title);
        if (format != null) {
            sb.append(" (" + format + ")");
        }
        return sb.toString();
    }

    public static Builder builder()
    {
        return new Builder();
    }

    public static final class Builder
    {
        private Project project;
        private String title = "Project export";
        private String format;
        private boolean includeInProgress;
        private boolean obfuscate;

        private Builder()
        {
        }

        public Builder withProject(Project aProject)
        {
            project = aProject;
            return this;
        }

        public Builder withTitle(String aTitle)
        {
            title = aTitle;
            return this;
        }

        public Builder withFormat(String aFormat)
        {
            format = aFormat;
            return this;
        }

        public Builder withIncludeInProgress(boolean aIncludeInProgress)
        {
            includeInProgress = aIncludeInProgress;
            return this;
        }

        public Builder withObfuscate(boolean aObfuscate)
        {
            obfuscate = aObfuscate;
            return this;
        }

        public FullProjectExportRequest build()
        {
            return new FullProjectExportRequest(this);
        }
    }
}
