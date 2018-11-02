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

/* First created by JCasGen Fri Nov 02 09:57:46 CET 2018 */
package de.tudarmstadt.ukp.clarin.webanno.api.type;

import org.apache.uima.cas.Feature;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.impl.FeatureImpl;
import org.apache.uima.cas.impl.TypeImpl;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.jcas.tcas.Annotation_Type;

/**
 * Updated by JCasGen Fri Nov 02 09:57:46 CET 2018
 * 
 * @generated
 */
public class CASMetadata_Type
    extends Annotation_Type
{
    /** @generated */
    @SuppressWarnings("hiding")
    public final static int typeIndexID = CASMetadata.typeIndexID;
    /**
     * @generated
     * @modifiable
     */
    @SuppressWarnings("hiding")
    public final static boolean featOkTst = JCasRegistry
            .getFeatOkTst("de.tudarmstadt.ukp.clarin.webanno.api.type.CASMetadata");

    /** @generated */
    final Feature casFeat_projectId;
    /** @generated */
    final int casFeatCode_projectId;

    /**
     * @generated
     * @param addr
     *            low level Feature Structure reference
     * @return the feature value
     */
    public long getProjectId(int addr)
    {
        if (featOkTst && casFeat_projectId == null) {
            jcas.throwFeatMissing("projectId",
                    "de.tudarmstadt.ukp.clarin.webanno.api.type.CASMetadata");
        }
        return ll_cas.ll_getLongValue(addr, casFeatCode_projectId);
    }

    /**
     * @generated
     * @param addr
     *            low level Feature Structure reference
     * @param v
     *            value to set
     */
    public void setProjectId(int addr, long v)
    {
        if (featOkTst && casFeat_projectId == null) {
            jcas.throwFeatMissing("projectId",
                    "de.tudarmstadt.ukp.clarin.webanno.api.type.CASMetadata");
        }
        ll_cas.ll_setLongValue(addr, casFeatCode_projectId, v);
    }

    /** @generated */
    final Feature casFeat_sourceDocumentId;
    /** @generated */
    final int casFeatCode_sourceDocumentId;

    /**
     * @generated
     * @param addr
     *            low level Feature Structure reference
     * @return the feature value
     */
    public long getSourceDocumentId(int addr)
    {
        if (featOkTst && casFeat_sourceDocumentId == null) {
            jcas.throwFeatMissing("sourceDocumentId",
                    "de.tudarmstadt.ukp.clarin.webanno.api.type.CASMetadata");
        }
        return ll_cas.ll_getLongValue(addr, casFeatCode_sourceDocumentId);
    }

    /**
     * @generated
     * @param addr
     *            low level Feature Structure reference
     * @param v
     *            value to set
     */
    public void setSourceDocumentId(int addr, long v)
    {
        if (featOkTst && casFeat_sourceDocumentId == null) {
            jcas.throwFeatMissing("sourceDocumentId",
                    "de.tudarmstadt.ukp.clarin.webanno.api.type.CASMetadata");
        }
        ll_cas.ll_setLongValue(addr, casFeatCode_sourceDocumentId, v);
    }

    /** @generated */
    final Feature casFeat_username;
    /** @generated */
    final int casFeatCode_username;

    /**
     * @generated
     * @param addr
     *            low level Feature Structure reference
     * @return the feature value
     */
    public String getUsername(int addr)
    {
        if (featOkTst && casFeat_username == null) {
            jcas.throwFeatMissing("username",
                    "de.tudarmstadt.ukp.clarin.webanno.api.type.CASMetadata");
        }
        return ll_cas.ll_getStringValue(addr, casFeatCode_username);
    }

    /**
     * @generated
     * @param addr
     *            low level Feature Structure reference
     * @param v
     *            value to set
     */
    public void setUsername(int addr, String v)
    {
        if (featOkTst && casFeat_username == null) {
            jcas.throwFeatMissing("username",
                    "de.tudarmstadt.ukp.clarin.webanno.api.type.CASMetadata");
        }
        ll_cas.ll_setStringValue(addr, casFeatCode_username, v);
    }

    /** @generated */
    final Feature casFeat_lastChangedOnDisk;
    /** @generated */
    final int casFeatCode_lastChangedOnDisk;

    /**
     * @generated
     * @param addr
     *            low level Feature Structure reference
     * @return the feature value
     */
    public long getLastChangedOnDisk(int addr)
    {
        if (featOkTst && casFeat_lastChangedOnDisk == null) {
            jcas.throwFeatMissing("lastChangedOnDisk",
                    "de.tudarmstadt.ukp.clarin.webanno.api.type.CASMetadata");
        }
        return ll_cas.ll_getLongValue(addr, casFeatCode_lastChangedOnDisk);
    }

    /**
     * @generated
     * @param addr
     *            low level Feature Structure reference
     * @param v
     *            value to set
     */
    public void setLastChangedOnDisk(int addr, long v)
    {
        if (featOkTst && casFeat_lastChangedOnDisk == null) {
            jcas.throwFeatMissing("lastChangedOnDisk",
                    "de.tudarmstadt.ukp.clarin.webanno.api.type.CASMetadata");
        }
        ll_cas.ll_setLongValue(addr, casFeatCode_lastChangedOnDisk, v);
    }

    /**
     * initialize variables to correspond with Cas Type and Features
     * 
     * @generated
     * @param jcas
     *            JCas
     * @param casType
     *            Type
     */
    public CASMetadata_Type(JCas jcas, Type casType)
    {
        super(jcas, casType);
        casImpl.getFSClassRegistry().addGeneratorForType((TypeImpl) this.casType, getFSGenerator());

        casFeat_projectId = jcas.getRequiredFeatureDE(casType, "projectId", "uima.cas.Long",
                featOkTst);
        casFeatCode_projectId = (null == casFeat_projectId) ? JCas.INVALID_FEATURE_CODE
                : ((FeatureImpl) casFeat_projectId).getCode();

        casFeat_sourceDocumentId = jcas.getRequiredFeatureDE(casType, "sourceDocumentId",
                "uima.cas.Long", featOkTst);
        casFeatCode_sourceDocumentId = (null == casFeat_sourceDocumentId)
                ? JCas.INVALID_FEATURE_CODE
                : ((FeatureImpl) casFeat_sourceDocumentId).getCode();

        casFeat_username = jcas.getRequiredFeatureDE(casType, "username", "uima.cas.String",
                featOkTst);
        casFeatCode_username = (null == casFeat_username) ? JCas.INVALID_FEATURE_CODE
                : ((FeatureImpl) casFeat_username).getCode();

        casFeat_lastChangedOnDisk = jcas.getRequiredFeatureDE(casType, "lastChangedOnDisk",
                "uima.cas.Long", featOkTst);
        casFeatCode_lastChangedOnDisk = (null == casFeat_lastChangedOnDisk)
                ? JCas.INVALID_FEATURE_CODE
                : ((FeatureImpl) casFeat_lastChangedOnDisk).getCode();

    }
}
