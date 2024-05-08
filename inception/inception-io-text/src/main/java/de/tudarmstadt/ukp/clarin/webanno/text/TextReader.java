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
package de.tudarmstadt.ukp.clarin.webanno.text;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.dkpro.core.api.resources.CompressionUtils.getInputStream;

import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.BOMInputStream;
import org.apache.uima.cas.CAS;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.descriptor.MimeTypeCapability;
import org.apache.uima.fit.descriptor.TypeCapability;
import org.dkpro.core.api.io.ResourceCollectionReaderBase;
import org.dkpro.core.api.parameter.MimeTypes;

/**
 * UIMA collection reader for plain text files.
 */
@MimeTypeCapability(MimeTypes.TEXT_PLAIN)
@TypeCapability(outputs = { "de.tudarmstadt.ukp.dkpro.core.api.metadata.type.DocumentMetaData" })
public class TextReader
    extends ResourceCollectionReaderBase
{
    /**
     * Whether to remove a byte-order mark from the start of the text.
     */
    public static final String PARAM_INCLUDE_BOM = "includeBom";
    @ConfigurationParameter(name = PARAM_INCLUDE_BOM, mandatory = true, defaultValue = "false")
    private boolean includeBom;

    @Override
    public void getNext(CAS aJCas) throws IOException, CollectionException
    {
        var res = nextFile();
        initCas(aJCas, res);

        try (var is = BOMInputStream.builder() //
                .setInclude(includeBom) //
                .setInputStream(getInputStream(res.getLocation(), res.getInputStream())) //
                .get()) {
            var text = IOUtils.toString(is, UTF_8);
            aJCas.setDocumentText(text);
        }
    }
}
