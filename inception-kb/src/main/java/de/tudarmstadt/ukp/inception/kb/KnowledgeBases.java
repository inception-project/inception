/*
 * Copyright 2017
 * Ubiquitous Knowledge Processing (UKP) Lab
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
package de.tudarmstadt.ukp.inception.kb;

import java.util.HashMap;
import java.util.Map;

public class KnowledgeBases {
    
    public static final Map<String, String> KNOWLEDGE_BASES;
    
    static {
        KNOWLEDGE_BASES = new HashMap<>();
        KNOWLEDGE_BASES.put("British Museum", "http://collection.britishmuseum.org/sparql");
        KNOWLEDGE_BASES.put("YAGO", "https://linkeddata1.calcul.u-psud.fr/sparql");
        KNOWLEDGE_BASES.put("DBpedia", "http://dbpedia.org/sparql");
        KNOWLEDGE_BASES.put("BabelNet", "http://babelnet.org/sparql/");
    }
}
