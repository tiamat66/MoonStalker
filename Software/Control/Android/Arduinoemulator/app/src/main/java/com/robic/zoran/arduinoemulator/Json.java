package com.robic.zoran.arduinoemulator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.IOException;

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

