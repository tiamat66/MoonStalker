package si.vajnartech.moonstalker;

import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.LruCache;


import java.util.HashMap;

import si.vajnartech.moonstalker.androidsvg.SVGParser;

public class SVGResources extends Resources implements SVGParser.CustomValueHandler
{
  private Resources mBase;
  LruCache<Long, Drawable> mCache = new LruCache<Long, Drawable>(16);

  private HashMap<Integer, Integer> mRegisteredColours;
  private HashMap<Integer, Integer> mColours;
  private SVGStyleTransformer styleTransformer = null;

  public SVGResources(Resources base, String packageName)
  {
    // Create a new Resources object on top of an existing set of assets in an AssetManager.
    super(base.getAssets(), base.getDisplayMetrics(), base.getConfiguration());
    mBase = base;

    mRegisteredColours = new HashMap<>();
//    registerColour(R.color.colorBase);
//    registerColour(R.color.colorDark);
//    registerColour(R.color.colorAccent);
//    registerColour(R.color.textNormal);
//    registerColour(R.color.textInvertedNormal);
//    registerColour(R.color.backgroundWindow);

    mColours = new HashMap<>();

    //Set default theme
    int themeBase   = (int) SharedPref.getDefault("theme_base", 0xff2196f3);
    int themeAccent = (int) SharedPref.getDefault("theme_accent", 0xffffeb3b);
    int themeUse    = (int) SharedPref.getDefault("theme_use", 0);

    setThemeBase(themeBase, themeAccent, themeUse == 0);

    //Load any package-default theme
    int customStyleID = getIdentifier("AppCustomTheme", "array", packageName);
    if (customStyleID != 0) {
      try {
        String[] a = getStringArray(customStyleID);
        for (int i = 0; i < a.length; i++)
          setThemeColour(mRegisteredColours.get(i), Color.parseColor(a[i]));
      } catch (Exception e) {
        Log.i("IZAA", "Error parsing custom default theme. Are there more colours than you registered?" + e);
      }
    }
    SVGParser.mCustomHandler = this;
  }

  public void registerColour(int colour)
  {
    mRegisteredColours.put(mRegisteredColours.size(), colour);
  }

  public void setThemeColour(int which, int colour)
  {
//    if (which == R.color.textNormal) { //Special handling for text colours
//      mColours.put(R.color.textNormal, 0xdd000000 + (colour & 0xffffff));
//      mColours.put(R.color.textSecondary, 0x88000000 + (colour & 0xffffff));
//      mColours.put(R.color.textDisabled, 0x44000000 + (colour & 0xffffff));
//      mColours.put(R.color.textDivider, 0x22000000 + (colour & 0xffffff));
//    } else if (which == R.color.textInvertedNormal) {
//      mColours.put(R.color.textInvertedNormal, 0xdd000000 + (colour & 0xffffff));
//      mColours.put(R.color.textInvertedSecondary, 0x88000000 + (colour & 0xffffff));
//      mColours.put(R.color.textInvertedDisabled, 0x44000000 + (colour & 0xffffff));
//      mColours.put(R.color.textInvertedDivider, 0x22000000 + (colour & 0xffffff));
//    } else if (which == R.color.textTitleNormal) {
//      mColours.put(R.color.textTitleNormal, 0xdd000000 + (colour & 0xffffff));
//      mColours.put(R.color.textTitleSecondary, 0x88000000 + (colour & 0xffffff));
//      mColours.put(R.color.textTitleDisabled, 0x44000000 + (colour & 0xffffff));
//      mColours.put(R.color.textTitleDivider, 0x22000000 + (colour & 0xffffff));
//    } else
    mColours.put(which, colour);
  }

  public void setThemeBase(int colourBase, int colourAccent, boolean isDark)
  {
//    setThemeColour(R.color.colorBase, HexPresets.getPaletteColor(colourBase, isDark ? 900 : 300));
//    setThemeColour(R.color.colorDark, HexPresets.getPaletteColor(colourBase, isDark ? 300 : 600));
//    setThemeColour(R.color.backgroundWindow, HexPresets.getPaletteColor(colourBase, isDark ? 980 : 20));
//
//    setThemeColour(R.color.colorAccent, HexPresets.getPaletteColor(colourAccent, isDark ? -200 : -700));
//
//    //log.i("juhuhu", "setting theme colours in SVGResources constructor");
//    setThemeColour(R.color.textNormal, isDark ? Color.WHITE : Color.BLACK);
//    setThemeColour(R.color.textInvertedNormal, !isDark ? Color.WHITE : Color.BLACK);
//    setThemeColour(R.color.textTitleNormal, Color.WHITE);
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
