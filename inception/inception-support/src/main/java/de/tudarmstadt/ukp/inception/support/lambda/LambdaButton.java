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
package de.tudarmstadt.ukp.inception.support.lambda;

import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.request.RequestHandlerExecutor.ReplaceHandlerException;
import org.danekja.java.misc.serializable.SerializableRunnable;
import org.danekja.java.util.function.serializable.SerializableConsumer;
import org.slf4j.LoggerFactory;

public class LambdaButton
    extends Button
{
    private static final long serialVersionUID = 3946442967075930557L;

    private SerializableRunnable action;
    private SerializableConsumer<Exception> exceptionHandler;
    private boolean triggerAfterSubmit;

    public LambdaButton(String aId, SerializableRunnable aAction)
    {
        this(aId, aAction, null);
    }

    public LambdaButton(String aId, SerializableRunnable aAction,
            SerializableConsumer<Exception> aExceptionHandler)
    {
        super(aId);
        action = aAction;
        exceptionHandler = aExceptionHandler;
    }

    public LambdaButton triggerAfterSubmit()
    {
        triggerAfterSubmit = true;
        return this;
    }

    @Override
    public void onSubmit()
    {
        if (!triggerAfterSubmit) {
            action();
        }
    }

    @Override
    public void onAfterSubmit()
    {
        if (triggerAfterSubmit) {
            action();
        }
    }

    private void action()
    {
        try {
            action.run();
        }
        catch (ReplaceHandlerException e) {
            // Let Wicket redirects still work
            throw e;
        }
        catch (Exception e) {
            if (exceptionHandler != null) {
                exceptionHandler.accept(e);
            }
            else {
                LoggerFactory.getLogger(getPage().getClass()).error("Error: " + e.getMessage(), e);
                error("Error: " + e.getMessage());
            }
        }
    }
}
