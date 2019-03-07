package si.vajnartech.moonstalker.rest;

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

  public Instruction(String opCode)
  {
    this.opCode = opCode;
  }

  @Override
  public String toString()
  {
    if (p1.isEmpty() && p2.isEmpty())
      return "<" + opCode + ">";
    if (!p1.isEmpty() && p2.isEmpty())
      return "<" + opCode + " " + p1 + ">";
    if (!p1.isEmpty())
      return "<" + opCode + " " + p1 + "," + p2 + ">";
    return "";
  }
}
