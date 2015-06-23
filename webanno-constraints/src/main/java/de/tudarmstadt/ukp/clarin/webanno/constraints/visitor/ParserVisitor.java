package de.tudarmstadt.ukp.clarin.webanno.constraints.visitor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import de.tudarmstadt.ukp.clarin.webanno.constraints.grammar.syntaxtree.Parse;
import de.tudarmstadt.ukp.clarin.webanno.constraints.grammar.visitor.GJNoArguDepthFirst;
import de.tudarmstadt.ukp.clarin.webanno.constraints.model.ParsedConstraints;
import de.tudarmstadt.ukp.clarin.webanno.constraints.model.Scope;

public class ParserVisitor
    extends GJNoArguDepthFirst<ParsedConstraints>
{
    private Map<String, String> imports = new LinkedHashMap<>();
    private List<Scope> scopes = new ArrayList<>();

    @Override
    public ParsedConstraints visit(Parse n)
    {

        n.accept(new ImportVisitor(), imports);
        n.accept(new ScopeVisitor(), scopes);

        return new ParsedConstraints(imports, scopes);
    }
}
