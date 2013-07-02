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
import org.apache.uima.jcas.cas.TOP_Type;

import org.apache.uima.jcas.tcas.Annotation;

/**
 * Updated by JCasGen Thu Nov 08 15:55:25 CET 2012 XML source:
 * /Users/bluefire/UKP/Workspaces/dkpro-juno
 * /de.tudarmstadt.ukp.clarin.webanno/de.tudarmstadt.ukp.clarin
 * .webanno.uima/src/main/resources/desc/type/coref.xml
 * 
 * @generated
 */
public class CoreferenceLink
    extends Annotation
{
    /**
     * @generated
     * @ordered
     */
    @SuppressWarnings("hiding")
    public final static int typeIndexID = JCasRegistry.register(CoreferenceLink.class);
    /**
     * @generated
     * @ordered
     */
    @SuppressWarnings("hiding")
    public final static int type = typeIndexID;

    /** @generated */
    @Override
    public int getTypeIndexID()
    {
        return typeIndexID;
    }

    /**
     * Never called. Disable default constructor
     * 
     * @generated
     */
    protected CoreferenceLink()
    {/* intentionally empty block */
    }

    /**
     * Internal - constructor used by generator
     * 
     * @generated
     */
    public CoreferenceLink(int addr, TOP_Type type)
    {
        super(addr, type);
        readObject();
    }

    /** @generated */
    public CoreferenceLink(JCas jcas)
    {
        super(jcas);
        readObject();
    }

    /** @generated */
    public CoreferenceLink(JCas jcas, int begin, int end)
    {
        super(jcas);
        setBegin(begin);
        setEnd(end);
        readObject();
    }

    /**
     * <!-- begin-user-doc --> Write your own initialization here <!-- end-user-doc -->
     * 
     * @generated modifiable
     */
    private void readObject()
    {/* default - does nothing empty block */
    }

    // *--------------*
    // * Feature: next

    /**
     * getter for next - gets If there is one, it is the next coreference link to the current
     * coreference link
     * 
     * @generated
     */
    public CoreferenceLink getNext()
    {
        if (CoreferenceLink_Type.featOkTst
                && ((CoreferenceLink_Type) jcasType).casFeat_next == null)
            jcasType.jcas.throwFeatMissing("next",
                    "de.tudarmstadt.ukp.dkpro.core.api.coref.type.CoreferenceLink");
        return (CoreferenceLink) (jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(
                addr, ((CoreferenceLink_Type) jcasType).casFeatCode_next)));
    }

    /**
     * setter for next - sets If there is one, it is the next coreference link to the current
     * coreference link
     * 
     * @generated
     */
    public void setNext(CoreferenceLink v)
    {
        if (CoreferenceLink_Type.featOkTst
                && ((CoreferenceLink_Type) jcasType).casFeat_next == null)
            jcasType.jcas.throwFeatMissing("next",
                    "de.tudarmstadt.ukp.dkpro.core.api.coref.type.CoreferenceLink");
        jcasType.ll_cas.ll_setRefValue(addr, ((CoreferenceLink_Type) jcasType).casFeatCode_next,
                jcasType.ll_cas.ll_getFSRef(v));
    }

    // *--------------*
    // * Feature: referenceType

    /**
     * getter for referenceType - gets the type of the coreference link, which is a span annotation
     * 
     * @generated
     */
    public String getReferenceType()
    {
        if (CoreferenceLink_Type.featOkTst
                && ((CoreferenceLink_Type) jcasType).casFeat_referenceType == null)
            jcasType.jcas.throwFeatMissing("referenceType",
                    "de.tudarmstadt.ukp.dkpro.core.api.coref.type.CoreferenceLink");
        return jcasType.ll_cas.ll_getStringValue(addr,
                ((CoreferenceLink_Type) jcasType).casFeatCode_referenceType);
    }

    /**
     * setter for referenceType - sets the type of the coreference link, which is a span annotation
     * 
     * @generated
     */
    public void setReferenceType(String v)
    {
        if (CoreferenceLink_Type.featOkTst
                && ((CoreferenceLink_Type) jcasType).casFeat_referenceType == null)
            jcasType.jcas.throwFeatMissing("referenceType",
                    "de.tudarmstadt.ukp.dkpro.core.api.coref.type.CoreferenceLink");
        jcasType.ll_cas.ll_setStringValue(addr,
                ((CoreferenceLink_Type) jcasType).casFeatCode_referenceType, v);
    }

    // *--------------*
    // * Feature: referenceRelation

    /**
     * getter for referenceRelation - gets the relationship between two coreference links. The arc
     * label of coreference links
     * 
     * @generated
     */
    public String getReferenceRelation()
    {
        if (CoreferenceLink_Type.featOkTst
                && ((CoreferenceLink_Type) jcasType).casFeat_referenceRelation == null)
            jcasType.jcas.throwFeatMissing("referenceRelation",
                    "de.tudarmstadt.ukp.dkpro.core.api.coref.type.CoreferenceLink");
        return jcasType.ll_cas.ll_getStringValue(addr,
                ((CoreferenceLink_Type) jcasType).casFeatCode_referenceRelation);
    }

    /**
     * setter for referenceRelation - sets the relationship between two coreference links. The arc
     * label of coreference links
     * 
     * @generated
     */
    public void setReferenceRelation(String v)
    {
        if (CoreferenceLink_Type.featOkTst
                && ((CoreferenceLink_Type) jcasType).casFeat_referenceRelation == null)
            jcasType.jcas.throwFeatMissing("referenceRelation",
                    "de.tudarmstadt.ukp.dkpro.core.api.coref.type.CoreferenceLink");
        jcasType.ll_cas.ll_setStringValue(addr,
                ((CoreferenceLink_Type) jcasType).casFeatCode_referenceRelation, v);
    }
}
