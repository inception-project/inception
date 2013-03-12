/*******************************************************************************
 * Copyright 2012
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package de.tudarmstadt.ukp.clarin.webanno.brat.display.model;

import java.util.ArrayList;

/**
 * List of {@link Offsets}. Required so Jackson knows the generic type of the list when converting
 * an array of offsets from JSON to Java.
 * 
 * @author Richard Eckart de Castilho
 */
public class OffsetsList
    extends ArrayList<Offsets>
{
    // See
    // http://stackoverflow.com/questions/6173182/spring-json-convert-a-typed-collection-like-listmypojo

    private static final long serialVersionUID = 1441338116416225186L;
}
