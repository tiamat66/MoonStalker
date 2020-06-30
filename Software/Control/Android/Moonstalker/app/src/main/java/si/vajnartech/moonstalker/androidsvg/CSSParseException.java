package si.vajnartech.moonstalker.androidsvg;

/*
 * Thrown by the CSS parser if a problem is found while parsing a CSS file.
 */

class CSSParseException extends Exception
{
  CSSParseException(String msg)
  {
    super(msg);
  }

  CSSParseException(String msg, Exception cause)
  {
    super(msg, cause);
  }
}
