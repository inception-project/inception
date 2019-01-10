package de.tudarmstadt.ukp.clarin.webanno.support.bootstrap.select;

import java.util.List;

import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.model.IModel;

/**
 * Bootstrap single choice select component.
 * Integrates <a href="http://silviomoreto.github.io/bootstrap-select">bootstrap select picker</a>
 *
 * @author Alexey Volkov
 * @since 02.11.14
 */
public class BootstrapSelect<T> extends DropDownChoice<T> {

    private static final long serialVersionUID = 4621814604115011002L;

    private BootstrapSelectConfig config = new BootstrapSelectConfig().withMultiple(false);

    /**
     * Constructor.
     *
     * @param id See Component
     */
    public BootstrapSelect(String id) {
        super(id);
    }

    /**
     * Constructor.
     *
     * @param id      See Component
     * @param choices choices
     */
    public BootstrapSelect(String id, List<? extends T> choices) {
        super(id, choices);
    }

    /**
     * Constructor.
     *
     * @param id       See Component
     * @param choices  choices
     * @param renderer The rendering engine
     */
    public BootstrapSelect(String id, List<? extends T> choices,
            IChoiceRenderer<? super T> renderer) {
        super(id, choices, renderer);
    }

    /**
     * Constructor.
     *
     * @param id      See Component
     * @param model   See Component
     * @param choices choices
     */
    public BootstrapSelect(String id, IModel<T> model, List<? extends T> choices) {
        super(id, model, choices);
    }

    /**
     * Constructor.
     *
     * @param id       See Component
     * @param model    See Component
     * @param choices  The drop down choices
     * @param renderer renderer
     */
    public BootstrapSelect(String id, IModel<T> model, List<? extends T> choices,
            IChoiceRenderer<? super T> renderer) {
        super(id, model, choices, renderer);
    }

    /**
     * Constructor.
     *
     * @param id      See Component
     * @param choices choices
     */
    public BootstrapSelect(String id, IModel<? extends List<? extends T>> choices) {
        super(id, choices);
    }

    /**
     * Constructor.
     *
     * @param id      See Component
     * @param model   See Component
     * @param choices choices
     */
    public BootstrapSelect(String id, IModel<T> model, 
            IModel<? extends List<? extends T>> choices) {
        super(id, model, choices);
    }

    /**
     * Constructor.
     *
     * @param id       See Component
     * @param choices  The drop down choices
     * @param renderer renderer
     */
    public BootstrapSelect(String id, IModel<? extends List<? extends T>> choices,
            IChoiceRenderer<? super T> renderer) {
        super(id, choices, renderer);
    }

    /**
     * Constructor.
     *
     * @param id       See Component
     * @param model    See Component
     * @param choices  The drop down choices
     * @param renderer renderer
     */
    public BootstrapSelect(String id, IModel<T> model, IModel<? extends List<? extends T>> choices,
            IChoiceRenderer<? super T> renderer) {
        super(id, model, choices, renderer);
    }

    /**
     * @return current config
     */
    public BootstrapSelectConfig config() {
        return config;
    }

    /**
     * @param config config to use
     * @return current instance
     */
    public BootstrapSelect<T> with(BootstrapSelectConfig config) {
        if (config != null) {
            this.config = config;
        }
        this.config.withMultiple(false);
        return this;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        add(newBootstrapSelectBehavior(config));
    }

    /**
     * create new behavior by specified config
     *
     * @param config config
     * @return new instance of select behavior
     */
    protected BootstrapSelectBehavior newBootstrapSelectBehavior(BootstrapSelectConfig config) {
        return new BootstrapSelectBehavior(config);
    }

}
