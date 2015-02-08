/*******************************************************************************
 * Copyright 2012
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
 ******************************************************************************/
package de.tudarmstadt.ukp.clarin.webanno.brat.message;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Collection;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.annotate.JsonPropertyOrder;
import org.codehaus.jackson.map.BeanProperty;
import org.codehaus.jackson.map.JsonSerializer;
import org.codehaus.jackson.map.SerializerProvider;
import org.codehaus.jackson.map.TypeSerializer;
import org.codehaus.jackson.map.ser.std.AsArraySerializerBase;
import org.codehaus.jackson.map.ser.std.ContainerSerializerBase;
import org.codehaus.jackson.type.JavaType;
import org.springframework.util.ReflectionUtils;

/**
 * Fallback serializer for cases where Collection is not known to be of type for which more
 * specializer serializer exists (such as index-accessible List). If so, we will just construct an
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
            BeanProperty property, JsonSerializer<Object> valueSerializer)
    {
        super(Collection.class, elemType, staticTyping, vts, property, null);
    }

    @Override
    public ContainerSerializerBase<?> _withValueTypeSerializer(TypeSerializer vts)
    {
        return new BeanAsArraySerializer(_elementType, _staticTyping, vts, _property,
                _elementSerializer);
    }

    @Override
    public void serializeContents(Object value, JsonGenerator jgen, SerializerProvider provider)
        throws IOException, JsonGenerationException
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
                    provider.defaultSerializeNull(jgen);
                }
                else {
                    Class<?> cc = elem.getClass();
                    JsonSerializer<Object> serializer = provider.findValueSerializer(cc, null);
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
