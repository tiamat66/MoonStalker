package com.robic.zoran.moonstalker.rest;

@SuppressWarnings("WeakerAccess")
public class Instruction
{
  public int    opCode;
  public String p1;
  public String p2;

  public Instruction(int opCode, String p1, String p2)
  {
    this.opCode = opCode;
    this.p1 = p1;
    this.p2 = p2;
  }

  public Instruction(int opCode)
  {
    this.opCode = opCode;
  }
}
