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
package de.tudarmstadt.ukp.inception.support.uima;

import static java.util.Arrays.asList;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.fit.util.CasUtil;
import org.apache.uima.fit.util.FSUtil;

public class FeatureStructureBuilder<T extends FeatureStructure>
{
    private final CAS cas;
    private final Type type;
    private final Map<String, Object> features;

    public FeatureStructureBuilder(CAS aCas, Type aType)
    {
        cas = aCas;
        type = aType;
        features = new HashMap<>();
    }

    public FeatureStructureBuilder<T> withFeature(String aName, Object aValue)
    {
        features.put(aName, aValue);
        return this;
    }

    public FeatureStructureBuilder<T> withFeature(String aName, Object aValue,
            Object... aAdditionalValues)
    {
        var values = new ArrayList<Object>();
        values.add(aValue);
        values.addAll(asList(aAdditionalValues));
        features.put(aName, values);
        return this;
    }

    public FeatureStructureBuilder<T> withFeature(Feature aFeature, Object aValue)
    {
        features.put(aFeature.getShortName(), aValue);
        return this;
    }

    public T buildWithoutAddingToIndexes()
    {
        T subject = cas.createFS(type);

        for (Entry<String, Object> fv : features.entrySet()) {
            setFeature(subject, fv.getKey(), fv.getValue());
        }

        return subject;
    }

    public T buildAndAddToIndexes()
    {
        T subject = buildWithoutAddingToIndexes();
        cas.addFsToIndexes(subject);
        return subject;
    }

    private void setFeature(FeatureStructure aFS, String aName, Object aValue)
    {
        if (aValue == null) {
            FSUtil.setFeature(aFS, aName, (FeatureStructure) null);
            return;
        }

        try {
            boolean isFeatureStructureValue = FeatureStructure.class
                    .isAssignableFrom(aValue.getClass());
            boolean isFeatureStructureVarArgValue = aValue.getClass().isArray()
                    && FeatureStructure.class
                            .isAssignableFrom(aValue.getClass().getComponentType());

            // Workaround for https://issues.apache.org/jira/browse/LANG-1597
            if (isFeatureStructureValue || isFeatureStructureVarArgValue) {
                // MethodUtils.invokeStaticMethod has a "bug" that vararg methods are only
                // properly found/matched if the parameter value type or the supertype of the
                // parameter value matches the varargs type. So here we have FeatureStructure...
                // as the varargs definition, but we may want to set a value of type Annotation
                // and from Annotation to FeatureStructure is a long way through the inheritance
                // hierarchy.
                // Note: this bug exists at least up to commons-lang3 3.11 - it might be fixed
                // in later versions.
                MethodUtils.invokeStaticMethod(FSUtil.class, "setFeature",
                        new Object[] { aFS, aName, aValue }, new Class[] { FeatureStructure.class,
                                String.class, FeatureStructure[].class });
                return;
            }

            // Workaround for https://issues.apache.org/jira/browse/LANG-1596
            // Note: we probable need to add this workaround for more types...
            // Note: this bug exists at least up to commons-lang3 3.11 - it might be fixed
            // in later versions.
            if (aValue instanceof Boolean) {
                FSUtil.setFeature(aFS, aName, (boolean) aValue);
                return;
            }
            if (aValue instanceof Boolean[]) {
                FSUtil.setFeature(aFS, aName, ArrayUtils.toPrimitive((Boolean[]) aValue));
                return;
            }
            if (aValue instanceof boolean[]) {
                FSUtil.setFeature(aFS, aName, (boolean[]) aValue);
                return;
            }

            MethodUtils.invokeStaticMethod(FSUtil.class, "setFeature", aFS, aName, aValue);
        }
        catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new IllegalArgumentException("Cannot set feature [" + aName + "]", e);
        }
    }

    public static FeatureStructureBuilder<FeatureStructure> buildFS(CAS aCas, String aType)
    {
        return new FeatureStructureBuilder<FeatureStructure>(aCas, CasUtil.getType(aCas, aType));
    }

    public CAS getCas()
    {
        return cas;
    }
}
