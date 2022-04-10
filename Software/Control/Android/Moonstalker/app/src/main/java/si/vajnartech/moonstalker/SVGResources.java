package si.vajnartech.moonstalker;

import android.content.res.Resources;

import si.vajnartech.moonstalker.androidsvg.SVGParser;

@SuppressWarnings("deprecation")
public class SVGResources extends Resources implements SVGParser.CustomValueHandler
{

  SVGResources(Resources base)
  {
    super(base.getAssets(), base.getDisplayMetrics(), base.getConfiguration());
    SVGParser.mCustomHandler = this;
  }

  @Override
  public Integer CustomColour(String name)
  {
    int id = getIdentifier(name, "color", getResourcePackageName(R.color.colorAccent));
    if (id != 0)
      return getColor(id) | 0xff000000;
    return null;
  }

  @Override
  public String transformStyle(String propertyName)
  {
    return propertyName;
  }

}
