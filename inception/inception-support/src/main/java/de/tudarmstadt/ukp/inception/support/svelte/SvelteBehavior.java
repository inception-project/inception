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
package de.tudarmstadt.ukp.inception.support.svelte;

import static de.tudarmstadt.ukp.clarin.webanno.support.JSONUtil.toInterpretableJsonString;

import java.io.IOException;
import java.lang.invoke.MethodHandles;

import javax.servlet.ServletContext;

import org.apache.wicket.Component;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.core.request.handler.IPartialPageRequestHandler;
import org.apache.wicket.markup.head.CssReferenceHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.request.IRequestHandler;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.handler.resource.ResourceReferenceRequestHandler;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.request.resource.ResourceReference;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.support.wicket.WicketUtil;

public class SvelteBehavior
    extends Behavior
{
    private static final long serialVersionUID = -5776151696499243799L;

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private @SpringBean ServletContext context;

    private ResourceReference jsRef;
    private ResourceReference cssRef;
    private Component host;

    public SvelteBehavior()
    {
        this(null);
    }

    public SvelteBehavior(ResourceReference aRef)
    {
        jsRef = aRef;
    }

    @Override
    public void bind(Component aComponent)
    {
        if (host != null && host != aComponent) {
            throw new IllegalStateException("Behavior already bound to another component");
        }

        host = aComponent;

        aComponent.setOutputMarkupId(true);

        var componentClass = aComponent.getClass();

        if (jsRef == null) {
            jsRef = new PackageResourceReference(componentClass,
                    componentClass.getSimpleName() + ".min.js");
        }

        if (componentClass.getResource(componentClass.getSimpleName() + ".min.css") != null) {
            cssRef = new PackageResourceReference(componentClass,
                    componentClass.getSimpleName() + ".min.css");
        }
    }

    @Override
    public void renderHead(Component aComponent, IHeaderResponse aResponse)
    {
        if (cssRef != null) {
            aResponse.render(CssReferenceHeaderItem.forReference(cssRef));
        }

        LOG.trace("Sending initialization code for Svelte component [{}] to frontend",
                host.getClass().getName());
        aResponse.render(OnDomReadyHeaderItem.forScript(initScript(aComponent)));
    }

    @Override
    public void onRemove(Component aComponent)
    {
        LOG.trace("Sending destruction code for Svelte component [{}] to frontend",
                host.getClass().getName());
        aComponent.getRequestCycle().find(IPartialPageRequestHandler.class)
                .ifPresent(target -> target.prependJavaScript(destroyScript(aComponent)));

    }

    private CharSequence initScript(Component aComponent)
    {
        IRequestHandler handler = new ResourceReferenceRequestHandler(jsRef);
        String url = RequestCycle.get().urlFor(handler).toString();

        Object model = aComponent.getDefaultModelObject();
        String propsJson;
        try {
            propsJson = model != null ? toInterpretableJsonString(model) : "{}";
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }

        String id = aComponent.getMarkupId();
        return String.join("\n", //
                "{", //
                "  let element = document.getElementById('" + id + "');", //
                // " if (Object.hasOwn(element, '$destroy')) {", //
                // " element.$destroy();", //
                // " delete element.$destroy;", //
                // " console.log('Svelte component on element [" + id + "] was destroyed');", //
                // " }", //
                "  import('" + url + "')", //
                "    .then(module => { ", //
                "      let component = new module.default({", //
                "        target: element,", //
                "        props: " + propsJson, //
                "      });", //
                "      element.$destroy = () => component.$destroy()", //
                "    })", //
                "}" //
        );
    }

    private CharSequence destroyScript(Component aComponent)
    {
        String id = aComponent.getMarkupId();
        return WicketUtil.wrapInTryCatch(String.join("\n", //
                "let element = document.getElementById('" + id + "');", //
                "element.$destroy();", //
                "delete element.$destroy;", //
                "console.log('Svelte component on element [" + id + "] was destroyed');"));
    }
}
