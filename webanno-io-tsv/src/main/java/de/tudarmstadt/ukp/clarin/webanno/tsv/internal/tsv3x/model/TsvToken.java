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
package de.tudarmstadt.ukp.clarin.webanno.tsv.internal.tsv3x.model;

import java.util.ArrayList;
import java.util.List;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;

public class TsvToken
    extends TsvUnit
{
    private List<TsvSubToken> subTokens = new ArrayList<>();

    public TsvToken(TsvDocument aDoc, TsvSentence aSentence, Token aUimaToken, int aPosition)
    {
        super(aDoc, aSentence, aUimaToken, aPosition);
    }

    /**
     * Creates a new sub-token if there is not already a sub-token with the same offsets. Otherwise,
     * it returns the existing sub-token.
     * 
     * @param aBegin
     *            begin offset.
     * @param aEnd
     *            end offset.
     * @return the new sub-token.
     */
    public TsvSubToken createSubToken(int aBegin, int aEnd)
    {
        TsvSubToken subToken = new TsvSubToken(this, aBegin, aEnd);
        int existingIndex = subTokens.indexOf(subToken);
        if (existingIndex > -1) {
            subToken = subTokens.get(existingIndex);
        }
        else {
            subTokens.add(subToken);
        }

        return subToken;
    }

    public List<TsvSubToken> getSubTokens()
    {
        return subTokens;
    }
}
