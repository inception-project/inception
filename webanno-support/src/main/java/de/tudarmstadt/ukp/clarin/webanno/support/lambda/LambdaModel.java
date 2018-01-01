/*
 * Copyright 2017
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

import org.apache.wicket.model.LoadableDetachableModel;

public abstract class LambdaModel<T>
    extends LoadableDetachableModel<T>
{
    private static final long serialVersionUID = -1455152622735082623L;

//    private final SerializableSupplier<T> supplier;
//
//    public LambdaModel(SerializableSupplier<T> aSupplier)
//    {
//        supplier = aSupplier;
//    }
//
//    @SuppressWarnings("unchecked")
//    @Override
//    protected T load()
//    {
//        Object value = supplier.get();
//        if (value instanceof IModel) {
//            return ((IModel<T>) value).getObject();
//        }
//        else {
//            return (T) value;
//        }
//    }
//
//    public static <T> LambdaModel<T> of(SerializableSupplier<T> aSupplier)
//    {
//        return new LambdaModel<T>(aSupplier);
//    }
}
