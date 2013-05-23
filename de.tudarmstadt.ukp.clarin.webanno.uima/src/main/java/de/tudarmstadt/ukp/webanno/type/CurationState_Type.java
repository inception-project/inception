
/* First created by JCasGen Thu May 23 10:29:50 CEST 2013 */
package de.tudarmstadt.ukp.webanno.type;

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
 * Updated by JCasGen Thu May 23 10:29:50 CEST 2013
 * @generated */
public class CurationState_Type extends Annotation_Type {
  /** @generated */
  @Override
  protected FSGenerator getFSGenerator() {return fsGenerator;}
  /** @generated */
  private final FSGenerator fsGenerator = 
    new FSGenerator() {
      public FeatureStructure createFS(int addr, CASImpl cas) {
  			 if (CurationState_Type.this.useExistingInstance) {
  			   // Return eq fs instance if already created
  		     FeatureStructure fs = CurationState_Type.this.jcas.getJfsFromCaddr(addr);
  		     if (null == fs) {
  		       fs = new CurationState(addr, CurationState_Type.this);
  			   CurationState_Type.this.jcas.putJfsFromCaddr(addr, fs);
  			   return fs;
  		     }
  		     return fs;
        } else return new CurationState(addr, CurationState_Type.this);
  	  }
    };
  /** @generated */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = CurationState.typeIndexID;
  /** @generated 
     @modifiable */
  @SuppressWarnings ("hiding")
  public final static boolean featOkTst = JCasRegistry.getFeatOkTst("de.tudarmstadt.ukp.webanno.type.CurationState");
 
  /** @generated */
  final Feature casFeat_target;
  /** @generated */
  final int     casFeatCode_target;
  /** @generated */ 
  public int getTarget(int addr) {
        if (featOkTst && casFeat_target == null)
      jcas.throwFeatMissing("target", "de.tudarmstadt.ukp.webanno.type.CurationState");
    return ll_cas.ll_getRefValue(addr, casFeatCode_target);
  }
  /** @generated */    
  public void setTarget(int addr, int v) {
        if (featOkTst && casFeat_target == null)
      jcas.throwFeatMissing("target", "de.tudarmstadt.ukp.webanno.type.CurationState");
    ll_cas.ll_setRefValue(addr, casFeatCode_target, v);}
    
  
 
  /** @generated */
  final Feature casFeat_state;
  /** @generated */
  final int     casFeatCode_state;
  /** @generated */ 
  public int getState(int addr) {
        if (featOkTst && casFeat_state == null)
      jcas.throwFeatMissing("state", "de.tudarmstadt.ukp.webanno.type.CurationState");
    return ll_cas.ll_getIntValue(addr, casFeatCode_state);
  }
  /** @generated */    
  public void setState(int addr, int v) {
        if (featOkTst && casFeat_state == null)
      jcas.throwFeatMissing("state", "de.tudarmstadt.ukp.webanno.type.CurationState");
    ll_cas.ll_setIntValue(addr, casFeatCode_state, v);}
    
  



  /** initialize variables to correspond with Cas Type and Features
	* @generated */
  public CurationState_Type(JCas jcas, Type casType) {
    super(jcas, casType);
    casImpl.getFSClassRegistry().addGeneratorForType((TypeImpl)this.casType, getFSGenerator());

 
    casFeat_target = jcas.getRequiredFeatureDE(casType, "target", "uima.tcas.Annotation", featOkTst);
    casFeatCode_target  = (null == casFeat_target) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_target).getCode();

 
    casFeat_state = jcas.getRequiredFeatureDE(casType, "state", "uima.cas.Integer", featOkTst);
    casFeatCode_state  = (null == casFeat_state) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_state).getCode();

  }
}



    