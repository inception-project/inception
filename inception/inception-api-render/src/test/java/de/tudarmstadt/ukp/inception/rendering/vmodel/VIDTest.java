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
package de.tudarmstadt.ukp.inception.rendering.vmodel;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class VIDTest
{
    @Test
    public void test()
    {
        assertEquals("10", new VID(10).toString());
        assertEquals("10.1", new VID(10, 1).toString());
        assertEquals("10.1.2", new VID(10, 1, 2).toString());
        assertEquals("10-1.2.3", new VID(10, 1, 2, 3).toString());
        assertEquals("ext:10-1.2.3", new VID(10, "ext", "1.2.3").toString());
        assertEquals("ext:10-1.2.3@1", new VID(10, "ext", "1.2.3@1").toString());

        assertEquals(VID.NONE_ID.toString(), VID.parse(VID.NONE_ID.toString()).toString());
        assertEquals("10", VID.parse("10").toString());
        assertEquals("10.1", VID.parse("10.1").toString());
        assertEquals("10.1.2", VID.parse("10.1.2").toString());
        assertEquals("10-1.2.3", VID.parse("10-1.2.3").toString());
        assertEquals("ext:10-1.2.3", VID.parse("ext:10-1.2.3").toString());
        assertEquals("ext:10-1.2.3@1", VID.parse("ext:10-1.2.3@1").toString());
    }

    @Test
    public void testParse()
    {
        assertParseVid(null, -1, 10, -1, -1, -1, "10", null);
        assertParseVid(null, -1, 10, -1, 1, -1, "10.1", null);
        assertParseVid(null, -1, 10, -1, 1, 2, "10.1.2", null);
        assertParseVid(null, -1, 10, 1, 2, 3, "10-1.2.3", null);
        assertParseVid("ext", -1, 10, -1, -1, -1, "ext:10-user", "user");
        assertParseVid("ext", -1, 10, -1, -1, -1, "ext:10-1.2.3", "1.2.3");
        assertParseVid("ext", -1, 10, -1, -1, -1, "ext:10-1.2.3@1", "1.2.3@1");
    }

    private void assertParseVid(String aExtensionId, int aLayerId, int aAnnotationID,
            int aSubAnnotationId, int aAttribute, int aSlot, String aVID, String aExtensionPayload)
    {
        VID a = VID.parse(aVID);
        assertEquals(aExtensionId, a.getExtensionId());
        assertEquals(aExtensionPayload, a.getExtensionPayload());
        assertEquals(aLayerId, a.getLayerId());
        assertEquals(aAnnotationID, a.getId());
        assertEquals(aSubAnnotationId, a.getSubId());
        assertEquals(aAttribute, a.getAttribute());
        assertEquals(aSlot, a.getSlot());
    }
}
