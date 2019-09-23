package si.vajnartech.moonstalker;

import static si.vajnartech.moonstalker.OpCodes.BATTERY;

@SuppressWarnings({"WeakerAccess", "NullableProblems"})
public class Instruction
{
  public String opCode;
  public String p1 = "";
  public String p2 = "";

  public Instruction(String opCode, String p1, String p2)
  {
    this.opCode = opCode;
    this.p1 = p1;
    this.p2 = p2;
  }

  public Instruction(String opCode, String p1)
  {
    this.opCode = opCode;
    this.p1 = p1;
  }

  public Instruction(String string)
  {
    if (string.contains("<") || string.contains(">"))
    {
      opCode = string.replace("<", "").replace(">", "");
      if(opCode.contains(" ")) {
        p1 = opCode.substring(opCode.indexOf(" ") + 1);
        opCode = BATTERY;
      }
    }
    else
      opCode = string;
  }

  @Override
  public String toString()
  {
    if (p1.isEmpty() && p2.isEmpty())
      return "<" + opCode + ">";
    if (!p1.isEmpty() && p2.isEmpty())
      return "<" + opCode + " " + p1 + ">";
    if (!p1.isEmpty())
      return "<" + opCode + " " + p1 + " " + p2 + ">";
    return "";
  }
}

