package de.tudarmstadt.ukp.clarin.webanno.brat.message;

/**
 * Response for the {@code acceptAction} command.
 *
 * This command is part of WebAnno and not contained in the original brat.
 */
public class AcceptActionResponse
    extends AjaxResponse
{
    public static final String COMMAND = "acceptAction";

    public AcceptActionResponse()
    {
        super(COMMAND);
    }

    public static boolean is(String aCommand)
    {
        return COMMAND.equals(aCommand);
    }
}
