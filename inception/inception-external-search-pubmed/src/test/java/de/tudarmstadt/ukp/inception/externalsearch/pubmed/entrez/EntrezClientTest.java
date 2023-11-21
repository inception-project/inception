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
package de.tudarmstadt.ukp.inception.externalsearch.pubmed.entrez;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("slow")
class EntrezClientTest
{
    private EntrezClient sut;

    @BeforeEach
    public void setup() throws InterruptedException
    {
        Thread.sleep(1000); // Get around API rate limiting
        sut = new EntrezClient();
    }

    @Test
    public void thatESearchWorks() throws Exception
    {
        var results = sut.esearch("pmc", "asthma", 0, 10);

        // System.out.println(results);

        assertThat(results.getIdList()).isNotEmpty();
    }

    @Test
    public void thatEsummaryWorks() throws Exception
    {
        var results = sut.esummary("pmc", 6678417, 9507199);

        // System.out.println(results);

        assertThat(results.getDocSumaries()).isNotEmpty();
    }
}
