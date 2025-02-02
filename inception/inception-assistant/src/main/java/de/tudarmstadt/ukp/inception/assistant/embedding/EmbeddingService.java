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
package de.tudarmstadt.ukp.inception.assistant.embedding;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import org.apache.commons.lang3.tuple.Pair;

public interface EmbeddingService
{
    int getDimension();

    Optional<float[]> embed(String aQuery) throws IOException;

    List<Pair<String, float[]>> embed(String... aStrings) throws IOException;

    <T> List<Pair<T, float[]>> embed(Function<T, String> aExtractor, Iterable<T> aObjects)
        throws IOException;
}
