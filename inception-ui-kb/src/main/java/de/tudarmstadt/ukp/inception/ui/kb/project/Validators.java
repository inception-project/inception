package de.tudarmstadt.ukp.inception.ui.kb.project;

import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.IValidator;
import org.apache.wicket.validation.ValidationError;
import org.apache.wicket.validation.validator.UrlValidator;
import org.eclipse.rdf4j.model.util.URIUtil;

public class Validators
{

    public static final IValidator<String> IRI_VALIDATOR = new IValidator<String>()
    {
        private static final long serialVersionUID = 7022579868551171981L;
    
        @Override
        public void validate(IValidatable<String> validatable)
        {
            if (!URIUtil.isValidURIReference(validatable.getValue())) {
                validatable.error(new ValidationError(this));
            }
        }
    };
    public static final UrlValidator URL_VALIDATOR = new UrlValidator(
    new String[] { "http", "https" });

    public Validators()
    {
        // TODO Auto-generated constructor stub
    }

}
