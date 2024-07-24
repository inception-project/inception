/*
 * Copyright 2017
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 * 
 * Licensed to the Technische Universität Darmstadt under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The Technische Universität Darmstadt 
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.
 *  
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.inception.pdfeditor2.config;

import static de.tudarmstadt.ukp.inception.security.config.InceptionSecurityWebUIApiAutoConfiguration.BASE_VIEW_URL;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class PdfAnnotationEditor2WebMvcConfiguration
    implements WebMvcConfigurer
{
    public static final String BASE_URL = BASE_VIEW_URL + "/pdfeditor2";

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry aRegistry)
    {
        aRegistry.addResourceHandler(BASE_URL + "/**") //
                .addResourceLocations(
                        "classpath:/de/tudarmstadt/ukp/inception/pdfeditor2/resources/");
    }
}
