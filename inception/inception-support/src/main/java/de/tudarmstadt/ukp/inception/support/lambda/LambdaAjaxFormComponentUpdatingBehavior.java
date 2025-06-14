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

import java.time.Duration;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.attributes.AjaxCallListener;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.ajax.attributes.ThrottlingSettings;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.request.RequestHandlerExecutor.ReplaceHandlerException;
import org.slf4j.LoggerFactory;

public class LambdaAjaxFormComponentUpdatingBehavior
    extends AjaxFormComponentUpdatingBehavior
    implements HtmlElementEvents
{
    private static final long serialVersionUID = -8496566485055940375L;

    private AjaxCallback action;
    private AjaxExceptionHandler exceptionHandler;
    private ThrottlingSettings throttlingSettings;
    private int keyCode;

    public LambdaAjaxFormComponentUpdatingBehavior()
    {
        this(CHANGE_EVENT, null, null);
    }

    public LambdaAjaxFormComponentUpdatingBehavior(String aEvent)
    {
        this(aEvent, null, null);
    }

    public LambdaAjaxFormComponentUpdatingBehavior(String aEvent, AjaxCallback aAction)
    {
        this(aEvent, aAction, null);
    }

    public LambdaAjaxFormComponentUpdatingBehavior(String aEvent, AjaxCallback aAction,
            AjaxExceptionHandler aExceptionHandler)
    {
        super(aEvent);
        action = aAction;
        exceptionHandler = aExceptionHandler;
    }

    public LambdaAjaxFormComponentUpdatingBehavior withDebounce(Duration aDuration)
    {
        throttlingSettings = new ThrottlingSettings(aDuration, true);
        return this;
    }

    /**
     * @param aKeyCode
     *            a keycode
     * @return the behavior for chaining
     * @see KeyCodes
     */
    public LambdaAjaxFormComponentUpdatingBehavior withKeyCode(int aKeyCode)
    {
        keyCode = aKeyCode;
        return this;
    }

    @Override
    protected void updateAjaxAttributes(AjaxRequestAttributes attributes)
    {
        super.updateAjaxAttributes(attributes);
        attributes.setThrottlingSettings(throttlingSettings);

        if (keyCode != 0) {
            var listener = new AjaxCallListener()
            {
                private static final long serialVersionUID = 3873282863927185983L;

                @Override
                public CharSequence getPrecondition(Component component)
                {
                    return String.join("\n", //
                            "var keycode = Wicket.Event.keyCode(attrs.event);", //
                            "return keycode == " + keyCode + ";");
                }
            };
            attributes.getAjaxCallListeners().add(listener);
        }
    }

    @Override
    public void onUpdate(AjaxRequestTarget aTarget)
    {
        try {
            if (action != null) {
                action.accept(aTarget);
            }
        }
        catch (ReplaceHandlerException e) {
            // Let Wicket redirects still work
            throw e;
        }
        catch (Exception e) {
            if (exceptionHandler != null) {
                exceptionHandler.accept(aTarget, e);
            }
            else {
                Component component = getComponent();
                LoggerFactory.getLogger(component.getPage().getClass())
                        .error("Error: " + e.getMessage(), e);
                component.error("Error: " + e.getMessage());
                aTarget.addChildren(component.getPage(), IFeedback.class);
            }
        }
    }
}
