package si.vajnartech.moonstalker;

import androidx.annotation.NonNull;

// To je lokalni astronomski objekt za UI
public class AstroObject
{
  public String name;
  public String ra;
  public String dec;
  public String constellation;
  public String azimuth, altitude;

  public AstroObject(AstroObject obj)
  {
    this.name = obj.name;
    this.ra = obj.ra;
    this.dec = obj.dec;
    this.constellation = obj.constellation;
    this.altitude = "";
    this.azimuth = "";
  }

  public AstroObject(String name, String ra, String dec, String constellation)
  {
    this.name = name;
    this.ra = ra;
    this.dec = dec;
    this.constellation = constellation;
  }

  public void setPosition(String alt, String az)
  {
    altitude = alt;
    azimuth = az;
  }

  @NonNull
  @Override
  public String toString()
  {
    return (name + " " + ra + " " + dec);
  }
}
