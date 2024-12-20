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

import static java.util.Collections.emptySet;
import static org.apache.uima.fit.util.FSCollectionFactory.createStringArrayFS;
import static org.apache.uima.fit.util.FSCollectionFactory.createStringList;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Method;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.util.CasUtil;
import org.apache.uima.fit.util.FSUtil;
import org.apache.uima.jcas.cas.Sofa;
import org.apache.uima.jcas.cas.TOP;
import org.apache.uima.jcas.tcas.Annotation;

public class ICasUtil
{
    public static final String UIMA_BUILTIN_JCAS_PREFIX = "org.apache.uima.jcas.";

    public static boolean isPrimitive(String aTypeName)
    {
        if (aTypeName == null) {
            return false;
        }

        switch (aTypeName) {
        case CAS.TYPE_NAME_BOOLEAN, //
                CAS.TYPE_NAME_BYTE, //
                CAS.TYPE_NAME_DOUBLE, //
                CAS.TYPE_NAME_FLOAT, //
                CAS.TYPE_NAME_INTEGER, //
                CAS.TYPE_NAME_LONG, //
                CAS.TYPE_NAME_SHORT, //
                CAS.TYPE_NAME_STRING:
            return true;
        default:
            return false;
        }
    }

    /**
     * Copied from {@link FSUtil#setFeature(FeatureStructure, Feature, String...)} but changed it to
     * allow setting any primitive feature value from a string value.
     */
    public static void setFeature(FeatureStructure aFS, Feature feat, String... aValue)
    {
        if (feat.getRange().isPrimitive()) {
            // requireSingleValue(feat, aValue);
            aFS.setFeatureValueFromString(feat, aValue[0]);
            // aFS.setStringValue(feat, aValue[0]);
        }
        else if (aValue == null) {
            aFS.setFeatureValue(feat, null);
        }
        else if (feat.getRange().isArray()) {
            aFS.setFeatureValue(feat, createStringArrayFS(aFS.getCAS(), aValue));
        }
        else {
            aFS.setFeatureValue(feat, createStringList(aFS.getCAS(), aValue));
        }
    }

    /**
     * @param aFS
     *            the feature structure to retrieve the value from
     * @param aFeature
     *            the feature to retrieve the value from
     * @return the feature value of this {@code Feature} on this annotation
     */
    public static Object getFeatureValue(FeatureStructure aFS, Feature aFeature)
    {
        switch (aFeature.getRange().getName()) {
        case CAS.TYPE_NAME_STRING:
            return aFS.getFeatureValueAsString(aFeature);
        case CAS.TYPE_NAME_BOOLEAN:
            return aFS.getBooleanValue(aFeature);
        case CAS.TYPE_NAME_FLOAT:
            return aFS.getFloatValue(aFeature);
        case CAS.TYPE_NAME_INTEGER:
            return aFS.getIntValue(aFeature);
        case CAS.TYPE_NAME_BYTE:
            return aFS.getByteValue(aFeature);
        case CAS.TYPE_NAME_DOUBLE:
            return aFS.getDoubleValue(aFeature);
        case CAS.TYPE_NAME_LONG:
            return aFS.getLongValue(aFeature);
        case CAS.TYPE_NAME_SHORT:
            return aFS.getShortValue(aFeature);
        default:
            return null;
        // return aFS.getFeatureValue(aFeature);
        }
    }

    /**
     * @param aFeature
     *            a feature
     * @return the feature value of this {@code Feature} on this annotation
     */
    public static Object getDefaultFeatureValue(Feature aFeature)
    {
        switch (aFeature.getRange().getName()) {
        case CAS.TYPE_NAME_STRING_ARRAY:
            return emptySet();
        case CAS.TYPE_NAME_STRING:
            return null;
        case CAS.TYPE_NAME_BOOLEAN:
            return false;
        case CAS.TYPE_NAME_FLOAT:
            return 0.0f;
        case CAS.TYPE_NAME_INTEGER:
            return 0;
        case CAS.TYPE_NAME_BYTE:
            return (byte) 0;
        case CAS.TYPE_NAME_DOUBLE:
            return 0.0d;
        case CAS.TYPE_NAME_LONG:
            return 0l;
        case CAS.TYPE_NAME_SHORT:
            return (short) 0;
        default:
            return null;
        }
    }

