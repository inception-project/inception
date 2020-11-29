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

package de.tudarmstadt.ukp.clarin.webanno.support;

import java.io.Serializable;

import org.apache.wicket.markup.repeater.util.ModelIteratorAdapter;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

public class DefaultModelIteratorAdapter<T extends Serializable>
    extends ModelIteratorAdapter<T>
{
    public DefaultModelIteratorAdapter(Iterable<T> aIterable)
    {
        super(aIterable);
    }

    @Override
    protected IModel<T> model(T aObject)
    {
        return Model.of(aObject);
    }

    public static <T extends Serializable> DefaultModelIteratorAdapter<T> of(Iterable<T> aIterable)
    {
        return new DefaultModelIteratorAdapter<>(aIterable);
    }
}
