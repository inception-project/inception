/*
 * Copyright 2019
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 * 
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
package de.tudarmstadt.ukp.inception.ui.curation.sidebar;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import de.tudarmstadt.ukp.inception.rendering.vmodel.VID;
import de.tudarmstadt.ukp.inception.ui.curation.sidebar.render.CurationVID;

public class CurationVIDTest
{
    @Test
    public void testParse()
    {
        assertParseVid(VID.parse("cur:10-kevin!10"), //
                "cur", "kevin", "10", -1, 10, -1, -1, -1);

        assertParseVid(VID.parse("cur:10-kevin!10.1"), //
                "cur", "kevin", "10.1", -1, 10, -1, 1, -1);

        assertParseVid(VID.parse("cur:10-kevin!10.1.2"), //
                "cur", "kevin", "10.1.2", -1, 10, -1, 1, 2);

        assertParseVid(VID.parse("cur:10-kevin!10-1.2.3"), //
                "cur", "kevin", "10-1.2.3", -1, 10, 1, 2, 3);

        assertParseVid(VID.parse("cur:10-kevin!10-1.2.3@1"), //
                "cur", "kevin", "10-1.2.3@1", 1, 10, 1, 2, 3);
    }

    private void assertParseVid(VID aVID, String aExtensionId, String aUsername, String aPayload,
            int aLayerId, int aAnnotationID, int aSubAnnotationId, int aAttribute, int aSlot)
    {
        var a = CurationVID.parse(aVID.getExtensionPayload());
        var b = VID.parse(a.getExtensionPayload());

        assertThat(a.getExtensionId()).isEqualTo(aExtensionId);
        assertThat(a.getExtensionPayload()).isEqualTo(aPayload);
        assertThat(a.getUsername()).isEqualTo(aUsername);

        assertThat(b.getLayerId()).isEqualTo(aLayerId);
        assertThat(b.getId()).isEqualTo(aAnnotationID);
        assertThat(b.getSubId()).isEqualTo(aSubAnnotationId);
        assertThat(b.getAttribute()).isEqualTo(aAttribute);
        assertThat(b.getSlot()).isEqualTo(aSlot);
    }
}
