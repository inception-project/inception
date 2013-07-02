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
/* First created by JCasGen Thu Nov 08 15:55:25 CET 2012 */
package de.tudarmstadt.ukp.dkpro.core.api.coref.type;

import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.impl.FSGenerator;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.impl.TypeImpl;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.impl.FeatureImpl;
import org.apache.uima.cas.Feature;
import org.apache.uima.jcas.tcas.Annotation_Type;

/**
 * Updated by JCasGen Thu Nov 08 15:55:25 CET 2012
 * 
 * @generated
 */
public class CoreferenceLink_Type
    extends Annotation_Type
{
    /** @generated */
    @Override
    protected FSGenerator getFSGenerator()
    {
        return fsGenerator;
    }

    /** @generated */
    private final FSGenerator fsGenerator = new FSGenerator()
    {
        public FeatureStructure createFS(int addr, CASImpl cas)
        {
            if (CoreferenceLink_Type.this.useExistingInstance) {
                // Return eq fs instance if already created
                FeatureStructure fs = CoreferenceLink_Type.this.jcas.getJfsFromCaddr(addr);
                if (null == fs) {
                    fs = new CoreferenceLink(addr, CoreferenceLink_Type.this);
                    CoreferenceLink_Type.this.jcas.putJfsFromCaddr(addr, fs);
                    return fs;
                }
                return fs;
            }
            else
                return new CoreferenceLink(addr, CoreferenceLink_Type.this);
        }
    };
    /** @generated */
    @SuppressWarnings("hiding")
    public final static int typeIndexID = CoreferenceLink.typeIndexID;
    /**
     * @generated
     * @modifiable
     */
    @SuppressWarnings("hiding")
    public final static boolean featOkTst = JCasRegistry
            .getFeatOkTst("de.tudarmstadt.ukp.dkpro.core.api.coref.type.CoreferenceLink");

    /** @generated */
    final Feature casFeat_next;
    /** @generated */
    final int casFeatCode_next;

    /** @generated */
    public int getNext(int addr)
    {
        if (featOkTst && casFeat_next == null)
            jcas.throwFeatMissing("next",
                    "de.tudarmstadt.ukp.dkpro.core.api.coref.type.CoreferenceLink");
        return ll_cas.ll_getRefValue(addr, casFeatCode_next);
    }

    /** @generated */
    public void setNext(int addr, int v)
    {
        if (featOkTst && casFeat_next == null)
            jcas.throwFeatMissing("next",
                    "de.tudarmstadt.ukp.dkpro.core.api.coref.type.CoreferenceLink");
        ll_cas.ll_setRefValue(addr, casFeatCode_next, v);
    }

    /** @generated */
    final Feature casFeat_referenceType;
    /** @generated */
    final int casFeatCode_referenceType;

    /** @generated */
    public String getReferenceType(int addr)
    {
        if (featOkTst && casFeat_referenceType == null)
            jcas.throwFeatMissing("referenceType",
                    "de.tudarmstadt.ukp.dkpro.core.api.coref.type.CoreferenceLink");
        return ll_cas.ll_getStringValue(addr, casFeatCode_referenceType);
    }

    /** @generated */
    public void setReferenceType(int addr, String v)
    {
        if (featOkTst && casFeat_referenceType == null)
            jcas.throwFeatMissing("referenceType",
                    "de.tudarmstadt.ukp.dkpro.core.api.coref.type.CoreferenceLink");
        ll_cas.ll_setStringValue(addr, casFeatCode_referenceType, v);
    }

    /** @generated */
    final Feature casFeat_referenceRelation;
    /** @generated */
    final int casFeatCode_referenceRelation;

    /** @generated */
    public String getReferenceRelation(int addr)
    {
        if (featOkTst && casFeat_referenceRelation == null)
            jcas.throwFeatMissing("referenceRelation",
                    "de.tudarmstadt.ukp.dkpro.core.api.coref.type.CoreferenceLink");
        return ll_cas.ll_getStringValue(addr, casFeatCode_referenceRelation);
    }

    /** @generated */
    public void setReferenceRelation(int addr, String v)
    {
        if (featOkTst && casFeat_referenceRelation == null)
            jcas.throwFeatMissing("referenceRelation",
                    "de.tudarmstadt.ukp.dkpro.core.api.coref.type.CoreferenceLink");
        ll_cas.ll_setStringValue(addr, casFeatCode_referenceRelation, v);
    }

    /**
     * initialize variables to correspond with Cas Type and Features
     * 
     * @generated
     */
    public CoreferenceLink_Type(JCas jcas, Type casType)
    {
        super(jcas, casType);
        casImpl.getFSClassRegistry().addGeneratorForType((TypeImpl) this.casType, getFSGenerator());

        casFeat_next = jcas.getRequiredFeatureDE(casType, "next",
                "de.tudarmstadt.ukp.dkpro.core.api.coref.type.CoreferenceLink", featOkTst);
        casFeatCode_next = (null == casFeat_next) ? JCas.INVALID_FEATURE_CODE
                : ((FeatureImpl) casFeat_next).getCode();

        casFeat_referenceType = jcas.getRequiredFeatureDE(casType, "referenceType",
                "uima.cas.String", featOkTst);
        casFeatCode_referenceType = (null == casFeat_referenceType) ? JCas.INVALID_FEATURE_CODE
                : ((FeatureImpl) casFeat_referenceType).getCode();

        casFeat_referenceRelation = jcas.getRequiredFeatureDE(casType, "referenceRelation",
                "uima.cas.String", featOkTst);
        casFeatCode_referenceRelation = (null == casFeat_referenceRelation) ? JCas.INVALID_FEATURE_CODE
                : ((FeatureImpl) casFeat_referenceRelation).getCode();

    }
}
