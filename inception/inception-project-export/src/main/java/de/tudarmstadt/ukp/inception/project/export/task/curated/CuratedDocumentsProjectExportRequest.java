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
package de.tudarmstadt.ukp.inception.project.export.task.curated;

import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExportRequest_ImplBase;
import de.tudarmstadt.ukp.clarin.webanno.api.format.FormatSupport;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;

public class CuratedDocumentsProjectExportRequest
    extends ProjectExportRequest_ImplBase
{
    private static final long serialVersionUID = 4272079942793991783L;

    private static final String FILENAME_PREFIX = "curated-docs";
    public static final String FORMAT_AUTO = "AUTO";

    private String format;
    private boolean includeInProgress;
    private String filenameTag;

    public CuratedDocumentsProjectExportRequest(Project aProject)
    {
        super(aProject);
    }

    /**
     * Set the ID of the export format.
     * 
     * @param aFormat
     *            the format ID
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
        StringBuilder sb = new StringBuilder("Curated documents");
        if (format != null) {
            sb.append(" (" + format + ")");
        }
        return sb.toString();
    }
}
