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

import static de.tudarmstadt.ukp.clarin.webanno.constraints.grammar.ConstraintsParser.parse;
import static org.assertj.core.api.Assertions.assertThat;

import org.apache.commons.lang3.StringUtils;
import org.apache.uima.fit.factory.JCasFactory;
import org.junit.jupiter.api.Test;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Lemma;

public class UnicodeRulesTest
{
    private static final String[] texts = { "This is a test.", // English
            "ይህ ፈተና ነው ፡፡", // Amharic
            "هذا اختبار.", // Arabic
            "Սա թեստ է.", // Armenian
            "এটা একটা পরীক্ষা.", // Bangla
            "Гэта тэст.", // Belarusian
            "Това е тест.", // Bulgarian
            "ဒါကစမ်းသပ်မှုတစ်ခု", // Burmese
            "Això és un test.", // Catalan
            "这是一个测试。", // Chinese (simplified)
            "這是一個測試。", // Chinese (traditional)
            "Ეს ტესტია.", // Georgian
            "Αυτό είναι ένα τεστ.", // Greek
            "આ એક કસોટી છે.", // Gujarati
            "זה מבחן.", // Hebrew
            "यह एक परीक्षण है।", // Hindi
            "Þetta er próf.", // Icelandic
            "🤷‍♀️" // Emoji
    };

    @Test
    public void thatRulesMatchUnicodeCharacters() throws Exception
    {
        var jcas = JCasFactory.createJCas();

        for (var text : texts) {
            jcas.reset();
            jcas.setDocumentText(text);

            var constraints = parse(String.join("\n",
                    "import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Lemma as Lemma;",
                    "Lemma {", "  text() = \"" + text + "\" -> value=\"ok\";", "}"));

            var ann = new Lemma(jcas, 0, jcas.getDocumentText().length());
            ann.addToIndexes();

            var evaluator = new ConstraintsEvaluator();
            var candidates = evaluator.generatePossibleValues(constraints, ann, "value");

            assertThat(candidates).containsExactly(new PossibleValue("ok", false));
        }
    }

    @Test
    public void thatRulesCanAlsoNotMatch() throws Exception
    {
        var jcas = JCasFactory.createJCas();

        for (var text : texts) {
            jcas.reset();
            jcas.setDocumentText(text);

            var constraints = parse(String.join("\n",
                    "import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Lemma as Lemma;",
                    "Lemma {", "  text() = \"" + StringUtils.reverse(text) + "\" -> value=\"ok\";",
                    "}"));

            var ann = new Lemma(jcas, 0, jcas.getDocumentText().length());
            ann.addToIndexes();

            var evaluator = new ConstraintsEvaluator();
            var candidates = evaluator.generatePossibleValues(constraints, ann, "value");

            assertThat(candidates).isEmpty();
        }
    }
}
