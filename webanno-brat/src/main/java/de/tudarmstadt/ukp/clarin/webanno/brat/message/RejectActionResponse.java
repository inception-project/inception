package de.tudarmstadt.ukp.clarin.webanno.brat.message;

/**
 * Response for the {@code acceptAction} command.
 *
 * This command is part of WebAnno and not contained in the original brat.
 */
public class RejectActionResponse
    extends AjaxResponse
{
    public static final String COMMAND = "rejectAction";

    public RejectActionResponse()
    {
        super(COMMAND);
    }

    public static boolean is(String aCommand)
    {
        return COMMAND.equals(aCommand);
    }
}
