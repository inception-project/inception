/*
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
package de.tudarmstadt.ukp.inception.support.extensionpoint;

import java.util.Optional;

public interface ContextLookupExtensionPoint<C, E extends Extension<C>>
    extends ExtensionPoint<C, E>
{
    /**
     * Get the extension for the given object.
     * 
     * @param aKey
     *            the object to get the extension for.
     * @return the feature support.
     * @throws IllegalArgumentException
     *             if there is no support for the given feature.
     */
    <X extends E> Optional<X> findGenericExtension(C aKey);
}
