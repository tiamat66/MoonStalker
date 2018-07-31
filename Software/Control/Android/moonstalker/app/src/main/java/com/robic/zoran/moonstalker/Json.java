package com.robic.zoran.moonstalker;

import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Json
{
  private static ObjectMapper mapper = new ObjectMapper();

  public static String toJson(Object obj)
  {
    try {
      return mapper.writeValueAsString(obj);
    } catch (JsonProcessingException e) {
      return "";
    }
  }

  public static <T extends Object> T fromJson(String data, Class<T> cls)
  {
    try {
      return mapper.readValue(data, cls);
    } catch (IOException e) {
      return null;
    }
  }

  public static <T extends Object> T fromJson(BufferedReader data, Class<T> cls)
  {
    try {
      return mapper.readValue(data, cls);
    } catch (IOException e) {
      return null;
    }
  }
}

