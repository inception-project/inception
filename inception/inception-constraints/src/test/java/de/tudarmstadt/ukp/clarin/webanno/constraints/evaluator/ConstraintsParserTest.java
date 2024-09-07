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
package de.tudarmstadt.ukp.clarin.webanno.constraints.evaluator;

import static java.util.Arrays.asList;

import java.io.File;
import java.io.FilenameFilter;

import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import de.tudarmstadt.ukp.clarin.webanno.constraints.grammar.ConstraintsParser;

public class ConstraintsParserTest
{

    @ParameterizedTest(name = "{index}: running on file {0}")
    @MethodSource("ruleFiles")
    public void thatRuleFileCanBeParsed(File aRuleFile) throws Exception
    {
        ConstraintsParser.parse(aRuleFile);
    }

    public static Iterable<File> ruleFiles()
    {
        return asList(new File("src/test/resources/rules/")
                .listFiles((FilenameFilter) new SuffixFileFilter(asList(".rules"))));
    }
}
