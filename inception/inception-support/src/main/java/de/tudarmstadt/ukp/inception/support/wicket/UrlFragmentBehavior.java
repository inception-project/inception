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

import java.lang.invoke.MethodHandles;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import org.apache.wicket.Page;
import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.request.IRequestParameters;
import org.danekja.java.util.function.serializable.SerializableBiConsumer;
import org.danekja.java.util.function.serializable.SerializableSupplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wicketstuff.urlfragment.UrlFragment;
import org.wicketstuff.urlfragment.UrlParametersReceivingBehavior;

/**
 * Keeps the URL fragment of the browser and the state of the component it is attached to in sync -
 * in both directions.
 * <p>
 * Incoming parameters (i.e. the user opening a bookmarked URL, editing the URL or using the
 * back/forward buttons) are passed to the parameter handler given to the constructor.
 * <p>
 * Outgoing parameters are obtained from the parameter supplier given to the constructor. Whenever
 * {@link #update} is called with an AJAX target, the parameters are recomputed at the end of the
 * request and - if they differ from the ones that were last written - pushed to the browser.
 * Parameters with a {@code null} value are removed from the URL fragment.
 * <p>
 * The parameters are only written if they actually changed. Setting them unnecessarily would
 * trigger another AJAX request from the browser telling us that the parameters were updated.
 * <p>
 * There is only one URL fragment per page, so this behavior can only be added to a page - and it
 * should be kept for the lifetime of that page since it needs to remember the parameters it has
 * last written.
 */
public class UrlFragmentBehavior
    extends UrlParametersReceivingBehavior
{
    private static final long serialVersionUID = 5735860644173611402L;

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final SerializableSupplier<Map<String, Object>> parametersSupplier;
    private final SerializableBiConsumer<IRequestParameters, AjaxRequestTarget> parameterHandler;

    // The parameters which were last written to the URL fragment. Used to determine whether an
    // update of the URL fragment is necessary at all.
    private Map<String, String> lastParameters;

    /**
     * @param aParametersSupplier
     *            supplies the parameters that the URL fragment should carry in the current state.
     *            Parameters with a {@code null} value are removed from the URL fragment.
     * @param aParameterHandler
     *            called when URL fragment parameters come in from the browser.
     */
    public UrlFragmentBehavior(SerializableSupplier<Map<String, Object>> aParametersSupplier,
            SerializableBiConsumer<IRequestParameters, AjaxRequestTarget> aParameterHandler)
    {
        parametersSupplier = aParametersSupplier;
        parameterHandler = aParameterHandler;
    }

    @Override
    protected void onBind()
    {
        super.onBind();

        // There is only one URL fragment per page, so this behavior only makes sense on a page.
        // Binding it to a component would also mean that it goes away when that component is
        // replaced while the URL fragment it has written stays.
        if (!(getComponent() instanceof Page)) {
            throw new WicketRuntimeException(
                    "[" + getClass().getSimpleName() + "] can only be added to a page, but was "
                            + "added to [" + getComponent().getClass().getName() + "]");
        }
    }

    @Override
    protected void onParameterArrival(IRequestParameters aRequestParameters,
            AjaxRequestTarget aTarget)
    {
        parameterHandler.accept(aRequestParameters, aTarget);
    }

    /**
     * Request that the URL fragment be brought in sync with the current state at the end of the
     * given AJAX request.
     *
     * @param aTarget
     *            the AJAX request target. If this is {@code null}, nothing happens since there is
     *            no AJAX response through which the browser could be updated.
     */
    public void update(AjaxRequestTarget aTarget)
    {
        // No AJAX request - nothing to do
        if (aTarget == null) {
            return;
        }

        try {
            aTarget.registerRespondListener(new UrlFragmentUpdateListener());
        }
        catch (Exception e) {
            LOG.debug("Unable to request URL fragment update anymore", e);
        }
    }

    /**
     * Forget the parameters that were last written to the URL fragment. The next {@link #update}
     * will then write the parameters again even if they did not change.
     */
    public void reset()
    {
        lastParameters = null;
    }

    private void updateUrlFragment(AjaxRequestTarget aTarget)
    {
        var parameters = new LinkedHashMap<String, String>();
        for (var parameter : parametersSupplier.get().entrySet()) {
            var value = parameter.getValue();
            parameters.put(parameter.getKey(), value != null ? String.valueOf(value) : null);
        }

        // Check if the relevant parameters have actually changed since the URL parameters were
        // last set - if this is not the case, then let's not set the parameters because that
        // triggers another AJAX request telling us that the parameters were updated (stupid,
        // right?)
        if (Objects.equals(lastParameters, parameters)) {
            return;
        }

        lastParameters = parameters;

        var fragment = new UrlFragment(aTarget);
        for (var parameter : parameters.entrySet()) {
            if (parameter.getValue() != null) {
                fragment.putParameter(parameter.getKey(), parameter.getValue());
            }
            else {
                fragment.removeParameter(parameter.getKey());
            }
        }

        // If we do not manually set editedFragment to false, then changing the URL
        // manually or using the back/forward buttons in the browser only works every
        // second time. Might be a bug in wicketstuff urlfragment... not sure.
        aTarget.appendJavaScript(
                "try{if(window.UrlUtil){window.UrlUtil.editedFragment = false;}}catch(e){}");
    }

    /**
     * This is a special AJAX target response listener which implements hashCode and equals. It uses
     * the identity of the behavior it belongs to. This enables us to add multiple instances of this
     * listener to an AJAX response without *actually* adding multiple instances since the AJAX
     * response internally keeps track of the listeners using a set.
     * <p>
     * Since the behavior can only be bound to a page and there is only one URL fragment per page,
     * this is the same as identifying the update by the page.
     */
    private class UrlFragmentUpdateListener
        implements AjaxRequestTarget.ITargetRespondListener
    {
        @Override
        public void onTargetRespond(AjaxRequestTarget aTarget)
        {
            // Check if the behavior is still attached to a component
            if (getComponent() == null) {
                return;
            }

            updateUrlFragment(aTarget);
        }

        private UrlFragmentBehavior getOuterType()
        {
            return UrlFragmentBehavior.this;
        }

        @Override
        public int hashCode()
        {
            return System.identityHashCode(getOuterType());
        }

        @Override
        public boolean equals(Object aObj)
        {
            if (this == aObj) {
                return true;
            }

            if (!(aObj instanceof UrlFragmentUpdateListener)) {
                return false;
            }

            return getOuterType() == ((UrlFragmentUpdateListener) aObj).getOuterType();
        }
    }
}
