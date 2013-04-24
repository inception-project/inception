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

package de.tudarmstadt.ukp.clarin.webanno.webapp.page.annotation;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.model.Model;

/**
 *  Add javascript code to any existing javascript by extending to {@link AttributeModifier}
 *  Code used from cwiki at https://cwiki.apache.org/WICKET/getting-user-confirmation.html
 *
 */
public class JavascriptEventConfirmation extends AttributeModifier {

    private static final long serialVersionUID = 1432391524256371065L;

    public JavascriptEventConfirmation(String event, String msg) {
        super(event, new Model(msg));
    }

    @Override
    protected String newValue(final String currentValue, final String replacementValue) {
        String prefix = "var conf = confirm('" + replacementValue + "'); " +
            "if (!conf) return false; ";
        String result = prefix;
        if (currentValue != null) {
            result = prefix + currentValue;
        }
        return result;
    }
}