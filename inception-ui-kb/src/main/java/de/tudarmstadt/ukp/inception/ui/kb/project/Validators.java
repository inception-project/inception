/*
 * Copyright 2018
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
package de.tudarmstadt.ukp.inception.ui.kb.project;

import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.IValidator;
import org.apache.wicket.validation.ValidationError;
import org.apache.wicket.validation.validator.UrlValidator;
import org.eclipse.rdf4j.model.util.URIUtil;

public class Validators
{

    public static final IValidator<String> IRI_VALIDATOR = new IValidator<String>()
    {
        private static final long serialVersionUID = 7022579868551171981L;
    
        @Override
        public void validate(IValidatable<String> validatable)
        {
            if (!URIUtil.isValidURIReference(validatable.getValue())) {
                validatable.error(new ValidationError(this));
            }
        }
    };
    public static final UrlValidator URL_VALIDATOR = new UrlValidator(
        new String[] { "http", "https" });

}
