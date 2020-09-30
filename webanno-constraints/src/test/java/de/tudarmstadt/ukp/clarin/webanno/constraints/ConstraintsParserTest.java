/*
 * Copyright 2015
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
package de.tudarmstadt.ukp.clarin.webanno.constraints;

import static java.util.Arrays.asList;

import java.io.File;
import java.io.FilenameFilter;

import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import de.tudarmstadt.ukp.clarin.webanno.constraints.grammar.ConstraintsParser;

@RunWith(value = Parameterized.class)
public class ConstraintsParserTest
{
    @Parameters(name = "{index}: running on file {0}")
    public static Iterable<File> ruleFiles()
    {
        return asList(new File("src/test/resources/rules/")
                .listFiles((FilenameFilter) new SuffixFileFilter(asList(".rules"))));
    }

    private File ruleFile;

    public ConstraintsParserTest(File aRuleFile)
    {
        ruleFile = aRuleFile;
    }

    @Test
    public void thatRuleFileCanBeParsed() throws Exception
    {
        ConstraintsParser.parse(ruleFile);
    }


}
