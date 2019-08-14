/*
 * Copyright 2019
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.inception.curation;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.VID;

public class CurationVIDTest
{
    private CurationEditorExtension extension;
    
    @Before
    public void setup() {
        extension = new CurationEditorExtension();
    }
    
    @Test
    public void testParse()
    {
        assertParseVid(VID.parse("ext:kevin:10"),  "ext", -1, 10, -1, -1, -1, "kevin", "kevin:10");
        assertParseVid(VID.parse("ext:kevin:10.1"), "ext", -1, 10, -1,  1, -1, "kevin", "kevin:10.1");
        assertParseVid(VID.parse("ext:kevin:10.1.2"), "ext",  -1, 10, -1,  1,  2, "kevin", "kevin:10.1.2");
        assertParseVid(VID.parse("ext:kevin:10-1.2.3"), "ext",  -1, 10,  1,  2,  3, "kevin", "kevin:10-1.2.3");
        assertParseVid(VID.parse("ext:kevin:10-1.2.3@1"), "ext", 1, 10,  1,  2,  3, "kevin", "kevin:10-1.2.3@1");
    }

    private void assertParseVid(VID aVID, String aExtensionId, int aLayerId, int aAnnotationID,
            int aSubAnnotationId, int aAttribute, int aSlot, String aUsername,
            String aExtensionPayload)
    {
        VID a = extension.parse(aVID, aExtensionPayload);
        assertEquals(aExtensionId, a.getExtensionId());
        assertEquals(aExtensionPayload, a.getExtensionPayload());
        assertEquals(aUsername, ((CurationVID) a).getUsername());
        assertEquals(aLayerId, a.getLayerId());
        assertEquals(aAnnotationID, a.getId());
        assertEquals(aSubAnnotationId, a.getSubId());
        assertEquals(aAttribute, a.getAttribute());
        assertEquals(aSlot, a.getSlot());
    }
}
