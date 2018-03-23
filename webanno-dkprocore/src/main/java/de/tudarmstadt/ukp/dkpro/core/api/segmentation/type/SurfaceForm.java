/*
 * Copyright 2016
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
// CHECKSTYLE:OFF

/* First created by JCasGen Fri Jun 17 19:10:16 CEST 2016 */
package de.tudarmstadt.ukp.dkpro.core.api.segmentation.type;

import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.jcas.cas.TOP_Type;
import org.apache.uima.jcas.tcas.Annotation;

/** This annotation can be used to indicate an alternate surface form. E.g. some corpora consider a normalized form of the text with resolved contractions as the canonical form and only maintain the original surface form as a secondary information. One example is the Conll-U format.
 * Updated by JCasGen Fri Jun 17 19:10:16 CEST 2016
 * XML source: /Users/bluefire/git/webanno/webanno-dkprocore/src/main/resources/desc/type/Backports.xml
 * @generated */
public class SurfaceForm extends Annotation {
  /** @generated
   * @ordered 
   */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = JCasRegistry.register(SurfaceForm.class);
  /** @generated
   * @ordered 
   */
  @SuppressWarnings ("hiding")
  public final static int type = typeIndexID;
  /** @generated
   * @return index of the type  
   */
  @Override
  public              int getTypeIndexID() {return typeIndexID;}
 
  /** Never called.  Disable default constructor
   * @generated */
  protected SurfaceForm() {/* intentionally empty block */}
    
  /** Internal - constructor used by generator 
   * @generated
   * @param addr low level Feature Structure reference
   * @param type the type of this Feature Structure 
   */
  public SurfaceForm(int addr, TOP_Type type) {
    super(addr, type);
    readObject();
  }
  
  /** @generated
   * @param jcas JCas to which this Feature Structure belongs 
   */
  public SurfaceForm(JCas jcas) {
    super(jcas);
    readObject();   
  } 

  /** @generated
   * @param jcas JCas to which this Feature Structure belongs
   * @param begin offset to the begin spot in the SofA
   * @param end offset to the end spot in the SofA 
  */  
  public SurfaceForm(JCas jcas, int begin, int end) {
    super(jcas);
    setBegin(begin);
    setEnd(end);
    readObject();
  }   

  /** 
   * <!-- begin-user-doc -->
   * Write your own initialization here
   * <!-- end-user-doc -->
   *
   * @generated modifiable 
   */
  private void readObject() {/*default - does nothing empty block */}
     
 
    
  //*--------------*
  //* Feature: value

  /** getter for value - gets Alternate surface form.
   * @generated
   * @return value of the feature 
   */
  public String getValue() {
    if (SurfaceForm_Type.featOkTst && ((SurfaceForm_Type)jcasType).casFeat_value == null) {
        jcasType.jcas.throwFeatMissing("value", "de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.SurfaceForm");
    }
    return jcasType.ll_cas.ll_getStringValue(addr, ((SurfaceForm_Type)jcasType).casFeatCode_value);}
    
  /** setter for value - sets Alternate surface form. 
   * @generated
   * @param v value to set into the feature 
   */
  public void setValue(String v) {
    if (SurfaceForm_Type.featOkTst && ((SurfaceForm_Type)jcasType).casFeat_value == null) {
        jcasType.jcas.throwFeatMissing("value", "de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.SurfaceForm");
    }
    jcasType.ll_cas.ll_setStringValue(addr, ((SurfaceForm_Type)jcasType).casFeatCode_value, v);}    
  }

