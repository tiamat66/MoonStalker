package si.vajnartech.moonstalker;

import android.content.res.Resources;

import si.vajnartech.moonstalker.androidsvg.SVGParser;

public class SVGResources extends Resources implements SVGParser.CustomValueHandler
{
  private SVGStyleTransformer       styleTransformer = null;

  SVGResources(Resources base)
  {
    super(base.getAssets(), base.getDisplayMetrics(), base.getConfiguration());
    SVGParser.mCustomHandler = this;
  }

  void setStyleTransformer(SVGStyleTransformer transformer)
  {
    styleTransformer = transformer;
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
    if (styleTransformer != null)
      return styleTransformer.transformStyle(propertyName);
    return propertyName;
  }

  public interface SVGStyleTransformer
  {
    String transformStyle(String propertyName);
  }
}
