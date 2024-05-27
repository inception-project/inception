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
package de.tudarmstadt.ukp.inception.support.wicket;

import org.apache.wicket.Component;
import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.inception.support.lambda.AjaxCallback;

/**
 * This is a special AJAX target response listener which implements hashCode and equals. It uses the
 * markup ID of its host component to identify itself. This enables us to add multiple instances of
 * this listener to an AJAX response without *actually* adding multiple instances since the AJAX
 * response internally keeps track of the listeners using a set.
 */
public class AjaxComponentRespondListener
    implements AjaxRequestTarget.ITargetRespondListener
{
    private final Logger log = LoggerFactory.getLogger(getClass());

    private Component owner;
    private AjaxCallback callback;
    private StackTraceElement[] creationStack;

    public AjaxComponentRespondListener(Component aOwner, AjaxCallback aCallback)
    {
        owner = aOwner;
        creationStack = new Exception().getStackTrace();
        callback = aCallback;
    }

    @Override
    public void onTargetRespond(AjaxRequestTarget aTarget)
    {
        // Check if the annotation editor is still on screen
        try {
            // Would have been nice if we could have used findPage() here, but that is unfortunately
            // a protected method.
            owner.getPage();
        }
        catch (WicketRuntimeException e) {
            return;
        }

        try {
            callback.accept(aTarget);
        }
        catch (Exception e) {
            log.error("Unable to trigger respond listener callback", e);
        }
    }

    public StackTraceElement[] getCreationStack()
    {
        return creationStack;
    }

    @Override
    public int hashCode()
    {
        return owner.hashCode();
    }

    @Override
    public boolean equals(Object obj)
    {
        if (!(obj instanceof AjaxComponentRespondListener)) {
            return false;
        }

        AjaxComponentRespondListener other = (AjaxComponentRespondListener) obj;
        return owner == other.owner;
    }
}
