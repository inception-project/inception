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
package de.tudarmstadt.ukp.inception.support.wicket.input;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;

import java.util.HashMap;

import org.apache.wicket.Component;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnLoadHeaderItem;
import org.apache.wicket.util.template.PackageTextTemplate;
import org.apache.wicket.util.template.TextTemplate;

import wicket.contrib.input.events.EventType;
import wicket.contrib.input.events.key.KeyType;

/**
 * Replacement for the Wicketstuff {@link wicket.contrib.input.events.InputBehavior} class which
 * fixes an issue with event listeners accumulating.
 * 
 * @see <a href="https://github.com/wicketstuff/core/issues/711">Wicketstuff Issue #711</a>
 */
public class InputBehavior
    extends wicket.contrib.input.events.InputBehavior
{
    private static final long serialVersionUID = 3336108174893747098L;

    private final TextTemplate shortcutRemove = new PackageTextTemplate(InputBehavior.class,
            "remove-input-behavior.js");

    private final KeyType[] keyCombo;

    private Component target;

    public InputBehavior(KeyType[] aKeyCombo, EventType aEventType)
    {
        super(aKeyCombo, aEventType);
        this.keyCombo = aKeyCombo;
    }

    @Override
    public void renderHead(Component aC, IHeaderResponse aResponse)
    {
        aResponse.render(OnLoadHeaderItem.forScript(generateString(shortcutRemove)));

        super.renderHead(aC, aResponse);
    }

    private String generateString(TextTemplate textTemplate)
    {
        // variables for the initialization script
        var variables = new HashMap<String, Object>();
        variables.put("keys", stream(keyCombo).map(KeyType::getKeyCode).collect(joining("+")));
        textTemplate.interpolate(variables);
        return textTemplate.asString();
    }

    @Override
    protected String getTarget()
    {
        if (target != null) {
            return target.getMarkupId();
        }

        return super.getTarget();
    }

    public InputBehavior setTarget(Component aComponent)
    {
        aComponent.setOutputMarkupId(true);
        target = aComponent;
        return this;
    }
}
