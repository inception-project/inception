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

import java.io.Serializable;

import org.apache.wicket.model.IModel;
import org.danekja.java.util.function.serializable.SerializableConsumer;
import org.danekja.java.util.function.serializable.SerializableSupplier;

public class LambdaModelAdapter<T>
    implements IModel<T>
{
    private static final long serialVersionUID = -1455152622735082623L;

    private final SerializableSupplier<T> supplier;
    private final SerializableConsumer<T> consumer;
    
    public LambdaModelAdapter(SerializableSupplier<T> aSupplier, SerializableConsumer<T> aConsumer)
    {
        supplier = aSupplier;
        consumer = aConsumer;
    }
    
    @Override
    public T getObject()
    {
        if (supplier != null) {
            return supplier.get();
        }
        else {
            return null;
        }
    }

    @Override
    public void setObject(T aObject)
    {
        if (consumer != null) {
            consumer.accept(aObject);
        }
    }

    public static <T extends Serializable> LambdaModelAdapter<T> of(
            SerializableSupplier<T> aSupplier, SerializableConsumer<T> aConsumer)
    {
        return new LambdaModelAdapter<T>(aSupplier, aConsumer);
    }

    @Override
    public void detach()
    {
        // Nothing to do
    }
    
    public static class Builder<T> {
        private SerializableSupplier<T> supplier;
        private SerializableConsumer<T> consumer;
        
        public Builder<T> getting(SerializableSupplier<T> aSupplier)
        {
            supplier = aSupplier;
            return this;
        }
        
        public Builder<T> setting(SerializableConsumer<T> aConsumer)
        {
            consumer = aConsumer;
            return this;
        }
        
        public LambdaModelAdapter<T> build()
        {
            return new LambdaModelAdapter<T>(supplier, consumer);
        }
    }
}
