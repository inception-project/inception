package paperai.pdfextract;

public class TextOperator
{
    String unicode;
    float fx;
    float fy;
    float fw;
    float fh;
    float gx;
    float gy;
    float gw;
    float gh;

    public TextOperator(String unicode, float fx, float fy, float fw, float fh, float gx, float gy,
        float gw, float gh)
    {
        this.unicode = unicode;
        this.fx = fx;
        this.fy = fy;
        this.fw = fw;
        this.fh = fh;
        this.gx = gx;
        this.gy = gy;
        this.gw = gw;
        this.gh = gh;
    }
}
