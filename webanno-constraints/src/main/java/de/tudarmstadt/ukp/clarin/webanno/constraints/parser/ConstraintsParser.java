/*
 * Copyright 2020
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
package de.tudarmstadt.ukp.clarin.webanno.constraints.parser;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import de.tudarmstadt.ukp.clarin.webanno.constraints.grammar.ConstraintsGrammar;
import de.tudarmstadt.ukp.clarin.webanno.constraints.grammar.ParseException;
import de.tudarmstadt.ukp.clarin.webanno.constraints.model.ParsedConstraints;
import de.tudarmstadt.ukp.clarin.webanno.constraints.visitor.ParserVisitor;

public class ConstraintsParser
{
    public static ParsedConstraints parseFile(String aFile) throws ParseException, IOException
    {
        return parse(new File(aFile));
    }

    public static ParsedConstraints parse(File aFile) throws ParseException, IOException
    {
        try (FileInputStream is = new FileInputStream(aFile)) {
            return parse(is);
        }
    }
    
    public static ParsedConstraints parse(InputStream aInputStream) throws ParseException
    {
        return parse(aInputStream, "UTF-8");
    }

    public static ParsedConstraints parse(InputStream aInputStream, String aEncoding)
        throws ParseException
    {
        ConstraintsGrammar parser = new ConstraintsGrammar(aInputStream, aEncoding);
        return parser.CLParse().accept(new ParserVisitor());
    }
}
