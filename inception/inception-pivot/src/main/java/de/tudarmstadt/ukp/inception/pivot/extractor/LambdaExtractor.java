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
package de.tudarmstadt.ukp.inception.pivot.extractor;

import java.io.Serializable;
import java.util.Optional;

import org.danekja.java.util.function.serializable.SerializableFunction;

import de.tudarmstadt.ukp.inception.pivot.api.extractor.Extractor;

public class LambdaExtractor<T, R extends Serializable>
    implements Extractor<T, R>
{
    private final String name;
    private final Class<T> type;
    private final Class<R> resultType;
    private final SerializableFunction<T, R> func;

    public LambdaExtractor(String aName, Class<T> aType, Class<R> aResultType,
            SerializableFunction<T, R> aFunc)
    {
        name = aName;
        func = aFunc;
        type = aType;
        resultType = aResultType;
    }

    @Override
    public Class<? extends R> getResultType()
    {
        return resultType;
    }

    @Override
    public boolean accepts(Object aSource)
    {
        return type.isInstance(aSource);
    }

    @Override
    public Optional<String> getTriggerType()
    {
        return Optional.of(type.getName());
    }

    @Override
    public R extract(T source)
    {
        return func.apply(source);
    }

    @Override
    public String getName()
    {
        return name;
    }
}
