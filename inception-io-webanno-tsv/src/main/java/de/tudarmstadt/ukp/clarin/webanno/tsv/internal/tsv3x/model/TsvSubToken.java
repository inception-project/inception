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

import org.apache.commons.lang3.Validate;

public class TsvSubToken
    extends TsvToken
{
    private final TsvToken token;
    private final int begin;
    private final int end;

    public TsvSubToken(TsvToken aToken, int aBegin, int aEnd)
    {
        super(aToken.getDocument(), aToken.getSentence(), aToken.getUimaToken(),
                aToken.getPosition());
        Validate.notNull(aToken, "Must specify a token");
        Validate.isTrue(aBegin >= 0, "Begin offset must be zero or positive: %d", aBegin);
        Validate.isTrue(aEnd >= 0, "End offset must be zero or positive: %d", aEnd);
        Validate.isTrue(aBegin <= aEnd,
                "End offset must be larger or equal to begin offset: [begin: %d, end: %d]", aBegin,
                aEnd);
        if (aToken.getUimaToken() != null
                && aToken.getUimaToken().getCAS().getDocumentText() != null) {
            int length = aToken.getUimaToken().getCAS().getDocumentText().length();
            Validate.isTrue(aBegin <= length, "Begin offset must be in document range [0-%d]): %d",
                    length, aBegin);
            Validate.isTrue(aEnd <= length, "End offset must be in document range [0-%d]): %d",
                    length, aBegin);
        }

        token = aToken;
        begin = aBegin;
        end = aEnd;
    }

    @Override
    public int getBegin()
    {
        return begin;
    }

    @Override
    public int getEnd()
    {
        return end;
    }

    @Override
    public String getId()
    {
        return String.format("%s.%d", token.getId(), token.getSubTokens().indexOf(this) + 1);
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + begin;
        result = prime * result + end;
        return result;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        TsvSubToken other = (TsvSubToken) obj;
        if (begin != other.begin) {
            return false;
        }
        if (end != other.end) {
            return false;
        }
        return true;
    }
}
