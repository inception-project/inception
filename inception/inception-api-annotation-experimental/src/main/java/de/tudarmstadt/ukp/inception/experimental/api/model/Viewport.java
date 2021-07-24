package de.tudarmstadt.ukp.inception.experimental.api.model;

import java.util.List;

public class Viewport
{
    List<List<Integer>> viewport;

    public Viewport(List<List<Integer>> aViewport)
    {
        viewport = aViewport;
    }

    public List<List<Integer>> getViewport()
    {
        return viewport;
    }

    public void setViewport(List<List<Integer>> aViewport)
    {
        viewport = aViewport;
    }
}
