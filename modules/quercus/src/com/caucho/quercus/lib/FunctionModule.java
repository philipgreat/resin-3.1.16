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
 * @author Scott Ferguson
 */

package com.caucho.quercus.lib;

import com.caucho.quercus.Quercus;
import com.caucho.quercus.QuercusException;
import com.caucho.quercus.annotation.VariableArguments;
import com.caucho.quercus.env.*;
import com.caucho.quercus.module.AbstractQuercusModule;
import com.caucho.quercus.program.AbstractFunction;
import com.caucho.util.L10N;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * PHP function routines.
 */
public class FunctionModule extends AbstractQuercusModule {
  private static final L10N L = new L10N(FunctionModule.class);

  private static final Logger log
    = Logger.getLogger(FunctionModule.class.getName());

  /**
   * Calls a user function
   */
  public static Value call_user_method(Env env,
                                       String name,
                                       Value obj,
                                       Value []args)
  {
    if (obj.isObject()) {
      AbstractFunction fun = obj.findFunction(name);
      
      return fun.callMethod(env, obj, args).copyReturn();
    }
    else {
      QuercusClass cls = env.findClass(obj.toString());
      
      AbstractFunction fun = cls.findFunction(name);
      
      return fun.call(env, args).copyReturn();
    }
  }
  
  /**
   * Calls a user function
   */
  public static Value call_user_func(Env env,
                                     Callback function,
                                     Value []args)
  {
    return function.call(env, args).copyReturn();
  }

  /**
   * Calls a user function
   */
  public static Value call_user_func_array(Env env,
                                           Callback function,
                                           Value arg)
  {
    ArrayValue argArray;

    if (arg instanceof ArrayValue)
      argArray = (ArrayValue) arg;
    else
      argArray = new ArrayValueImpl().append(arg);

    Value []args;

    if (argArray != null) {
      args = new Value[argArray.getSize()];

      int i = 0;
      for (ArrayValue.Entry head = argArray.getHead();
	   head != null;
	   head = head.getNext()) {
	args[i++] = head.getRawValue(); // php/1c09
      }
    }
    else
      args = new Value[0];

    return function.call(env, args).copyReturn();
  }

  /**
   * Creates an anonymous function
   */
  public static Value create_function(Env env,
                                      String args,
                                      String code)
  {
    try {
      AbstractFunction fun = env.createAnonymousFunction(args, code);
      
      return new CallbackFunction(fun, fun.getName());
    } catch (IOException e) {
      env.warning(e.getMessage());

      return BooleanValue.FALSE;
    }
  }

  /**
   * Returns an array of the defined functions
   */
  public static Value get_defined_functions(Env env)
  {
    return env.getDefinedFunctions();
  }

  /**
   * Returns the nth function argument.
   */
  @VariableArguments
  public static Value func_get_arg(Env env, int index)
  {
    Value []args = env.getFunctionArgs();

    if (0 <= index && index < args.length)
      return args[index];
    else {
      // XXX: warning
      return NullValue.NULL;
    }
  }

  /**
   * Returns the function arguments as an array.
   */
  @VariableArguments
  public static Value func_get_args(Env env)
  {
    Value []args = env.getFunctionArgs();

    ArrayValue result = new ArrayValueImpl();
    for (int i = 0; i < args.length; i++)
      result.put(args[i]);

    return result;
  }

  /**
   * Returns the number of arguments to the function.
   */
  @VariableArguments
  public static Value func_num_args(Env env)
  {
    Value []args = env.getFunctionArgs();

    if (args != null && args.length > 0)
      return new LongValue(args.length);
    else
      return LongValue.ZERO;
  }

  /**
   * Returns true if the function exists.
   *
   * @param env the PHP environment
   * @param name the function name
   */
  public static boolean function_exists(Env env, String name)
  {
    return name != null && env.findFunction(name) != null;
  }

  /**
   * Registers a shutdown function.
   */
  public static Value register_shutdown_function(Env env,
						 Callback fun,
						 Value []args)
  {
    env.addShutdown(fun, args);

    return NullValue.NULL;
  }
}
