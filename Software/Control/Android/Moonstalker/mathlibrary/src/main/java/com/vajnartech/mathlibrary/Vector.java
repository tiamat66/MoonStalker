package com.vajnartech.mathlibrary;

class Vector extends MathBaseObject<Double>
{
  Vector(int dimension)
  {
    super(dimension);
  }

  @Override protected Double getZerro()
  {
    return null;
  }

  @Override protected Double getOne()
  {
    return null;
  }
}
