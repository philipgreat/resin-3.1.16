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

package com.caucho.quercus.program;

import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.NullValue;
import com.caucho.quercus.env.Value;
import com.caucho.quercus.expr.Expr;
import com.caucho.util.L10N;

import java.util.logging.Logger;

/**
 * Represents a compiled method with N args
 */
abstract public class CompiledMethodRef_N extends CompiledMethodRef {
  private static final Logger log
    = Logger.getLogger(CompiledMethodRef_N.class.getName());
  private static final L10N L = new L10N(CompiledMethodRef_N.class);

  private String _name;
  private Expr []_defaultArgs;

  public CompiledMethodRef_N(String name, Expr []defaultArgs)
  {
    _name = name;

    _defaultArgs = defaultArgs;
  }
  
  /**
   * Returns this function's name.
   */
  @Override
  public String getName()
  {
    return _name;
  }

  /**
   * Binds the user's arguments to the actual arguments.
   *
   * @param args the user's arguments
   * @return the user arguments augmented by any defaults
   */
  public Expr []bindArguments(Env env, Expr fun, Expr []args)
  {
    return args;
  }

  public final Value callMethodRef(Env env, Value obj, Value []argValues)
  {
    Value []args;

    if (_defaultArgs.length <= argValues.length) {
      args = argValues;
    }
    else {
      args = new Value[_defaultArgs.length];

      System.arraycopy(argValues, 0, args, 0, argValues.length);

      for (int i = argValues.length; i < args.length; i++) {
	if (_defaultArgs[i] != null)
	  args[i] = _defaultArgs[i].eval(env);
	else
	  args[i] = NullValue.NULL;
      }
    }

    return callMethodRefImpl(env, obj, args);
  }

  abstract public Value callMethodRefImpl(Env env, Value obj, Value []argValues);

  /**
   * Evaluates the method as a static function
   */
  public Value call(Env env, Value []argValues)
  {
    env.error(L.l("can't call {0} as a static function",
		  _name));

    return NullValue.NULL;
  }
  
  public String toString()
  {
    return "CompiledMethodRef_N[" + _name + "]";
  }
}

