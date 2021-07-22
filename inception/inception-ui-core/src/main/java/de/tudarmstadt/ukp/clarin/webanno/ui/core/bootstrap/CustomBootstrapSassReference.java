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
package de.tudarmstadt.ukp.clarin.webanno.ui.core.bootstrap;

import java.lang.reflect.Proxy;
import java.util.Locale;

import org.apache.wicket.util.resource.IResourceStream;

import de.agilecoders.wicket.sass.SassPackageResource;
import de.agilecoders.wicket.sass.SassResourceReference;

public class CustomBootstrapSassReference
    extends SassResourceReference
{
    private static final long serialVersionUID = 1L;

    private static final CustomBootstrapSassReference INSTANCE = new CustomBootstrapSassReference();

    /**
     * Gets the instance of the resource reference
     *
     * @return the single instance of the resource reference
     */
    public static CustomBootstrapSassReference get()
    {
        return INSTANCE;
    }

    /**
     * Private constructor
     */
    private CustomBootstrapSassReference()
    {
        super(CustomBootstrapSassReference.class, "bootstrap.scss");
    }

    @Override
    public SassPackageResource getResource()
    {
        return new CustomSassPackageResource(getScope(), getName(), getLocale(), getStyle(),
                getVariation());
    }

    /**
     * Custom resource to fix https://gitlab.com/jsass/jsass/-/issues/95
     */
    private class CustomSassPackageResource
        extends SassPackageResource
    {
        private static final long serialVersionUID = 8931106544015083514L;

        public CustomSassPackageResource(Class<?> scope, String name, Locale locale, String style,
                String variation)
        {
            super(scope, name, locale, style, variation);
        }

        @Override
        public IResourceStream getResourceStream()
        {
            IResourceStream resourceStream = super.getResourceStream();

            return (IResourceStream) Proxy.newProxyInstance(
                    resourceStream.getClass().getClassLoader(),
                    new Class[] { IResourceStream.class }, (proxy, method, args) -> {
                        if (method.getName().equals("getString")) {
                            return ((String) method.invoke(resourceStream, args)).replace(
                                    "$jsass-void: jsass_import_stack_push(",
                                    "$jsass-void: null;%n$jsass-void: jsass_import_stack_push(");
                        }

                        return method.invoke(resourceStream, args);
                    });
        }
    }
}
