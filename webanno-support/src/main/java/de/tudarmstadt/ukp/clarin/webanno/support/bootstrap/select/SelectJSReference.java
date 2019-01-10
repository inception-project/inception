package de.tudarmstadt.ukp.clarin.webanno.support.bootstrap.select;

import org.apache.wicket.resource.JQueryPluginResourceReference;

/**
 * @author Alexey Volkov
 * @since 02.11.14
 */
public class SelectJSReference extends JQueryPluginResourceReference {

    private static final long serialVersionUID = 1L;

    /**
     * Singleton instance of this reference
     */
    private static final SelectJSReference INSTANCE = new SelectJSReference();

    /**
     * @return the single instance of the resource reference
     */
    public static SelectJSReference instance() {
        return INSTANCE;
    }

    /**
     * Private constructor.
     */
    private SelectJSReference() {
        super(SelectJSReference.class, "js/bootstrap-select.js");
    }
}
