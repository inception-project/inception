/*
 * Copyright 2018
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
package de.tudarmstadt.ukp.inception.ui.kb.stmt.editor;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.vocabulary.XMLSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ValueEditorFactory implements Serializable {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(ValueEditorFactory.class);

    private static final long serialVersionUID = -4863266680837254843L;
    
    private static final Map<IRI, Class<? extends ValueEditor<?>>> EDITOR_MAP = new HashMap<>();
    static {
        EDITOR_MAP.put(XMLSchema.STRING, StringValueEditor.class);
//        EDITOR_MAP.put(Integer.class, XMLSchema.INT);
//        EDITOR_MAP.put(Date.class, XMLSchema.DATETIME);
//        EDITOR_MAP.put(Boolean.class, XMLSchema.BOOLEAN);
//        EDITOR_MAP.put(Float.class, XMLSchema.FLOAT);
//        EDITOR_MAP.put(Double.class, XMLSchema.DOUBLE);
//        EDITOR_MAP.put(Byte.class, XMLSchema.BYTE);
//        EDITOR_MAP.put(Long.class, XMLSchema.LONG);
//        EDITOR_MAP.put(Short.class, XMLSchema.SHORT);
//        EDITOR_MAP.put(BigDecimal.class, XMLSchema.DECIMAL);
//        EDITOR_MAP.put(java.net.URI.class, XMLSchema.ANYURI);
    }

    public ValueEditorFactory() {
    }

    /**
     * Returns a {@link ValueEditor} instance given a datatype IRI (most likely the range of a
     * property or the datatype of a statement).<br/>
     * If {@code datatype} is {@code null} or not supported, a {@link StringValueEditor} instance is
     * returned.
     * 
     * @param datatype
     *            the IRI of the datatype ({@link XMLSchema})
     * @param id
     *            Wicket markup id received by the editor instances
     * @return a {@link ValueEditor} instance
     */
    public ValueEditor<?> getValueEditorClass(IRI datatype, String id) {
        if (datatype == null || !EDITOR_MAP.containsKey(datatype)) {
            return new StringValueEditor(id);
        } else {
            try {
                return EDITOR_MAP.get(datatype).getConstructor(String.class).newInstance(id);
            } catch (InstantiationException | IllegalAccessException | IllegalArgumentException
                    | InvocationTargetException | NoSuchMethodException | SecurityException e) {
                LOGGER.error("Value editor instantiation failed.", e);
                throw new IllegalStateException(e);
            }
        }

    }
}
