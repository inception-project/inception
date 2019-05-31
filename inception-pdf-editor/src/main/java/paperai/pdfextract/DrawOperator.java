package paperai.pdfextract;

public class DrawOperator
{
    String type;
    float[] values;

    public DrawOperator(String type, float[] values)
    {
        this.type = type;
        this.values = values;
    }
}
