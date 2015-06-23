package de.tudarmstadt.ukp.clarin.webanno.constraints.visitor;

import java.util.List;

import de.tudarmstadt.ukp.clarin.webanno.constraints.grammar.syntaxtree.NodeOptional;
import de.tudarmstadt.ukp.clarin.webanno.constraints.grammar.syntaxtree.NodeToken;
import de.tudarmstadt.ukp.clarin.webanno.constraints.grammar.syntaxtree.Path;
import de.tudarmstadt.ukp.clarin.webanno.constraints.grammar.visitor.DepthFirstVisitor;
import de.tudarmstadt.ukp.clarin.webanno.constraints.grammar.visitor.GJVoidDepthFirst;
import de.tudarmstadt.ukp.clarin.webanno.constraints.model.Restriction;

public class RestrictionVisitor
    extends GJVoidDepthFirst<List<Restriction>>
{
    private String path;
    private String value;
    private boolean flagImportant;

    @Override
    public void visit(
            de.tudarmstadt.ukp.clarin.webanno.constraints.grammar.syntaxtree.Restriction aN,
            List<Restriction> aArgu)
    {
        path = null;
        value = QuoteUtil.unquote(aN.f2.tokenImage);
        flagImportant = false;

        super.visit(aN, aArgu);

        aArgu.add(new Restriction(path, value, flagImportant));
    }

    @Override
    public void visit(final NodeOptional n, List<Restriction> argu)
    {
        super.visit(n, argu);

        n.accept(new DepthFirstVisitor()
        {
            @Override
            public void visit(NodeToken aN)
            {
                if (n.present()) { // If flag is there
                    flagImportant = true;
                }
            }
        });
    }

    @Override
    public void visit(Path aN, List<Restriction> aArgu)
    {
        super.visit(aN, aArgu);

        aN.accept(new DepthFirstVisitor()
        {
            @Override
            public void visit(NodeToken aN)
            {
                path = aN.tokenImage;
            }
        });
    }
}
