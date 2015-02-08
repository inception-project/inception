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
/* First created by JCasGen Thu May 23 10:29:48 CEST 2013 */
package de.tudarmstadt.ukp.webanno.type;

import org.apache.uima.jcas.JCas; 
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.jcas.cas.TOP_Type;

import org.apache.uima.jcas.tcas.Annotation;


/** 
 * Updated by JCasGen Thu May 23 10:29:48 CEST 2013
 * XML source: /home/likewise-open/UKP/yimam/dkproworkspace/de.tudarmstadt.ukp.clarin.webanno/de.tudarmstadt.ukp.clarin.webanno.uima/src/main/resources/desc/type/curation-state.xml
 * @generated */
public class CurationState extends Annotation {
  /** @generated
   * @ordered 
   */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = JCasRegistry.register(CurationState.class);
  /** @generated
   * @ordered 
   */
  @SuppressWarnings ("hiding")
  public final static int type = typeIndexID;
  /** @generated  */
  @Override
  public              int getTypeIndexID() {return typeIndexID;}
 
  /** Never called.  Disable default constructor
   * @generated */
  protected CurationState() {/* intentionally empty block */}
    
  /** Internal - constructor used by generator 
   * @generated */
  public CurationState(int addr, TOP_Type type) {
    super(addr, type);
    readObject();
  }
  
  /** @generated */
  public CurationState(JCas jcas) {
    super(jcas);
    readObject();   
  } 

  /** @generated */  
  public CurationState(JCas jcas, int begin, int end) {
    super(jcas);
    setBegin(begin);
    setEnd(end);
    readObject();
  }   

  /** <!-- begin-user-doc -->
    * Write your own initialization here
    * <!-- end-user-doc -->
  @generated modifiable */
  private void readObject() {/*default - does nothing empty block */}
     
 
    
  //*--------------*
  //* Feature: target

  /** getter for target - gets Target, that has the state as a property
   * @generated */
  public Annotation getTarget() {
    if (CurationState_Type.featOkTst && ((CurationState_Type)jcasType).casFeat_target == null)
      jcasType.jcas.throwFeatMissing("target", "de.tudarmstadt.ukp.webanno.type.CurationState");
    return (Annotation)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr, ((CurationState_Type)jcasType).casFeatCode_target)));}
    
  /** setter for target - sets Target, that has the state as a property 
   * @generated */
  public void setTarget(Annotation v) {
    if (CurationState_Type.featOkTst && ((CurationState_Type)jcasType).casFeat_target == null)
      jcasType.jcas.throwFeatMissing("target", "de.tudarmstadt.ukp.webanno.type.CurationState");
    jcasType.ll_cas.ll_setRefValue(addr, ((CurationState_Type)jcasType).casFeatCode_target, jcasType.ll_cas.ll_getFSRef(v));}    
   
    
  //*--------------*
  //* Feature: state

  /** getter for state - gets State of curation, 0 = NOT_FINISHED, 1 = FINISHED
   * @generated */
  public int getState() {
    if (CurationState_Type.featOkTst && ((CurationState_Type)jcasType).casFeat_state == null)
      jcasType.jcas.throwFeatMissing("state", "de.tudarmstadt.ukp.webanno.type.CurationState");
    return jcasType.ll_cas.ll_getIntValue(addr, ((CurationState_Type)jcasType).casFeatCode_state);}
    
  /** setter for state - sets State of curation, 0 = NOT_FINISHED, 1 = FINISHED 
   * @generated */
  public void setState(int v) {
    if (CurationState_Type.featOkTst && ((CurationState_Type)jcasType).casFeat_state == null)
      jcasType.jcas.throwFeatMissing("state", "de.tudarmstadt.ukp.webanno.type.CurationState");
    jcasType.ll_cas.ll_setIntValue(addr, ((CurationState_Type)jcasType).casFeatCode_state, v);}    
  }

    
