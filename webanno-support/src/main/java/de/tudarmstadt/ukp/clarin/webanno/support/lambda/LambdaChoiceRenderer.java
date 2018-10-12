/*
 * Copyright 2018
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
package de.tudarmstadt.ukp.clarin.webanno.support.lambda;

import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.danekja.java.util.function.serializable.SerializableFunction;

public class LambdaChoiceRenderer<T>
    extends ChoiceRenderer<T>
{
    private static final long serialVersionUID = -3003614158636642108L;
    
    private SerializableFunction<T, String> displayValueFunc;

    public LambdaChoiceRenderer(SerializableFunction<T, String> aDisplayValueFunc)
    {
        displayValueFunc = aDisplayValueFunc;
    }

    @Override
    public Object getDisplayValue(T object)
    {
        Object returnValue = object;

        if ((displayValueFunc != null)) {
            returnValue = displayValueFunc.apply(object);
        }

        if (returnValue == null) {
            return "";
        }

        return returnValue;
    }
}
