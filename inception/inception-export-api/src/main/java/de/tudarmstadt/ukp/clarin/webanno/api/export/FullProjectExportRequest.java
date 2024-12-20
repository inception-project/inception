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
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState;

public class FullProjectExportRequest
    extends ProjectExportRequest_ImplBase
{
    private static final long serialVersionUID = -7010995651575991241L;

    private static final String FILENAME_PREFIX = "project";
    public static final String FORMAT_AUTO = "AUTO";

    private String format;
    private boolean includeInProgress;
    private String filenameTag;

    /**
     * Create a new project export request. Use this constructor if the project is not known yet or
     * may change. Make sure to set the project via the setter before starting the export.
     * 
     * @param aFormat
     *            the ID of the export format.
     * @param aIncludeInProgress
     *            whether to include documents that are
     *            {@link SourceDocumentState#CURATION_IN_PROGRESS}
     */
    public FullProjectExportRequest(String aFormat, boolean aIncludeInProgress)
    {
        super(null);
        format = aFormat;
        includeInProgress = aIncludeInProgress;
    }

    public FullProjectExportRequest(Project aProject, String aFormat, boolean aIncludeInProgress)
    {
        super(aProject);
        format = aFormat;
        includeInProgress = aIncludeInProgress;
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

    /**
     * @param aIncludeInProgress
     *            whether to include documents that are
     *            {@link SourceDocumentState#CURATION_IN_PROGRESS}
     */
    public void setIncludeInProgress(boolean aIncludeInProgress)
    {
        includeInProgress = aIncludeInProgress;
    }

    public boolean isIncludeInProgress()
    {
        return includeInProgress;
    }

    @Override
    public String getFilenamePrefix()
    {
        return FILENAME_PREFIX;
    }

    @Override
    public String getTitle()
    {
        StringBuilder sb = new StringBuilder("Project backup");
        if (format != null) {
            sb.append(" (" + format + ")");
        }
        return sb.toString();
    }
}
