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
package de.tudarmstadt.ukp.inception.documents;

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.wicket.validation.ValidationError;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DocumentServiceImplTest
{
    DocumentServiceImpl sut;

    @BeforeEach
    void setup()
    {
        sut = new DocumentServiceImpl(null, null, null, null, null, null, null);
    }

    @Test
    void testDocumentNameValidationErrorMessages()
    {
        assertThat(sut.validateDocumentName("")) //
                .hasSize(1) //
                .extracting(ValidationError::getMessage).first().asString() //
                .contains("empty");

        assertThat(sut.validateDocumentName(" ")) //
                .hasSize(1) //
                .extracting(ValidationError::getMessage).first().asString() //
                .contains("empty");

        assertThat(sut.validateDocumentName(" john")) //
                .hasSize(1) //
                .extracting(ValidationError::getMessage).first().asString() //
                .contains("start or end with whitespace");

        assertThat(sut.validateDocumentName("john ")) //
                .hasSize(1) //
                .extracting(ValidationError::getMessage).first().asString() //
                .contains("start or end with whitespace");

        assertThat(sut.validateDocumentName("jo\thn")) //
                .hasSize(1) //
                .extracting(ValidationError::getMessage).first().asString() //
                .contains("cannot contain any control characters");

        assertThat(sut.validateDocumentName("john\n")) //
                .hasSize(1) //
                .extracting(ValidationError::getMessage).first().asString() //
                .contains("start or end with whitespace");

        assertThat(sut.validateDocumentName("john\0")) //
                .hasSize(1) //
                .extracting(ValidationError::getMessage).first().asString() //
                .contains("cannot contain any control characters");

        assertThat(sut.validateDocumentName("john\u001B")) //
                .hasSize(1) //
                .extracting(ValidationError::getMessage).first().asString() //
                .contains("control characters");

        assertThat(sut.validateDocumentName("loveme".repeat(2000))) //
                .hasSize(1) //
                .extracting(ValidationError::getMessage).first().asString() //
                .contains("too long");

        assertThat(sut.validateDocumentName("/etc/passwd")) //
                .hasSize(1) //
                .extracting(ValidationError::getMessage).first().asString() //
                .contains("contain any of the following characters");

        assertThat(sut.validateDocumentName("../../bomb.zip")) //
                .hasSize(1) //
                .extracting(ValidationError::getMessage).first().asString() //
                .contains("start with any of these characters");

        assertThat(sut.validateDocumentName(".hidden")) //
                .hasSize(1) //
                .extracting(ValidationError::getMessage).first().asString() //
                .contains("start with any of these characters");
    }
}
