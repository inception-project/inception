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
package de.tudarmstadt.ukp.inception.support.vue;

import static de.tudarmstadt.ukp.clarin.webanno.support.JSONUtil.toInterpretableJsonString;
import static org.apache.wicket.markup.head.JavaScriptHeaderItem.forReference;
import static org.apache.wicket.markup.head.JavaScriptHeaderItem.forScript;

import java.io.IOException;

import javax.servlet.ServletContext;

import org.apache.wicket.Component;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.request.IRequestHandler;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.handler.resource.ResourceReferenceRequestHandler;
import org.apache.wicket.request.resource.JavaScriptResourceReference;
import org.apache.wicket.request.resource.ResourceReference;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.agilecoders.wicket.webjars.request.resource.WebjarsJavaScriptResourceReference;

public class VueBehavior
    extends Behavior
{
    private static final long serialVersionUID = -5776151696499243799L;

    private @SpringBean ServletContext context;

    private final ResourceReference ref;

    public VueBehavior(ResourceReference aRef)
    {
        super();
        ref = aRef;
    }

    @Override
    public void bind(Component aComponent)
    {
        aComponent.setOutputMarkupId(true);
    }

    @Override
    public void renderHead(Component aComponent, IHeaderResponse aResponse)
    {
        IRequestHandler handler = new ResourceReferenceRequestHandler(ref);
        String url = RequestCycle.get().urlFor(handler).toString();

        switch (aComponent.getApplication().getConfigurationType()) {
        case DEPLOYMENT:
            aResponse.render(forReference(
                    new WebjarsJavaScriptResourceReference("vue/current/dist/vue.global.prod.js")));
            break;
        case DEVELOPMENT:
            aResponse.render(forReference(
                    new WebjarsJavaScriptResourceReference("vue/current/dist/vue.global.js")));
            break;
        }

        JavaScriptResourceReference vue3SfcLoaderRef = new JavaScriptResourceReference(getClass(),
                "vue3-sfc-loader.min.js");
        aResponse.render(forReference(vue3SfcLoaderRef));

        Object model = aComponent.getDefaultModelObject();
        String propsJson;
        try {
            propsJson = model != null ? toInterpretableJsonString(model) : "{}";
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }

        StringBuilder script = new StringBuilder();
        script.append("const { loadModule } = window['vue3-sfc-loader'];\n");
        script.append("const options = {\n");
        script.append("  moduleCache: {\n");
        script.append("    vue: Vue\n");
        script.append("  },\n");
        script.append("  async getFile(url) {\n");
        script.append("    const res = await fetch(url);\n");
        script.append("    if ( !res.ok )\n");
        script.append("      throw Object.assign(new Error(url+' '+res.statusText), { res });\n");
        script.append("    return await res.text();\n");
        script.append("  },\n");
        script.append("  addStyle() {},\n");
        script.append("};\n");
        script.append("const app = Vue.createApp({\n");
        script.append("  data: () => { return {\n");
        script.append("    props: ").append(propsJson).append("\n");
        script.append("  } },\n");
        script.append("  components: {\n");
        script.append("    'VueComponent': Vue.defineAsyncComponent(() => loadModule('").append(url)
                .append("', options))\n");
        script.append("  },\n");
        script.append(" template: `<VueComponent v-bind='props'/>`\n");
        script.append("});\n");
        script.append(
                "document.addEventListener('DOMContentLoaded', () => { const vm = app.mount('#"
                        + aComponent.getMarkupId() + "') });\n");

        aResponse.render(forScript(script, aComponent.getMarkupId() + "-vue"));
    }
}
