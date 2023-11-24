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
package de.tudarmstadt.ukp.inception.support.wicket.resource;

import java.lang.StackWalker.Option;
import java.util.Locale;
import java.util.Set;

import org.apache.wicket.Localizer;
import org.apache.wicket.resource.loader.PackageStringResourceLoader;

public class ContextSensitivePackageStringResourceLoader
    extends PackageStringResourceLoader
{
    private static final Set<String> IGNORED_PREFIXES = Set.of(
            ContextSensitivePackageStringResourceLoader.class.getPackageName() + ".",
            Localizer.class.getPackageName() + ".");

    @Override
    public String loadStringResource(Class<?> aClazz, String aKey, Locale aLocale, String aStyle,
            String aVariation)
    {
        var context = StackWalker.getInstance(Option.RETAIN_CLASS_REFERENCE)
                .walk(s -> s.dropWhile(
                        f -> IGNORED_PREFIXES.stream().anyMatch(f.getClassName()::startsWith))
                        .findFirst());

        return context.map(v -> super.loadStringResource(v.getDeclaringClass(), aKey, aLocale,
                aStyle, aVariation)).orElse(null);
    }
}
