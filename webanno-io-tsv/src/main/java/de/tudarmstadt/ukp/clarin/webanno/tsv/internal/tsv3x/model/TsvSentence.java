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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import de.tudarmstadt.ukp.clarin.webanno.tsv.internal.tsv3x.Tsv3XSerializer;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;

public class TsvSentence
{
    private final TsvDocument doc;
    private final Sentence uimaSentence;
    private final List<TsvToken> tokens = new ArrayList<>();
    private final int position;

    public TsvSentence(TsvDocument aDoc, Sentence aUimaSentence, int aPosition)
    {
        doc = aDoc;
        uimaSentence = aUimaSentence;
        position = aPosition;
    }

    public int getBegin()
    {
        return uimaSentence.getBegin();
    }

    public int getEnd()
    {
        return uimaSentence.getEnd();
    }

    public Sentence getUimaSentence()
    {
        return uimaSentence;
    }

    public TsvToken createToken(Token aUimaToken)
    {
        TsvToken token = doc.createToken(this, aUimaToken, tokens.size() + 1);
        token.addUimaAnnotation(aUimaToken);
        tokens.add(token);
        return token;
    }

    public List<TsvToken> getTokens()
    {
        return tokens;
    }

    public int getPosition()
    {
        return position;
    }

    @Override
    public String toString()
    {
        StringWriter buf = new StringWriter();
        try (PrintWriter out = new PrintWriter(buf)) {
            new Tsv3XSerializer().write(out, this,
                    doc.getSchema().getHeaderColumns(doc.getActiveColumns()));
        }
        return buf.toString();
    }
}
