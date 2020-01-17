package si.vajnartech.moonstalker;

import java.util.ArrayList;

import static si.vajnartech.moonstalker.OpCodes.BATTERY;

public class Instruction
{
  String opCode;
  ArrayList<String> parameters = new ArrayList<>();

  Instruction(String opCode, String p1, String p2, String p3)
  {
    this.opCode = opCode;
    parameters.add(p1);
    parameters.add(p2);
    parameters.add(p3);
  }

  Instruction(String opCode, String p1, String p2)
  {
    this.opCode = opCode;
    parameters.add(p1);
    parameters.add(p2);
  }

  Instruction(String opCode, String p1)
  {
    this.opCode = opCode;
    parameters.add(p1);
  }

  Instruction(String string)
  {
    if (string.contains("<") && string.contains(">"))
    {
      opCode = string.replace("<", "").replace(">", "");
      if(opCode.contains(" ")) {
        parameters.add(opCode.substring(opCode.indexOf(" ") + 1));
        opCode = BATTERY;
      }
    }
    else
      opCode = string;
  }

  @SuppressWarnings("NullableProblems")
  @Override
  public String toString()
  {
    int numParams = parameters.size();
    switch (numParams) {
    case 0:
      return "<" + opCode + ">";
    case 1:
      return "<" + opCode + " " + parameters.get(0) + ">";
    case 2:
      return "<" + opCode + " " + parameters.get(0) + " " + parameters.get(1) + ">";
    case 3:
      return "<" + opCode + " " + parameters.get(0) + " " + parameters.get(1) + " " + parameters.get(2) + ">";
    }
    return "";
  }
}

