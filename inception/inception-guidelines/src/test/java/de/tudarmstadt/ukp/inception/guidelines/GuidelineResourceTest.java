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

import static de.tudarmstadt.ukp.inception.support.http.HttpHeaderConstants.NOSNIFF;
import static de.tudarmstadt.ukp.inception.support.http.HttpHeaderConstants.X_CONTENT_TYPE_OPTIONS;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.wicket.request.resource.ContentDisposition.ATTACHMENT;
import static org.apache.wicket.request.resource.ContentDisposition.INLINE;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.nio.file.Files;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GuidelineResourceTest
{
    private @TempDir File tempDir;

    private File write(String aName, String aContent) throws Exception
    {
        var file = new File(tempDir, aName);
        Files.write(file.toPath(), aContent.getBytes(UTF_8));
        return file;
    }

    @Test
    void thatPdfIsServedInline() throws Exception
    {
        var response = new GuidelineResource(write("a.pdf", "%PDF-1.4\n"))
                .newResourceResponse(null);

        assertThat(response.getContentType()).isEqualTo("application/pdf");
        assertThat(response.getContentDisposition()).isEqualTo(INLINE);
        assertThat(response.getHeaders().getHeader(X_CONTENT_TYPE_OPTIONS)).contains(NOSNIFF);
    }

    @Test
    @SuppressWarnings("java:S5961")
    void thatNonPdfIsServedAsAttachment() throws Exception
    {
        for (var name : new String[] { "a.html", "a.svg", "a.txt", "a.pdf" }) {
            var file = write(name, "<html><script>alert(1)</script></html>");

            var response = new GuidelineResource(file).newResourceResponse(null);

            assertThat(response.getContentType()).as(name).isEqualTo("application/octet-stream");
            assertThat(response.getContentDisposition()).as(name).isEqualTo(ATTACHMENT);
            assertThat(response.getHeaders().getHeader(X_CONTENT_TYPE_OPTIONS)).as(name)
                    .contains(NOSNIFF);
        }
    }

    @Test
    void thatMarkerNotAtByteZeroIsServedAsAttachment() throws Exception
    {
        var file = write("a.pdf", "<html></html>\n%PDF-1.4\n");

        var response = new GuidelineResource(file).newResourceResponse(null);

        assertThat(response.getContentDisposition()).isEqualTo(ATTACHMENT);
    }

    @Test
    void thatMissingFileIsNotFound()
    {
        var response = new GuidelineResource(new File(tempDir, "missing.pdf"))
                .newResourceResponse(null);

        assertThat(response.getStatusCode()).isEqualTo(404);
        assertThat(response.getHeaders().getHeader(X_CONTENT_TYPE_OPTIONS)).contains(NOSNIFF);
    }
}
