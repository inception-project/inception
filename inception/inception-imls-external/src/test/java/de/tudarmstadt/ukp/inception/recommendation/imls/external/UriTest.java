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
package de.tudarmstadt.ukp.inception.recommendation.imls.external;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;

import org.junit.jupiter.api.Test;

public class UriTest
{
    @Test
    public void thatUriResolvingWorksAsExpected()
    {
        assertThat(URI.create("http://this.is/a/test/foo").resolve("bar"))
                .isEqualTo(URI.create("http://this.is/a/test/bar"));
        assertThat(URI.create("http://this.is/a/test/foo").resolve("./bar"))
                .isEqualTo(URI.create("http://this.is/a/test/bar"));
        assertThat(URI.create("http://this.is/a/test/foo").resolve("/bar"))
                .isEqualTo(URI.create("http://this.is/bar"));
        assertThat(URI.create("http://this.is/a/test/foo/").resolve("/bar"))
                .isEqualTo(URI.create("http://this.is/bar"));
        assertThat(URI.create("http://this.is/a/test/foo/").resolve("bar"))
                .isEqualTo(URI.create("http://this.is/a/test/foo/bar"));
        assertThat(URI.create("http://this.is/a/test/foo//").resolve("bar"))
                .isEqualTo(URI.create("http://this.is/a/test/foo/bar"));
        assertThat(URI.create("http://this.is/a/test/foo//").resolve("bar").toString())
                .as("This is most important to use because it means that paths are normalized!")
                .isEqualTo("http://this.is/a/test/foo/bar");
    }
}
