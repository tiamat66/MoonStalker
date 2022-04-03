package com.robic.zoran.arduinoemulator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.IOException;

@SuppressWarnings("unused")
public class Json
{
  private static final ObjectMapper mapper = new ObjectMapper();

  public static String toJson(Object obj)
  {
    try {
      return mapper.writeValueAsString(obj);
    } catch (JsonProcessingException e) {
      return "";
    }
  }

  public static <T> T fromJson(String data, Class<T> cls)
  {
    try {
      return mapper.readValue(data, cls);
    } catch (IOException e) {
      return null;
    }
  }

  static <T> T fromJson(BufferedReader data, Class<T> cls)
  {
    try {
      return mapper.readValue(data, cls);
    } catch (IOException e) {
      return null;
    }
  }
}
