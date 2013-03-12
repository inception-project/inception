/*******************************************************************************
 * Copyright 2012
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
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
import org.apache.uima.jcas.cas.AnnotationBase_Type;

/**
 * Updated by JCasGen Thu Nov 08 15:55:25 CET 2012
 * 
 * @generated
 */
public class CoreferenceChain_Type
    extends AnnotationBase_Type
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
            if (CoreferenceChain_Type.this.useExistingInstance) {
                // Return eq fs instance if already created
                FeatureStructure fs = CoreferenceChain_Type.this.jcas.getJfsFromCaddr(addr);
                if (null == fs) {
                    fs = new CoreferenceChain(addr, CoreferenceChain_Type.this);
                    CoreferenceChain_Type.this.jcas.putJfsFromCaddr(addr, fs);
                    return fs;
                }
                return fs;
            }
            else
                return new CoreferenceChain(addr, CoreferenceChain_Type.this);
        }
    };
    /** @generated */
    @SuppressWarnings("hiding")
    public final static int typeIndexID = CoreferenceChain.typeIndexID;
    /**
     * @generated
     * @modifiable
     */
    @SuppressWarnings("hiding")
    public final static boolean featOkTst = JCasRegistry
            .getFeatOkTst("de.tudarmstadt.ukp.dkpro.core.api.coref.type.CoreferenceChain");

    /** @generated */
    final Feature casFeat_first;
    /** @generated */
    final int casFeatCode_first;

    /** @generated */
    public int getFirst(int addr)
    {
        if (featOkTst && casFeat_first == null)
            jcas.throwFeatMissing("first",
                    "de.tudarmstadt.ukp.dkpro.core.api.coref.type.CoreferenceChain");
        return ll_cas.ll_getRefValue(addr, casFeatCode_first);
    }

    /** @generated */
    public void setFirst(int addr, int v)
    {
        if (featOkTst && casFeat_first == null)
            jcas.throwFeatMissing("first",
                    "de.tudarmstadt.ukp.dkpro.core.api.coref.type.CoreferenceChain");
        ll_cas.ll_setRefValue(addr, casFeatCode_first, v);
    }

    /**
     * initialize variables to correspond with Cas Type and Features
     * 
     * @generated
     */
    public CoreferenceChain_Type(JCas jcas, Type casType)
    {
        super(jcas, casType);
        casImpl.getFSClassRegistry().addGeneratorForType((TypeImpl) this.casType, getFSGenerator());

        casFeat_first = jcas.getRequiredFeatureDE(casType, "first",
                "de.tudarmstadt.ukp.dkpro.core.api.coref.type.CoreferenceLink", featOkTst);
        casFeatCode_first = (null == casFeat_first) ? JCas.INVALID_FEATURE_CODE
                : ((FeatureImpl) casFeat_first).getCode();

    }
}
