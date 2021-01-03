package de.tudarmstadt.ukp.inception.ui.core.dashboard.dashlet.vue;

import java.io.IOException;

import javax.servlet.ServletContext;

import org.apache.wicket.Component;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.request.IRequestHandler;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.handler.resource.ResourceReferenceRequestHandler;
import org.apache.wicket.request.resource.JavaScriptResourceReference;
import org.apache.wicket.request.resource.ResourceReference;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.agilecoders.wicket.webjars.request.resource.WebjarsJavaScriptResourceReference;
import de.tudarmstadt.ukp.clarin.webanno.support.JSONUtil;

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

        aResponse.render(JavaScriptHeaderItem.forReference(
                new WebjarsJavaScriptResourceReference("vue/current/dist/vue.global.js")));
        aResponse.render(JavaScriptHeaderItem
                .forReference(new JavaScriptResourceReference(getClass(), "vue3-sfc-loader.js")));

        Object model = aComponent.getDefaultModelObject();
        String propsJson;
        try {
            if (model == null) {
                propsJson = "{}";
            }
            else {
                propsJson = JSONUtil.toInterpretableJsonString(model);

            }
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }

        String script = String.join("\n", //
                "const { loadModule } = window['vue3-sfc-loader'];", //
                "const options = {", //
                "  moduleCache: {", //
                "    vue: Vue", //
                "  },", //
                "  async getFile(url) {", //
                "    const res = await fetch(url);", //
                "    if ( !res.ok )", //
                "      throw Object.assign(new Error(url+' '+res.statusText), { res });", //
                "    return await res.text();", //
                "  },", //
                "  addStyle() {},", //
                "}", //
                "const app = Vue.createApp({", //
                "  data: () => { return {", //
                "    props: " + propsJson, //
                "  } },", //
                "  components: {", //
                "    'VueComponent': Vue.defineAsyncComponent(() => loadModule('" + url
                        + "', options))", //
                "  },", //
                " template: `<VueComponent v-bind='props'/>`", //
                "})", //
                "document.addEventListener('DOMContentLoaded', () => { const vm = app.mount('#"
                        + aComponent.getMarkupId() + "') })");

        aResponse.render(JavaScriptHeaderItem.forScript(script, aComponent.getMarkupId() + "-vue"));
    }
}
