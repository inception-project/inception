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
package de.tudarmstadt.ukp.inception.support.wicket;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;

import org.apache.commons.io.IOUtils;
import org.apache.wicket.Application;
import org.apache.wicket.util.resource.AbstractResourceStream;
import org.apache.wicket.util.resource.ResourceStreamNotFoundException;

public class TempFileResource
    extends AbstractResourceStream
{
    private static final long serialVersionUID = -5472036265926982604L;

    private final DataSupplier supplier;

    private File tempFile;
    private FileInputStream is;

    public TempFileResource(DataSupplier aSupplier)
    {
        supplier = aSupplier;
    }

    private void deleteTempFile()
    {
        // Remove the temporary file
        if (tempFile != null) {
            tempFile.delete();
        }
    }

    @Override
    public InputStream getInputStream() throws ResourceStreamNotFoundException
    {
        // Write the data to the temporary file
        try {
            tempFile = File.createTempFile("inception-temp", "");
            var app = Application.get();
            if (app != null) {
                app.getResourceSettings().getFileCleaner().track(tempFile, this);
            }
            tempFile.deleteOnExit();

            try (FileOutputStream os = new FileOutputStream(tempFile)) {
                supplier.write(os);
            }
        }
        catch (IOException e) {
            deleteTempFile();
            throw new ResourceStreamNotFoundException(e);
        }

        // Return a stream for the file
        try {
            is = new FileInputStream(tempFile);
            return is;
        }
        catch (Exception e) {
            IOUtils.closeQuietly(is);
            deleteTempFile();
            throw new ResourceStreamNotFoundException(e);
        }
    }

    @Override
    public void close() throws IOException
    {
        IOUtils.closeQuietly(is);
        deleteTempFile();
    }

    @FunctionalInterface
    public interface DataSupplier
        extends Serializable
    {
        void write(OutputStream aOS) throws IOException;
    }
}
