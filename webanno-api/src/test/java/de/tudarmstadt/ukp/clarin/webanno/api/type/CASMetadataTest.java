/*
 * Copyright 2018
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
 */
package de.tudarmstadt.ukp.clarin.webanno.api.type;

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.uima.fit.factory.TypeSystemDescriptionFactory;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.junit.Test;

public class CASMetadataTest
{
    @Test
    public void thatCASMetadataIsNotAutodetected() throws Exception
    {
        TypeSystemDescription tsd = TypeSystemDescriptionFactory.createTypeSystemDescription();
        
        assertThat(tsd.getType(CASMetadata.class.getName()))
                .as("CASMetadata is detected by uimaFIT").isNull();
    }
}
