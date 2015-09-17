/*******************************************************************************
 * Copyright 2015
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
package de.tudarmstadt.ukp.clarin.webanno.constraints.evaluator;

import java.io.Serializable;
/***
 * Class for indicating whether Constraints affected this feature or not.
 * https://github.com/webanno/webanno/issues/46
 * 
 *
 */
public class RulesIndicator
    implements Serializable
{

    private static final long serialVersionUID = -5606299056181945134L;
    private int status= 0;
    private boolean areThereRules;
    
    public String getStatusColor(){
        if(status==1){
            return "red";
        }else if(status==2){
            return "yellow";
        }else if(status==3){
            return "green";
        }else{
            return "";
        }
    }
    
    public boolean areThereRules(){
        return areThereRules;
    }
    public void reset()
    {
        status=0;
        areThereRules=false;
        
    }

    //Sets if rules are there or not.
    public void setRulesExist(boolean existence){
        areThereRules=existence;
    }
    
    // if a feature is affected by a constraint but there is no tagset defined on
    // the feature. In such a case the constraints cannot reorder tags and have no effect.
    public void didntMatchAnyTag(){
        if(areThereRules && status!=2 && status!=3){
            status=1;
        }
    }
    
    // if a feature is affected by a constraint but no rule covers the feature
    // value, e.g. @Lemma.value = "go" -> aFrame = "going". Here aFrame is affected by a
    // constraint. However, if the actual lemma annotated in the document is walk and there is
    // no rule that covers walk, then we should also indicate that.
    public void didntMatchAnyRule(){
        if(areThereRules && status!=3 && status!=1){
            status =2;
        }
    }
    
    // for case that a constrained actually applied ok there should be a marker.
    public void rulesApplied(){
       status = 3;
    }

    /**
     * https://github.com/webanno/webanno/issues/46
     * @return status symbols in fontawesome 
     */
    public String getStatusSymbol()
    {
        if(status==1){ //red
            return "fa fa-exclamation-circle";
        }else if(status==2){ //yellow
            return "fa fa-info-circle";
        }else if(status==3){ //green
            return "fa fa-check-circle";
        }
        return "";
        
    }
}

