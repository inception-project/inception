package de.tudarmstadt.ukp.inception.recommendation.regexrecommender;

public class Pair<L,R>
{
    private L left;
    private R right;

    public Pair(L aLeft, R aRight)
    {
        assert left != null;
        assert right != null;

        this.left = aLeft;
        this.right = aRight;
    }

    public L getLeft()
    {
        return left;
    }
    public R getRight()
    {
        return right;
    }
      
    public void setLeft(L aItem)
    {
        this.left = aItem;
    }
    
    public void setRight(R aItem)
    {
        this.right = aItem;
    }

    @Override
    public int hashCode()
    {
        return left.hashCode() ^ right.hashCode();
    }

    @Override
    public boolean equals(Object aObj)
    {
        if (!(aObj instanceof Pair)) return false;
        Pair pairo = (Pair) aObj;
        return this.left.equals(pairo.getLeft()) &&
               this.right.equals(pairo.getRight());
    }
    
    @Override
    public String toString()
    {
        return "Left: " + left.toString() + " Right: " + right.toString();
    }

}
