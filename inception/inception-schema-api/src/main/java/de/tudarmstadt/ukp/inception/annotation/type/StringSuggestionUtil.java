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
package de.tudarmstadt.ukp.inception.annotation.type;

import static java.util.Arrays.asList;
import static org.apache.uima.fit.util.FSUtil.getFeature;
import static org.apache.uima.fit.util.FSUtil.setFeature;

import java.util.ArrayList;
import java.util.List;

import org.apache.uima.cas.AnnotationBaseFS;

import de.tudarmstadt.ukp.inception.rendering.editorstate.SuggestionState;

public final class StringSuggestionUtil
{
    private StringSuggestionUtil()
    {
        // No instances
    }

    public static void setStringSuggestions(AnnotationBaseFS aAnnotation, String aFeature,
            List<SuggestionState> aSuggestions)
    {
        set(aAnnotation, aFeature, aSuggestions, false);

    }

    public static void appendStringSuggestions(AnnotationBaseFS aAnnotation, String aFeature,
            List<SuggestionState> aSuggestions)
    {
        set(aAnnotation, aFeature, aSuggestions, true);
    }

    private static void set(AnnotationBaseFS aAnnotation, String aFeature,
            List<SuggestionState> aSuggestions, boolean aAppend)
    {
        var cas = aAnnotation.getCAS();

        var stringSuggestions = new ArrayList<StringSuggestion>();

        if (aAppend) {
            var existingStringSuggestions = getFeature(aAnnotation, aFeature,
                    StringSuggestion[].class);
            if (existingStringSuggestions != null) {
                stringSuggestions.addAll(asList(existingStringSuggestions));
            }
        }

        for (var suggestion : aSuggestions) {
            var recommenderDecl = RecommenderDeclUtil.getOrCreateRecommenderDecl(cas,
                    suggestion.recommender());

            var stringSuggestion = new StringSuggestion(cas.getJCasImpl());
            stringSuggestion.setLabel((String) suggestion.value());
            stringSuggestion.setScore(suggestion.score());
            stringSuggestion.setRecommender(recommenderDecl);
            stringSuggestions.add(stringSuggestion);
        }

        setFeature(aAnnotation, aFeature, stringSuggestions);
    }
}
