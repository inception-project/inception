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

import java.util.ArrayList;
import java.util.List;

import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.jcas.cas.TOP_Type;

import org.apache.uima.jcas.cas.AnnotationBase;

/**
 * Updated by JCasGen Thu Nov 08 15:55:25 CET 2012 XML source:
 * /Users/bluefire/UKP/Workspaces/dkpro-juno
 * /de.tudarmstadt.ukp.clarin.webanno/de.tudarmstadt.ukp.clarin
 * .webanno.uima/src/main/resources/desc/type/coref.xml
 * 
 * @generated
 */
public class CoreferenceChain
    extends AnnotationBase
{
    /**
     * @generated
     * @ordered
     */
    @SuppressWarnings("hiding")
    public final static int typeIndexID = JCasRegistry.register(CoreferenceChain.class);
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
    protected CoreferenceChain()
    {/* intentionally empty block */
    }

    /**
     * Internal - constructor used by generator
     * 
     * @generated
     */
    public CoreferenceChain(int addr, TOP_Type type)
    {
        super(addr, type);
        readObject();
    }

    /** @generated */
    public CoreferenceChain(JCas jcas)
    {
        super(jcas);
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
    // * Feature: first

    /**
     * getter for first - gets This is the first corefernce link in coreference chain
     * 
     * @generated
     */
    public CoreferenceLink getFirst()
    {
        if (CoreferenceChain_Type.featOkTst
                && ((CoreferenceChain_Type) jcasType).casFeat_first == null)
            jcasType.jcas.throwFeatMissing("first",
                    "de.tudarmstadt.ukp.dkpro.core.api.coref.type.CoreferenceChain");
        return (CoreferenceLink) (jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(
                addr, ((CoreferenceChain_Type) jcasType).casFeatCode_first)));
    }

    /**
     * setter for first - sets This is the first corefernce link in coreference chain
     * 
     * @generated
     */
    public void setFirst(CoreferenceLink v)
    {
        if (CoreferenceChain_Type.featOkTst
                && ((CoreferenceChain_Type) jcasType).casFeat_first == null)
            jcasType.jcas.throwFeatMissing("first",
                    "de.tudarmstadt.ukp.dkpro.core.api.coref.type.CoreferenceChain");
        jcasType.ll_cas.ll_setRefValue(addr, ((CoreferenceChain_Type) jcasType).casFeatCode_first,
                jcasType.ll_cas.ll_getFSRef(v));
    }

    /**
     * Get all links in the chain as a list.
     */
    public List<CoreferenceLink> links()
    {
        List<CoreferenceLink> links = new ArrayList<CoreferenceLink>();
        CoreferenceLink l = getFirst();
        while (l != null) {
            links.add(l);
            l = l.getNext();
        }
        return links;
    }
}
