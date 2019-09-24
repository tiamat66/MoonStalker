package si.vajnartech.moonstalker;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;

@SuppressWarnings({"ConstantConditions", "WeakerAccess", "UnusedReturnValue", "DeprecatedIsStillUsed", "deprecation"})
public class SharedPref
{
  private static final String                   PREF_USER_LEARNED_DRAWER = "navigation_drawer_learned";
  private              SharedPreferences        pref;
  private              SharedPreferences.Editor e                        = null;
  private              int                      editLevel                = 0;
  static private       DefHash                  defaults                 = new DefHash();
  private static       SharedPreferences        defaultSharedPref        = null;

  SharedPref(Context context)
  {
    this(
        defaultSharedPref != null ? defaultSharedPref : (defaultSharedPref = PreferenceManager.getDefaultSharedPreferences(
            context)));
  }

  private SharedPref(SharedPreferences aPref)
  {
    pref = aPref;
  }

  static void setDefault(String key, Object data) { defaults.put(key, data); }

  public static Object getDefault(String key, Object def) { return defaults.get(key, def); }

  private boolean getSettBool(String sName, boolean defVal)
  {
    if (sName.equals("notification_type") || sName.equals("widget_color"))
      return pref.getInt(sName, 1) > 0;
    return pref.getBoolean(sName, defVal);
  }

  @SuppressLint("CommitPrefEdits")
  private void editBegin()
  {
    if (++editLevel == 1)
      e = pref.edit();
  }

  private void editCommit()
  {
    if (--editLevel == 0)
      e.commit();
  }

  public boolean put(String sName, int value)
  {
    boolean res = getInt(sName) != value;
    editBegin();
    e.putInt(sName, value);
    editCommit();
    return res;
  }

  public boolean put(String sName, Date value)
  {
    boolean res = getDate(sName) != value;
    editBegin();
    e.putLong(sName, value.getTime());
    editCommit();
    return res;
  }

  public boolean put(String sName, String value)
  {
    if (value == null) value = "";
    boolean res = !getString(sName).equals(value);
    if (sName.contains("pass")) {
      res = !PassEncryption.encode(getString(sName)).contains(PassEncryption.encrypt(value));
      value = PassEncryption.encrypt(value);
    }
    editBegin();
    e.putString(sName, value);
    editCommit();
    return res;
  }

  public boolean put(String sName, boolean value)
  {
    boolean res = getBool(sName) != value;
    editBegin();
    e.putBoolean(sName, value);
    editCommit();
    return res;
  }

  public boolean put(String sName, long value)
  {
    boolean res = getLong(sName) != value;
    editBegin();
    e.putLong(sName, value);
    editCommit();
    return res;
  }

  private int getInt(String sName)
  {
    try {
      return pref.getInt(sName, (int) defaults.get(sName, 0));
    } catch (Exception e) {
      try {
        return Integer.parseInt(pref.getString(sName, ""));
      } catch (Exception e1) {
        return (int) defaults.get(sName, 0);
      }
    }
  }

  public Date getDate(String sName)
  {
    try {
      return new Date(pref.getLong(sName, ((Date) defaults.get(sName, new Date(0))).getTime()));
    } catch (Exception e) {
      return new Date(0);
    }
  }

  public boolean getBool(String sName)
  {
    try {
      if (sName.equals("notification_type") || sName.equals("widget_color"))
        return pref.getInt(sName, 1) > 0;
      return pref.getBoolean(sName, (boolean) defaults.get(sName, false));
    } catch (Exception e) {
      return (boolean) defaults.get(sName, false);
    }
  }

  public String getString(String sName)
  {
    String res;
    try {
      res = pref.getString(sName, defaults.get(sName, "").toString());
      if (sName.contains("pass"))
        res = PassEncryption.decode(res);
      return res;
    } catch (Exception e) {
      Log.i("sett", "getting default for " + sName);
      return defaults.get(sName, "").toString();
    }
  }

  public long getLong(String sName)
  {
    try {
      return pref.getLong(sName, (long) defaults.get(sName, 0L));
    } catch (Exception e) {
      Log.i("sett", "getting default for " + sName);
      return (long) defaults.get(sName, 0L);
    }
  }

  public SharedPref remove(String name)
  {
    editBegin();
    e.remove(name);
    editCommit();
    return this;
  }

  @Deprecated
  public String getSettS(String sName, String sDefault)
  {
    try {
      return pref.getString(sName, sDefault);
    } catch (Exception e) {
      return Integer.toString(pref.getInt(sName, 0));
    }
  }

  @Deprecated
  public String getSettS(String sName)
  {
    return getSettS(sName, "");
  }

  public String getAsString(String sName)
  {
    return pref.getAll().get(sName).toString();
  }

  public int getSettArrayIdx(String sName, final String[] rValues, final String defValue)
  {
    String v = getSettS(sName);
    if (v.length() == 0) v = defValue;

    int idx = Arrays.asList(rValues).indexOf(v);
    if (idx == -1) idx = 0;
    return idx;
  }

  public boolean contains(String sName)
  {
    return pref.contains(sName);
  }

  public String getSettArray(String sName, final String[] rNames, final String[] rValues, final String defValue)
  {
    return rNames[getSettArrayIdx(sName, rValues, defValue)];
  }

  public boolean drawerLearned()
  {
    return getSettBool(PREF_USER_LEARNED_DRAWER, false);
  }

  public void drawerLearned(boolean learned)
  {
    put(PREF_USER_LEARNED_DRAWER, learned);
  }

  static class DefHash extends HashMap<String, Object>
  {
    DefHash()
    {
      super();
    }

    public Object get(String key, Object def)
    {
      if (containsKey(key)) return get(key);
      else return def;
    }
  }

  public static class PassEncryption
  {
    private static final String SECRET_KEY        = "a7s6afrew87gt";
    private static final String ENCRYPTION_PREFIX = "žlj";

    static String encode(String string)
    {
      StringBuilder encodedString = new StringBuilder();
      char[]        keyBuf        = SECRET_KEY.toCharArray();

      char[] sBuf = string.toCharArray();
      for (int i = 0; i < string.length(); i++) {
        char keyCh = keyBuf[i % SECRET_KEY.length()];
        encodedString.append((char) (sBuf[i] + keyCh % 256));
      }
      String encodedStr = encodedString.toString();
      byte[] dec        = android.util.Base64.encode(encodedStr.getBytes(), android.util.Base64.DEFAULT);
      return new String(dec);
    }

    static String decode(String string)
    {
      StringBuilder decodedString = new StringBuilder();
      char[]        keyBuf        = SECRET_KEY.toCharArray();
      byte[]        dec;
      dec = android.util.Base64.decode(string.getBytes(StandardCharsets.UTF_8), android.util.Base64.DEFAULT);
      String decodedStr = new String(dec);
      char[] sBuf       = decodedStr.toCharArray();
      for (int i = 0; i < decodedStr.length(); i++) {
        char keyCh = keyBuf[i % SECRET_KEY.length()];
        decodedString.append((char) (sBuf[i] - keyCh % 256));
      }
      return decodedString.toString();
    }

    static String encrypt(String pass)
    {
      if (pass.isEmpty()) return "";
      // The tablet knows now that server also sending encrypted
      if (pass.startsWith(ENCRYPTION_PREFIX))
        return pass.substring(ENCRYPTION_PREFIX.length());
      // Server does not have encryption capability
      return encode(pass);
    }
  }
}


