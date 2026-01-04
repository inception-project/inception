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
package de.tudarmstadt.ukp.clarin.webanno.api.annotation.config;

import static java.util.Collections.singleton;

import java.lang.reflect.Array;
import java.util.Set;

import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.ConditionalGenericConverter;
import org.springframework.lang.Nullable;

/**
 * Generic Spring converter that handles comma-separated enum values and converts them to enum
 * arrays. This converter delegates the actual enum value conversion to Spring's built-in enum
 * support and only handles the array/comma-separation aspect.
 * <p>
 * For example, it converts:
 * <ul>
 * <li>"Ctrl,z" → KeyType[] { KeyType.Ctrl, KeyType.z }</li>
 * <li>"Page_down" → KeyType[] { KeyType.Page_down }</li>
 * </ul>
 */
public class StringToEnumArrayConverter
    implements ConditionalGenericConverter
{
    @Override
    public Set<ConvertiblePair> getConvertibleTypes()
    {
        return singleton(new ConvertiblePair(String.class, Object[].class));
    }

    @Override
    public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType)
    {
        // Only handle conversions to enum arrays
        return targetType.isArray() && targetType.getElementTypeDescriptor() != null
                && targetType.getElementTypeDescriptor().getType().isEnum();
    }

    @Override
    @Nullable
    public Object convert(@Nullable Object source, TypeDescriptor sourceType,
            TypeDescriptor targetType)
    {
        if (source == null) {
            return null;
        }

        var sourceString = (String) source;
        if (sourceString.trim().isEmpty()) {
            return Array.newInstance(targetType.getElementTypeDescriptor().getType(), 0);
        }

        // Split by comma and trim whitespace
        // Use -1 limit to preserve trailing empty strings
        var parts = sourceString.trim().split("\\s*,\\s*", -1);

        // Get the target enum type
        var enumType = targetType.getElementTypeDescriptor().getType();

        // Create the result array
        var resultArray = Array.newInstance(enumType, parts.length);

        // Convert each part using the enum's valueOf method
        for (var i = 0; i < parts.length; i++) {
            var part = parts[i].trim();
            if (part.isEmpty()) {
                throw new IllegalArgumentException(
                        "Empty value at position " + i + " in comma-separated enum list. "
                                + "All parts must be non-empty. Input was: '" + source + "'");
            }
            try {
                // Use Enum.valueOf to leverage Spring's enum conversion behavior
                @SuppressWarnings({ "unchecked", "rawtypes" })
                var enumValue = Enum.valueOf((Class<Enum>) enumType, part);
                Array.set(resultArray, i, enumValue);
            }
            catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Cannot convert value '" + part
                        + "' to enum type " + enumType.getSimpleName() + ". Valid values are: "
                        + String.join(", ", getEnumNames(enumType)), e);
            }
        }

        return resultArray;
    }

    private String[] getEnumNames(Class<?> enumType)
    {
        var enumConstants = enumType.getEnumConstants();
        var names = new String[enumConstants.length];
        for (var i = 0; i < enumConstants.length; i++) {
            names[i] = ((Enum<?>) enumConstants[i]).name();
        }
        return names;
    }
}
