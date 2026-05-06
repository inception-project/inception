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
package de.tudarmstadt.ukp.inception.support.json;

import java.lang.reflect.Field;
import java.util.Collection;

import org.springframework.util.ReflectionUtils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.BeanProperty;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.jsontype.TypeSerializer;
import tools.jackson.databind.ser.std.AsArraySerializerBase;
import tools.jackson.databind.ser.std.StdContainerSerializer;

/**
 * Fallback serializer for cases where Collection is not known to be of type for which more
 * specialized serializer exists (such as index-accessible List). If so, we will just construct an
 * {@link java.util.Iterator} to iterate over elements.
 */
public class BeanAsArraySerializer
    extends AsArraySerializerBase<Object>
{
    public BeanAsArraySerializer()
    {
        this(null, false, null, null, null);
    }

    public BeanAsArraySerializer(JavaType elemType, boolean staticTyping, TypeSerializer vts,
            ValueSerializer<Object> valueSerializer)
    {
        super(Collection.class, elemType, staticTyping, vts, valueSerializer);
    }

    public BeanAsArraySerializer(JavaType elemType, boolean staticTyping, TypeSerializer vts,
            BeanProperty property, ValueSerializer<Object> valueSerializer)
    {
        super(Collection.class, elemType, staticTyping, vts, valueSerializer);
    }

    public BeanAsArraySerializer(BeanAsArraySerializer src, BeanProperty property,
            TypeSerializer vts, ValueSerializer<?> valueSerializer, Boolean unwrapSingle)
    {
        super(src, vts, valueSerializer, unwrapSingle, property);
    }

    @Override
    public StdContainerSerializer<?> _withValueTypeSerializer(TypeSerializer vts)
    {
        return new BeanAsArraySerializer(_elementType, _staticTyping, vts, _property,
                _elementSerializer);
    }

    @Override
    public BeanAsArraySerializer withResolved(BeanProperty aProperty, TypeSerializer aVts,
            ValueSerializer<?> aElementSerializer, Boolean aUnwrapSingle)
    {
        return new BeanAsArraySerializer(this, aProperty, aVts, aElementSerializer, aUnwrapSingle);
    }

    @Override
    public boolean hasSingleElement(Object aValue)
    {
        if (aValue != null) {
            if (aValue.getClass().isArray()) {
                return ((Object[]) aValue).length == 1;
            }
        }

        return false;
    }

    @Override
    public boolean isEmpty(SerializationContext provider, Object value)
    {
        if (value == null) {
            return true;
        }
        if (value.getClass().isArray()) {
            return ((Object[]) value).length == 0;
        }
        return false;
    }

    @Override
    public void serialize(Object value, JsonGenerator gen, SerializationContext provider)
    {
        gen.writeStartArray();
        serializeContents(value, gen, provider);
        gen.writeEndArray();
    }

    @Override
    public void serializeContents(Object value, JsonGenerator jgen, SerializationContext provider)
    {
        JsonPropertyOrder order = value.getClass().getAnnotation(JsonPropertyOrder.class);
        String[] propOrder = (order == null) ? null : order.value();

        if (propOrder == null) {
            throw new IllegalStateException("Bean must declare JsonPropertyOrder!");
        }

        if (propOrder.length == 0) {
            return;
        }

        int i = 0;
        try {
            do {
                Field field = value.getClass().getDeclaredField(propOrder[i]);
                ReflectionUtils.makeAccessible(field);
                Object elem = field.get(value);
                if (elem == null) {
                    JsonInclude include = field.getAnnotation(JsonInclude.class);
                    boolean renderNull = (include == null) ? false
                            : include.value() == Include.ALWAYS;
                    if (renderNull) {
                        jgen.writeNull();
                    }
                }
                else {
                    Class<?> cc = elem.getClass();
                    ValueSerializer<Object> serializer = provider.findValueSerializer(cc);
                    serializer.serialize(elem, jgen, provider);
                }
                ++i;
            }
            while (i < propOrder.length);
        }
        catch (Exception e) {
            // [JACKSON-55] Need to add reference information
            wrapAndThrow(provider, e, value, i);
        }
    }
}
