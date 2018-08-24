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
package de.tudarmstadt.ukp.inception.ui.kb.value;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.ClassUtils;
import org.cyberborean.rdfbeans.datatype.DefaultDatatypeMapper;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.vocabulary.XMLSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.inception.kb.graph.KBProperty;
import de.tudarmstadt.ukp.inception.kb.graph.KBStatement;

@Component
public class ValueTypeSupportRegistryImpl
    implements ValueTypeSupportRegistry
{
    private final Logger log = LoggerFactory.getLogger(getClass());

    private final List<ValueTypeSupport> valueSupportsProxy;
    
    private List<ValueTypeSupport> valueSupports;
    
    private final Map<String, ValueTypeSupport> supportCache = new HashMap<>();
    
    public ValueTypeSupportRegistryImpl(
            @Lazy @Autowired(required = false) List<ValueTypeSupport> aValueTypeSupports)
    {
        valueSupportsProxy = aValueTypeSupports;
    }
    
    @EventListener
    public void onContextRefreshedEvent(ContextRefreshedEvent aEvent)
    {
        init();
    }
    
    public void init()
    {
        List<ValueTypeSupport> fsp = new ArrayList<>();

        if (valueSupportsProxy != null) {
            fsp.addAll(valueSupportsProxy);
            AnnotationAwareOrderComparator.sort(fsp);
        
            for (ValueTypeSupport fs : fsp) {
                log.info("Found value type support: {}",
                        ClassUtils.getAbbreviatedName(fs.getClass(), 20));
            }
        }
        
        valueSupports = Collections.unmodifiableList(fsp);
    }
    
    @Override
    public ValueType getValueType(KBStatement aStatement, KBProperty aProperty)
    {
        String datatype = getDataType(aStatement, aProperty);
        try {
            return getValueSupport(aStatement, aProperty).getSupportedValueTypes().stream()
                .findFirst().orElse(null);
        }
        catch (IllegalArgumentException e) {
            return null;
        }
    }
    
    @Override
    public List<ValueTypeSupport> getValueSupports()
    {
        return valueSupports;
    }
    
    private String getDataType(KBStatement aStatement, KBProperty aProperty)
    {
        String datatype = null;
        
        if (aStatement.getValue() != null) {
            Class<?> clazz = aStatement.getValue().getClass();
            IRI type = DefaultDatatypeMapper.getDatatypeURI(clazz);

            // Mapping fails for NaiveIRI class, so check manually
            // if the value is an instance of IRI
            if (type == null && aStatement.getValue() instanceof IRI) {
                type = XMLSchema.ANYURI;
            }
            datatype = type != null ? type.stringValue() : null;
        }
        
        if (datatype == null && aProperty != null && aProperty.getRange() != null) {
            return aProperty.getRange();
        }
        
        if (datatype == null) {
            datatype = XMLSchema.STRING.stringValue();
        }
        
        return datatype;
    }
    
    @Override
    public ValueTypeSupport getValueSupport(KBStatement aStatement, KBProperty aProperty)
    {
        // Determine the data type
        String datatype = getDataType(aStatement, aProperty);
        String range = null;;
        if (aProperty != null) {
            range = aProperty.getRange();
        }
        ValueTypeSupport support = supportCache.get(datatype);
        for (ValueTypeSupport s : getValueSupports()) {
            if (s.accepts(aStatement, aProperty) || s.accepts(range, null)) {
                support = s;
                supportCache.put(datatype, s);
                break;
            }
        }
        
        if (support == null) {
            throw new IllegalArgumentException(
                    "Unsupported value type: [" + datatype + "]");
        }
        
        return support;
    }

    @Override
    public ValueTypeSupport getValueSupport(ValueType type)
    {
        ValueTypeSupport support = null;
                
        for (ValueTypeSupport s : getValueSupports()) {
            if (s.getSupportedValueTypes().contains(type)) {
                support = s;
                break;
            }
        }
        
        if (support == null) {
            throw new IllegalArgumentException(
                    "Unsupported value type: [" + type.getUiName() + "]");
        }
        
        return support;
    }

}
