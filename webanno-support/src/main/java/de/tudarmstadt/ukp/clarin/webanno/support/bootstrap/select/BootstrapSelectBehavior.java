package de.tudarmstadt.ukp.clarin.webanno.support.bootstrap.select;

import static de.agilecoders.wicket.jquery.JQuery.$;

import org.apache.wicket.Component;
import org.apache.wicket.core.request.handler.IPartialPageRequestHandler;
import org.apache.wicket.event.IEvent;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;

import de.agilecoders.wicket.core.markup.html.bootstrap.behavior.BootstrapJavascriptBehavior;
import de.agilecoders.wicket.jquery.function.AbstractFunction;
import de.agilecoders.wicket.jquery.function.IFunction;

/**
 * Bootstrap select behavior. Add js/css resources and updates views
 * integrates <a href="http://silviomoreto.github.io/bootstrap-select">bootstrap select picker</a>
 *
 * @author Alexey Volkov
 * @since 02.11.14
 */
public class BootstrapSelectBehavior extends BootstrapJavascriptBehavior {

    private static final long serialVersionUID = 4785647088660913269L;

    private static class DestroyScript extends AbstractFunction {

        private static final long serialVersionUID = 5744163685461085633L;

        private DestroyScript() {
            super("selectpicker");
            addParameter(toParameterValue("destroy"));
        }
    }

    // destroy script for component
    private static final IFunction destroyScript = new DestroyScript();

    private final BootstrapSelectConfig config;

    /**
     * @param config select config
     */
    public BootstrapSelectBehavior(BootstrapSelectConfig config) {
        this.config = config;
    }

    @Override
    public void onEvent(Component component, IEvent<?> event) {
        super.onEvent(component, event);
        if (event.getPayload() instanceof IPartialPageRequestHandler) {
            IPartialPageRequestHandler target = (IPartialPageRequestHandler) event.getPayload();
            if (target.getComponents().contains(component)) {
                // if this component is being repainted by ajax, directly, we must destroy bootstrap
                // select so it removes its elements from DOM
                target.prependJavaScript($(component).chain(destroyScript).get());
            }
        }
    }

    @Override
    public void renderHead(Component component, IHeaderResponse response) {
        super.renderHead(component, response);
        response.render(JavaScriptHeaderItem.forReference(SelectJSReference.instance()));
        renderCss(response);
        response.render($(component).chain("selectpicker", config).asDomReadyScript());
    }

    /**
     * render css resource
     *
     * @param response response
     */
    protected void renderCss(IHeaderResponse response) {
        response.render(CssHeaderItem.forReference(SelectCSSReference.instance()));
    }

}
