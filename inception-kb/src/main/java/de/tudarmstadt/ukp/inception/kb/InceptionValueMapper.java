package de.tudarmstadt.ukp.inception.kb;

import org.apache.commons.lang3.Validate;
import org.cyberborean.rdfbeans.datatype.DatatypeMapper;
import org.cyberborean.rdfbeans.datatype.DefaultDatatypeMapper;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.util.URIUtil;

import de.tudarmstadt.ukp.inception.kb.graph.KBStatement;

public class InceptionValueMapper {

    public Value mapStatementValue(KBStatement aStatement, ValueFactory vf)
    {
        Validate.notNull(aStatement, "Statement cannot be null");

        Object value = aStatement.getValue();
        String language = aStatement.getLanguage();

        if (value instanceof IRI) {
            return (IRI) value;
        }
        else if (value instanceof String && URIUtil.isValidURIReference((String) value)) {
            return vf.createIRI((String) value);
        }
        else if (language != null) {
            return vf.createLiteral((String) value, language);
        }
        else {
            DatatypeMapper mapper = new DefaultDatatypeMapper();
            return mapper.getRDFValue(value, vf);
        }
    }
}
