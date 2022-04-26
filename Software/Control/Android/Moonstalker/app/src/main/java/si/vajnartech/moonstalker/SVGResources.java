package si.vajnartech.moonstalker;

import android.content.res.Resources;

import si.vajnartech.moonstalker.androidsvg.SVGParser;

public class SVGResources extends Resources implements SVGParser.CustomValueHandler
{

  SVGResources(Resources base)
  {
    super(base.getAssets(), base.getDisplayMetrics(), base.getConfiguration());
    SVGParser.mCustomHandler = this;
  }

  @Override public Integer CustomColour(String name)
  {
    return null;
  }

  @Override
  public String transformStyle(String propertyName)
  {
    return propertyName;
  }

}
