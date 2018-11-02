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

import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.jcas.cas.TOP_Type;
import org.apache.uima.jcas.tcas.Annotation;

/**
 * Updated by JCasGen Fri Nov 02 09:57:46 CET 2018 XML source:
 * /Users/bluefire/git/webanno/webanno-api/src/main/resources/desc/type/webanno-internal.xml
 * 
 * @generated
 */
public class CASMetadata
    extends Annotation
{
    /**
     * @generated
     * @ordered
     */
    @SuppressWarnings("hiding")
    public final static int typeIndexID = JCasRegistry.register(CASMetadata.class);
    /**
     * @generated
     * @ordered
     */
    @SuppressWarnings("hiding")
    public final static int type = typeIndexID;

    /**
     * @generated
     * @return index of the type
     */
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
    protected CASMetadata()
    {
        /* intentionally empty block */}

    /**
     * Internal - constructor used by generator
     * 
     * @generated
     * @param addr
     *            low level Feature Structure reference
     * @param type
     *            the type of this Feature Structure
     */
    public CASMetadata(int addr, TOP_Type type)
    {
        super(addr, type);
        readObject();
    }

    /**
     * @generated
     * @param jcas
     *            JCas to which this Feature Structure belongs
     */
    public CASMetadata(JCas jcas)
    {
        super(jcas);
        readObject();
    }

    /**
     * @generated
     * @param jcas
     *            JCas to which this Feature Structure belongs
     * @param begin
     *            offset to the begin spot in the SofA
     * @param end
     *            offset to the end spot in the SofA
     */
    public CASMetadata(JCas jcas, int begin, int end)
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
    {
        /* default - does nothing empty block */}

    // *--------------*
    // * Feature: projectId

    /**
     * getter for projectId - gets The ID of the project to which these annotations belong.
     * 
     * @generated
     * @return value of the feature
     */
    public long getProjectId()
    {
        if (CASMetadata_Type.featOkTst && ((CASMetadata_Type) jcasType).casFeat_projectId == null) {
            jcasType.jcas.throwFeatMissing("projectId",
                    "de.tudarmstadt.ukp.clarin.webanno.api.type.CASMetadata");
        }
        return jcasType.ll_cas.ll_getLongValue(addr,
                ((CASMetadata_Type) jcasType).casFeatCode_projectId);
    }

    /**
     * setter for projectId - sets The ID of the project to which these annotations belong.
     * 
     * @generated
     * @param v
     *            value to set into the feature
     */
    public void setProjectId(long v)
    {
        if (CASMetadata_Type.featOkTst && ((CASMetadata_Type) jcasType).casFeat_projectId == null) {
            jcasType.jcas.throwFeatMissing("projectId",
                    "de.tudarmstadt.ukp.clarin.webanno.api.type.CASMetadata");
        }
        jcasType.ll_cas.ll_setLongValue(addr, ((CASMetadata_Type) jcasType).casFeatCode_projectId,
                v);
    }

    // *--------------*
    // * Feature: sourceDocumentId

    /**
     * getter for sourceDocumentId - gets The ID of the source documents to which these annotations
     * belong.
     * 
     * @generated
     * @return value of the feature
     */
    public long getSourceDocumentId()
    {
        if (CASMetadata_Type.featOkTst
                && ((CASMetadata_Type) jcasType).casFeat_sourceDocumentId == null) {
            jcasType.jcas.throwFeatMissing("sourceDocumentId",
                    "de.tudarmstadt.ukp.clarin.webanno.api.type.CASMetadata");
        }
        return jcasType.ll_cas.ll_getLongValue(addr,
                ((CASMetadata_Type) jcasType).casFeatCode_sourceDocumentId);
    }

    /**
     * setter for sourceDocumentId - sets The ID of the source documents to which these annotations
     * belong.
     * 
     * @generated
     * @param v
     *            value to set into the feature
     */
    public void setSourceDocumentId(long v)
    {
        if (CASMetadata_Type.featOkTst
                && ((CASMetadata_Type) jcasType).casFeat_sourceDocumentId == null) {
            jcasType.jcas.throwFeatMissing("sourceDocumentId",
                    "de.tudarmstadt.ukp.clarin.webanno.api.type.CASMetadata");
        }
        jcasType.ll_cas.ll_setLongValue(addr,
                ((CASMetadata_Type) jcasType).casFeatCode_sourceDocumentId, v);
    }

    // *--------------*
    // * Feature: username

    /**
     * getter for username - gets The name of the user to whom the annotations in this CAS belong.
     * 
     * @generated
     * @return value of the feature
     */
    public String getUsername()
    {
        if (CASMetadata_Type.featOkTst && ((CASMetadata_Type) jcasType).casFeat_username == null) {
            jcasType.jcas.throwFeatMissing("username",
                    "de.tudarmstadt.ukp.clarin.webanno.api.type.CASMetadata");
        }
        return jcasType.ll_cas.ll_getStringValue(addr,
                ((CASMetadata_Type) jcasType).casFeatCode_username);
    }

    /**
     * setter for username - sets The name of the user to whom the annotations in this CAS belong.
     * 
     * @generated
     * @param v
     *            value to set into the feature
     */
    public void setUsername(String v)
    {
        if (CASMetadata_Type.featOkTst && ((CASMetadata_Type) jcasType).casFeat_username == null) {
            jcasType.jcas.throwFeatMissing("username",
                    "de.tudarmstadt.ukp.clarin.webanno.api.type.CASMetadata");
        }
        jcasType.ll_cas.ll_setStringValue(addr, ((CASMetadata_Type) jcasType).casFeatCode_username,
                v);
    }

    // *--------------*
    // * Feature: lastChangedOnDisk

    /**
     * getter for lastChangedOnDisk - gets When a CAS is loaded, the last-changed timestamp of the
     * CAS file on disk is stored here. This is used to detect whether the CAS file has concurrently
     * changed when an attempt is made to write the CAS back to disk. A value of -1 indicates that
     * the CAS has never been stored to disk so far.
     * 
     * @generated
     * @return value of the feature
     */
    public long getLastChangedOnDisk()
    {
        if (CASMetadata_Type.featOkTst
                && ((CASMetadata_Type) jcasType).casFeat_lastChangedOnDisk == null) {
            jcasType.jcas.throwFeatMissing("lastChangedOnDisk",
                    "de.tudarmstadt.ukp.clarin.webanno.api.type.CASMetadata");
        }
        return jcasType.ll_cas.ll_getLongValue(addr,
                ((CASMetadata_Type) jcasType).casFeatCode_lastChangedOnDisk);
    }

    /**
     * setter for lastChangedOnDisk - sets When a CAS is loaded, the last-changed timestamp of the
     * CAS file on disk is stored here. This is used to detect whether the CAS file has concurrently
     * changed when an attempt is made to write the CAS back to disk. A value of -1 indicates that
     * the CAS has never been stored to disk so far.
     * 
     * @generated
     * @param v
     *            value to set into the feature
     */
    public void setLastChangedOnDisk(long v)
    {
        if (CASMetadata_Type.featOkTst
                && ((CASMetadata_Type) jcasType).casFeat_lastChangedOnDisk == null) {
            jcasType.jcas.throwFeatMissing("lastChangedOnDisk",
                    "de.tudarmstadt.ukp.clarin.webanno.api.type.CASMetadata");
        }
        jcasType.ll_cas.ll_setLongValue(addr,
                ((CASMetadata_Type) jcasType).casFeatCode_lastChangedOnDisk, v);
    }
}
