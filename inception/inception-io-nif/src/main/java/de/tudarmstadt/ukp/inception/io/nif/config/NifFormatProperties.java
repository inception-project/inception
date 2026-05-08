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
package de.tudarmstadt.ukp.inception.io.nif.config;

public interface NifFormatProperties
{
    /**
     * @return IRI prefix used by the writer to mint a {@code taClassRef} target when the named
     *         entity {@code value} is not a valid IRI. Non-IRI values are URL-encoded and appended
     *         to this prefix. If {@code null} or empty, non-IRI values are skipped on export.
     */
    String getDefaultClassIri();

    /**
     * @return IRI prefix used by the writer to mint a {@code taIdentRef} target when the named
     *         entity {@code identifier} is not a valid IRI. Non-IRI values are URL-encoded and
     *         appended to this prefix. If {@code null} or empty, non-IRI values are skipped on
     *         export.
     */
    String getDefaultIdentifierIri();

    /**
     * @return IRI prefix the reader strips from {@code taClassRef} values before storing them as
     *         the named entity {@code value}. The remainder is URL-decoded. Enables a clean
     *         round-trip with the corresponding writer prefix.
     */
    String getStripClassIri();

    /**
     * @return IRI prefix the reader strips from {@code taIdentRef} values before storing them as
     *         the named entity {@code identifier}. The remainder is URL-decoded. Enables a clean
     *         round-trip with the corresponding writer prefix.
     */
    String getStripIdentifierIri();
}
