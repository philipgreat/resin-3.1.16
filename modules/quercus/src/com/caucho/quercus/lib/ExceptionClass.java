/*
 * Copyright (c) 1998-2008 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R) Open Source
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Resin Open Source is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Resin Open Source is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, or any warranty
 * of NON-INFRINGEMENT.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Resin Open Source; if not, write to the
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Charles Reich
 */

package com.caucho.quercus.lib;

import com.caucho.quercus.Location;
import com.caucho.quercus.annotation.ClassImplementation;
import com.caucho.quercus.annotation.Optional;
import com.caucho.quercus.annotation.This;
import com.caucho.quercus.env.*;

/**
 * Exception object facade.
 */

@ClassImplementation
public class ExceptionClass
{
  private static final StringValue MESSAGE = new StringBuilderValue("message");
  private static final StringValue FILE = new StringBuilderValue("file");
  private static final StringValue LINE = new StringBuilderValue("line");
  private static final StringValue CODE = new StringBuilderValue("code");
  private static final StringValue TRACE = new StringBuilderValue("trace");
  
  /**
   * Create a new exception API object.
   */
  public static Value __construct(Env env,
                                  @This ObjectValue value,
                                  @Optional StringValue message,
                                  @Optional("0") int code)
  {
    value.putField(env, "message", message);
    value.putField(env, "code", LongValue.create(code));

    Location location = env.getLocation();
    if (location != null) {
      if (location.getFileName() != null)
        value.putField(env, "file", env.createString(location.getFileName()));
      else
        value.putField(env, "file", env.createString("unknown"));

      value.putField(env, "line", new LongValue(location.getLineNumber()));
    }

    value.putField(env, "trace", ErrorModule.debug_backtrace(env));

    return value;
  }

  /**
   * Returns a String representation of this Exception.
   */
  public static Value __toString(Env env, @This ObjectValue value)
  {
    StringValue sb = env.createUnicodeBuilder();
    
    sb.append("ExceptionClass[" + value.getName() + "]\n");
    sb.append(getMessage(env, value));
    sb.append("\n");
    sb.append(getTraceAsString(env, value));
    sb.append("\n");
    
    return sb;
  }

  /**
   * Returns the message.
   */
  public static Value getMessage(Env env, @This ObjectValue obj)
  {
    return obj.getField(env, MESSAGE);
  }

  /**
   * Returns the code.
   */
  public static Value getCode(Env env, @This ObjectValue obj)
  {
    return obj.getField(env, CODE);
  }

  /**
   * Returns the file.
   */
  public static Value getFile(Env env, @This ObjectValue obj)
  {
    return obj.getField(env, FILE);
  }

  /**
   * Returns the line.
   */
  public static Value getLine(Env env, @This ObjectValue obj)
  {
    return obj.getField(env, LINE);
  }

  /**
   * Returns the trace.
   */
  public static Value getTrace(Env env, @This ObjectValue obj)
  {
    return obj.getField(env, TRACE);
  }

  /**
   * Returns the trace.
   */
  public static Value getTraceAsString(Env env, @This ObjectValue obj)
  {
    return env.createString("<trace>");
  }
}
