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
package de.tudarmstadt.ukp.inception.io.brat;

import static org.apache.uima.fit.factory.CollectionReaderFactory.createReaderDescription;

import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.TypeSystemDescription;

import de.tudarmstadt.ukp.clarin.webanno.api.format.FormatSupport;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.inception.io.brat.config.BratAutoConfiguration;
import de.tudarmstadt.ukp.inception.io.brat.dkprocore.BratReader;

/**
 * Support for brat format.
 * <p>
 * This class is exposed as a Spring Component via
 * {@link BratAutoConfiguration#bratBasicFormatSupport}.
 * </p>
 */
public class BratBasicFormatSupport
    implements FormatSupport
{
    public static final String ID = "bratBasic";
    public static final String NAME = "brat basic (experimental)";

    @Override
    public String getId()
    {
        return ID;
    }

    @Override
    public String getName()
    {
        return NAME;
    }

    @Override
    public boolean isReadable()
    {
        return true;
    }

    @Override
    public CollectionReaderDescription getReaderDescription(Project aProject,
            TypeSystemDescription aTSD)
        throws ResourceInitializationException
    {
        var mapping = """
                {
                  'textTypeMapppings': [
                    {
                      'from': '.*',
                      'to': 'custom.Span',
                      'defaultFeatureValues': { }
                    }
                  ],
                  'relationTypeMapppings': [
                    {
                      'from': '.*',
                      'to': 'custom.Relation',
                      'defaultFeatureValues': { }
                    }
                  ],
                  'spans': [
                    {
                      'type': 'custom.Span',
                      'subCatFeature': 'label',
                      'defaultFeatureValues': { }
                    }
                  ],
                  'relations': [
                    {
                      'type': 'custom.Relation',
                      'arg1': 'Governor',
                      'arg2': 'Dependent',
                      'flags2': 'A',
                      'subCatFeature': 'label',
                      'defaultFeatureValues': { }
                    }
                  ],
                  'comments': [ ]
                }
                """;

        return createReaderDescription(BratReader.class, aTSD, //
                BratReader.PARAM_MAPPING, mapping, //
                BratReader.PARAM_LENIENT, true);
    }
}
