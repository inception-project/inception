/*
 * Copyright 2014
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
package de.tudarmstadt.ukp.clarin.webanno.conll;

import static de.tudarmstadt.ukp.dkpro.core.testing.IOTestRunner.testOneWay;

import org.junit.Rule;
import org.junit.Test;

import de.tudarmstadt.ukp.dkpro.core.testing.DkproTestContext;

public class Conll2009ReaderWriterTest
{
    @Test
    public void test()
        throws Exception
    {
        testOneWay(Conll2009Reader.class, Conll2009Writer.class, "conll/2009/en-ref.conll",
                "conll/2009/en-orig.conll");
    }

    @Rule
    public DkproTestContext testContext = new DkproTestContext();
}
