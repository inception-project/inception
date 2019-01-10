package de.tudarmstadt.ukp.clarin.webanno.support.bootstrap.select;

import org.apache.wicket.request.resource.CssResourceReference;

/**
 * Bootstrap select css reference
 *
 * @author Alexey Volkov
 * @since 02.11.14
 */
public class SelectCSSReference extends CssResourceReference {

    private static final long serialVersionUID = 1L;

    /**
     * Singleton instance of this reference
     */
    private static final SelectCSSReference INSTANCE = new SelectCSSReference();

    /**
     * @return the single instance of the resource reference
     */
    public static SelectCSSReference instance() {
        return INSTANCE;
    }

    private SelectCSSReference() {
        super(BootstrapSelectBehavior.class, "css/bootstrap-select.css");
    }
}
