package si.vajnartech.moonstalker;

import androidx.annotation.NonNull;

//@JsonIgnoreProperties(ignoreUnknown = true)
public class AstroObject
{
  public String name;
  public double ra;
  public double dec;
  public String sRa;
  public String sDec;
  public String constellation;

  public AstroObject(String name, double ra, double dec, String sRa, String sDec)
  {
    set(name, ra, dec);
    this.sRa = sRa;
    this.sDec = sDec;
  }

  void set(String name, double ra, double dec)
  {
    this.name = name;
    this.ra = ra;
    this.dec = dec;
  }

  @NonNull @Override
  public String toString()
  {
    return "AstroObject{" +
           "name=" + name +
           ", ra=" + ra +
           ", dec=" + dec +
           ", sRa=" + sRa +
           ", sDec=" + sDec +
           '}';
  }
}
