/*
 * Copyright 2017
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
package de.tudarmstadt.ukp.clarin.webanno.brat.render.model;

import static de.tudarmstadt.ukp.clarin.webanno.brat.render.BratRenderer.abbreviate;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class LayerNameAbbreviationTest
{
    @Test
    public void test()
    {
        assertEquals("Dep...", abbreviate("Dependency"));
        assertEquals("MorFea...", abbreviate("Morphological features"));
        assertEquals("SemArg...", abbreviate("Semantic argument"));
        assertEquals("OneTwoThr...", abbreviate("One two three"));
        assertEquals("Foo of Lala", abbreviate("Foo of Lala"));
    }
}
