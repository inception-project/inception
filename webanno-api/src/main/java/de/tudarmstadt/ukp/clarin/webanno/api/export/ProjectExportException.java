/*
 * Copyright 2014
 * FG Language Technology and Ubiquitous Knowledge Processing (UKP) Lab
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

package de.tudarmstadt.ukp.clarin.webanno.api.export;

/**
 * Exception thrown while exporting the whole project
 */
public class ProjectExportException
    extends Exception
{
    private static final long serialVersionUID = -4242850260003049868L;

    public ProjectExportException(String message)
    {
        super(message);
    }

    public ProjectExportException(String aMessage, Throwable aCause)
    {
        super(aMessage, aCause);
    }

    public ProjectExportException(Throwable aCause)
    {
        super(aCause);
    }
}
