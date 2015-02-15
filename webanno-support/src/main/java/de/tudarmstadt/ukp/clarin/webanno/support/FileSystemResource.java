/*******************************************************************************
 * Copyright 2015
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
 ******************************************************************************/
package de.tudarmstadt.ukp.clarin.webanno.support;

import java.io.File;

import org.apache.wicket.request.resource.IResource;
import org.apache.wicket.request.resource.ResourceStreamResource;
import org.apache.wicket.util.resource.FileResourceStream;

public class FileSystemResource
    implements IResource
{
    private static final long serialVersionUID = 6435457858590524482L;

    private final File file;

    public FileSystemResource(File aFile)
    {
        file = aFile;
    }

    public void respond(Attributes attributes)
    {
        FileResourceStream fileResourceStream = new FileResourceStream(file);
        ResourceStreamResource resource = new ResourceStreamResource(fileResourceStream);
        resource.respond(attributes);
    }
}