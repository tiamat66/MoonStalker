package com.vajnartech.mathlibrary;

import java.util.ArrayList;

abstract class MathBaseObject<E>
{
   MathBaseObject(int dimension)
   {
      for(int i = 0; i < dimension; i++)
         value.add(getZerro());
      n = dimension;
   }

   private int n;
   private ArrayList<E> value = new ArrayList<>();

   protected abstract  E getZerro();
   protected abstract E getOne();
}
