/*
 * Copyright (c) 2021 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */
package de.tudarmstadt.ukp.inception.kb.querybuilder.backport;

import org.eclipse.rdf4j.sparqlbuilder.core.Assignable;
import org.eclipse.rdf4j.sparqlbuilder.core.Variable;
import org.eclipse.rdf4j.sparqlbuilder.graphpattern.GraphPattern;
import org.eclipse.rdf4j.sparqlbuilder.util.SparqlBuilderUtils;

/**
 * @since 4.0.0
 * @author Florian Kleedorfer
 */
public class Bind
    implements GraphPattern
{
    private static final String AS = " AS ";
    private Assignable expression;
    private Variable var;

    public Bind(Assignable exp, Variable var)
    {
        this.expression = exp;
        this.var = var;
    }

    @Override
    public String getQueryString()
    {
        return "BIND" + SparqlBuilderUtils
                .getParenthesizedString(expression.getQueryString() + AS + var.getQueryString());
    }
}
