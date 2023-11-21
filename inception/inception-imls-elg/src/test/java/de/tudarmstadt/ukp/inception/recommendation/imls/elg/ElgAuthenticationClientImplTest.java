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
package de.tudarmstadt.ukp.inception.recommendation.imls.elg;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import de.tudarmstadt.ukp.inception.recommendation.imls.elg.client.ElgAuthenticationClient;
import de.tudarmstadt.ukp.inception.recommendation.imls.elg.client.ElgAuthenticationClientImpl;

public class ElgAuthenticationClientImplTest
{
    private ElgAuthenticationClient sut;

    @BeforeEach
    public void setup()
    {
        sut = new ElgAuthenticationClientImpl();
    }

    @Disabled("Would need an actual token")
    @Test
    public void testGetToken() throws Exception
    {
        // Get a code here:
        // https://live.european-language-grid.eu/auth/realms/ELG/protocol/openid-connect/auth?client_id=elg-oob&redirect_uri=urn:ietf:wg:oauth:2.0:oob&response_type=code&scope=offline_access
        var response = sut.getToken("hahaha");

        System.out.println(response);
    }
}
