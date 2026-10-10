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
package de.tudarmstadt.ukp.inception.guidelines;

import static java.nio.file.Files.newInputStream;
import static de.tudarmstadt.ukp.inception.support.http.HttpHeaderConstants.NOSNIFF;
import static de.tudarmstadt.ukp.inception.support.http.HttpHeaderConstants.X_CONTENT_TYPE_OPTIONS;
import static org.apache.wicket.request.resource.ContentDisposition.ATTACHMENT;
import static org.apache.wicket.request.resource.ContentDisposition.INLINE;
import static org.springframework.http.MediaType.APPLICATION_OCTET_STREAM_VALUE;
import static org.springframework.http.MediaType.APPLICATION_PDF_VALUE;

import java.io.File;
import java.io.IOException;

import org.apache.wicket.request.resource.AbstractResource;

/**
 * Serves a guideline file. Only files that really are PDFs are shown in the browser. Anything else
 * is sent as a download with a neutral type, so that the browser never renders it. The decision is
 * made when the file is requested, based on the content, never on the file name.
 */
public class GuidelineResource
    extends AbstractResource
{
    private static final long serialVersionUID = 6459872513300143286L;

    private final File file;

    public GuidelineResource(File aFile)
    {
        file = aFile;
    }

    @Override
    protected ResourceResponse newResourceResponse(Attributes aAttributes)
    {
        var response = new ResourceResponse();
        response.getHeaders().addHeader(X_CONTENT_TYPE_OPTIONS, NOSNIFF);

        if (!file.isFile()) {
            // Not setError(): Wicket would then not send the headers set above
            response.setStatusCode(404);
            return response;
        }

        var pdf = GuidelineFiles.isPdf(file);
        response.setContentType(pdf ? APPLICATION_PDF_VALUE : APPLICATION_OCTET_STREAM_VALUE);
        response.setContentDisposition(pdf ? INLINE : ATTACHMENT);
        response.setFileName(file.getName());
        response.setContentLength(file.length());
        response.disableCaching();
        response.setWriteCallback(new WriteCallback()
        {
            @Override
            public void writeData(Attributes aAttributes) throws IOException
            {
                try (var is = newInputStream(file.toPath())) {
                    writeStream(aAttributes, is);
                }
            }
        });

        return response;
    }
}