    public static int getAddr(FeatureStructure aFS)
    {
        return ((CASImpl) aFS.getCAS()).ll_getFSRef(aFS);
    }

    public static Annotation selectAnnotationByAddr(CAS aCas, int aAddress)
    {
        return selectByAddr(aCas, Annotation.class, aAddress);
    }

    public static <T extends AnnotationFS> Annotation selectByAddr(CAS aCas, Class<T> aType,
            int aAddress)
    {
        // Check that the type passed is actually an annotation type
        CasUtil.getAnnotationType(aCas, aType);

        return aCas.getLowLevelCAS().ll_getFSForRef(aAddress);
    }

    public static FeatureStructure selectFsByAddr(CAS aCas, int aAddress)
    {
        return aCas.getLowLevelCAS().ll_getFSForRef(aAddress);
    }

    /**
     * Get a feature value.
     *
     * @param aFS
     *            the feature structure.
     * @param aFeatureName
     *            the feature within the annotation whose value to set.
     * @return the feature value.
     */
    public static FeatureStructure getFeatureFS(FeatureStructure aFS, String aFeatureName)
    {
        return aFS.getFeatureValue(aFS.getType().getFeatureByBaseName(aFeatureName));
    }

    public static String getUimaTypeName(Class<? extends TOP> aClazz)
    {
        String typeName = aClazz.getName();
        if (typeName.startsWith(UIMA_BUILTIN_JCAS_PREFIX)) {
            typeName = "uima." + typeName.substring(UIMA_BUILTIN_JCAS_PREFIX.length());
        }
        else if (FeatureStructure.class.getName().equals(typeName)) {
            typeName = CAS.TYPE_NAME_TOP;
        }
        else if (AnnotationFS.class.getName().equals(typeName)) {
            typeName = CAS.TYPE_NAME_ANNOTATION;
        }
        return typeName;
    }

    /**
     * NOT AT HOME THIS YOU SHOULD TRY<br>
     * SETTING THE SOFA STRING FORCEFULLY FOLLOWING THE DARK SIDE IS!
     * 
     * @param aCas
     *            the CAS to change
     * @param aValue
     *            the new sofa string
     */
    public static void forceOverwriteSofa(CAS aCas, String aValue)
    {
        try {
            Sofa sofa = (Sofa) aCas.getSofa();
            MethodHandle _FH_sofaString = (MethodHandle) FieldUtils.readField(sofa,
                    "_FH_sofaString", true);
            Method method = MethodUtils.getMatchingMethod(Sofa.class, "wrapGetIntCatchException",
                    MethodHandle.class);
            int adjOffset;
            try {
                method.setAccessible(true);
                adjOffset = (int) method.invoke(null, _FH_sofaString);
            }
            finally {
                method.setAccessible(false);
            }
            sofa._setStringValueNcWj(adjOffset, aValue);
        }
        catch (Exception e) {
            throw new IllegalStateException("Cannot force-update SofA string", e);
        }
    }

    public static boolean hasSameType(AnnotationFS aFs1, AnnotationFS aFs2)
    {
        // Null check
        if (aFs1 == null || aFs2 == null) {
            return false;
        }

        // Trivial case: same instance
        if (aFs1 == aFs2) {
            return true;
        }

        // Trivial case: same address
        if (aFs1.getCAS() == aFs1.getCAS() && getAddr(aFs1) == getAddr(aFs1)) {
            return true;
        }

        var type1 = aFs1.getType();
        var type2 = aFs2.getType();

        // Types must be the same
        return type1.getName().equals(type2.getName());
    }
}
