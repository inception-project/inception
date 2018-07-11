/*
 * Copyright 2017
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
package de.tudarmstadt.ukp.inception.recommendation.api;

/**
 * Defines if an object can contain a ClassifierConfiguration.
 * In general, all Trainer and Classifier classes extend the ConfigurableComponent class.
 * 
 *
 *
 * @param <T> The classifier specific training parameters object.
 */
public abstract class ConfigurableComponent<T>
{
    protected ClassifierConfiguration<T> conf;
    
    public ConfigurableComponent(ClassifierConfiguration<T> conf) {
        if (conf == null) {
            throw new IllegalArgumentException("ClassifierConfiguration cannot be null!");
        }
        
        setClassifierConfiguration(conf);
    }
    
    public void setClassifierConfiguration(ClassifierConfiguration<T> conf) {
        this.conf = conf;
        reconfigure();
    }
    
    public ClassifierConfiguration<T> getClassifierConfiguration() {
        return conf;
    }
    
    public abstract void reconfigure();
    
}
