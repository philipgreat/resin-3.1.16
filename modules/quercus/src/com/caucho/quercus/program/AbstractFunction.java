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

import com.caucho.quercus.Location;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.ObjectValue;
import com.caucho.quercus.env.QuercusClass;
import com.caucho.quercus.env.Value;
import com.caucho.quercus.expr.Expr;
import com.caucho.util.L10N;

/**
 * Represents a function
 */
abstract public class AbstractFunction {
  private static final L10N L = new L10N(AbstractFunction.class);

  private static final Arg []NULL_ARGS = new Arg[0];
  private static final Value []NULL_ARG_VALUES = new Value[0];

  private final Location _location;

  private boolean _isGlobal = true;
  private boolean _isFinal = false;
  
  protected Visibility _visibility = Visibility.PUBLIC;
  protected String _declaringClassName;
  
  public AbstractFunction()
  {
    // XXX:
    _location = Location.UNKNOWN;
  }

  public AbstractFunction(Location location)
  {
    _location = location;
  }

  public String getName()
  {
    return "unknown";
  }
  
  /*
   * Returns the name of the implementing class.
   */
  public String getDeclaringClassName()
  {
    return _declaringClassName;
  }
  
  public void setDeclaringClassName(String name)
  {
    _declaringClassName = name;
  }
  
  /*
   * Returns the implementing class.
   */
  public ClassDef getDeclaringClass()
  {
    return null;
  }

  /**
   * Returns true for a global function.
   */
  public final boolean isGlobal()
  {
    return _isGlobal;
  }

  /**
   * Returns true for an abstract function.
   */
  public boolean isAbstract()
  {
    return false;
  }
  
  /*
   * Returns true for a static function.
   */
  public boolean isStatic()
  {
    return false;
  }
  
  /*
   * Returns true for a final function.
   */
  public final boolean isFinal()
  {
    return _isFinal;
  }
  
  public final void setFinal(boolean isFinal)
  {
    _isFinal = isFinal;
  }

  /*
   * Returns true for a protected function.
   */
  public final boolean isPublic()
  {
    return _visibility == Visibility.PUBLIC;
  }
  
  /*
   * Returns true for a protected function.
   */
  public final boolean isProtected()
  {
    return _visibility == Visibility.PROTECTED;
  }
  
  /*
   * Returns true for a private function.
   */
  public final boolean isPrivate()
  {
    return _visibility == Visibility.PRIVATE;
  }
  
  public final void setVisibility(Visibility v)
  {
    _visibility = v;
  }
  
  public final Location getLocation()
  {
    return _location;
  }

  /**
   * Returns true for a global function.
   */
  public final void setGlobal(boolean isGlobal)
  {
    _isGlobal = isGlobal;
  }

  /**
   * Returns true for a boolean function.
   */
  public boolean isBoolean()
  {
    return false;
  }

  /**
   * Returns true for a string function.
   */
  public boolean isString()
  {
    return false;
  }

  /**
   * Returns true for a long function.
   */
  public boolean isLong()
  {
    return false;
  }

  /**
   * Returns true for a double function.
   */
  public boolean isDouble()
  {
    return false;
  }

  /**
   * Returns true if the function uses variable args.
   */
  public boolean isCallUsesVariableArgs()
  {
    return false;
  }

  /**
   * Returns true if the function uses/modifies the local symbol table
   */
  public boolean isCallUsesSymbolTable()
  {
    return false;
  }

  /**
   * True for a returns reference.
   */
  public boolean isReturnsReference()
  {
    return true;
  }

  /**
   * Returns the args.
   */
  public Arg []getArgs()
  {
    return NULL_ARGS;
  }

  /**
   * Binds the user's arguments to the actual arguments.
   *
   * @param args the user's arguments
   * @return the user arguments augmented by any defaults
   */
  public Value []evalArguments(Env env, Expr fun, Expr []args)
  {
    Value[]values = new Value[args.length];

    for (int i = 0; i < args.length; i++)
      values[i] = args[i].evalArg(env);

    return values;
  }

  /**
   * Evaluates the function.
   */
  abstract public Value call(Env env, Value []args);

  /**
   * Evaluates the function, returning a reference.
   */
  public Value callRef(Env env, Value []args)
  {
    return call(env, args);
  }

  /**
   * Evaluates the function, returning a copy
   */
  public Value callCopy(Env env, Value []args)
  {
    return call(env, args).copyReturn();
  }
  
  /**
   * Evaluates the function as a method call.
   */
  public Value callMethod(Env env, Value obj, Value []args)
  {
    Value oldThis = env.getThis();

    try {
      if (obj != null) {
        env.setThis(obj);

        /*
        if (isPublic()) {
        }
        else if (isProtected()) {
          if (oldThis != null
              && oldThis.isA(getDeclaringClassName())) {
          }
          else {
            errorProtectedAccess(env, oldThis);
          }
        }
        else {
          //private
          
          if (oldThis != null
              && getDeclaringClassName().equals(oldThis.getClassName())) {
          }
          else {
            errorPrivateAccess(env, oldThis);
          }
        }
	*/
      }

      return call(env, args);
    } finally {
      env.setThis(oldThis);
    }
  }
  
  protected Value errorProtectedAccess(Env env, Value oldThis)
  {
    return env.error(L.l("Cannot call protected method {0}::{1}() from '{2}' context",
                         getDeclaringClassName(),
                         getName(),
                         oldThis != null ? oldThis.getClassName() : null));
  }
  
  protected Value errorPrivateAccess(Env env, Value oldThis)
  {
    return env.error(L.l("Cannot call private method {0}::{1}() from '{2}' context",
                         getDeclaringClassName(),
                         getName(),
                         oldThis != null ? oldThis.getClassName() : null));
  }
  
  /**
   * Evaluates the function as a method call, returning a reference.
   */
  public Value callMethodRef(Env env, Value obj, Value []args)
  {
    Value oldThis = env.getThis();

    try {
      env.setThis(obj);

      return callRef(env, args);
    } finally {
      env.setThis(oldThis);
    }
  }

  /**
   * Evaluates the function.
   */
  public Value call(Env env)
  {
    return call(env, NULL_ARG_VALUES);
  }

  /**
   * Evaluates the function with an argument .
   */
  public Value call(Env env, Value a1)
  {
    return call(env, new Value[] { a1 });
  }

  /**
   * Evaluates the function with arguments
   */
  public Value call(Env env, Value a1, Value a2)
  {
    return call(env, new Value[] { a1, a2 });
  }

  /**
   * Evaluates the function with arguments
   */
  public Value call(Env env, Value a1, Value a2, Value a3)
  {
    return call(env, new Value[] { a1, a2, a3 });
  }

  /**
   * Evaluates the function with arguments
   */
  public Value call(Env env, Value a1, Value a2, Value a3, Value a4)
  {
    return call(env, new Value[] { a1, a2, a3, a4 });
  }

  /**
   * Evaluates the function with arguments
   */
  public Value call(Env env, Value a1, Value a2, Value a3, Value a4, Value a5)
  {
    return call(env, new Value[] { a1, a2, a3, a4, a5 });
  }

  /**
   * Evaluates the function.
   */
  public Value call(Env env, Expr []exprs)
  {
    Value []argValues = new Value[exprs.length];
    Arg []args = getArgs();

    for (int i = 0; i < exprs.length; i++) {
      // quercus/0d19
      if (i < args.length && args[i].isReference())
	argValues[i] = exprs[i].evalArg(env);
      else
	argValues[i] = exprs[i].eval(env);
    }

    return call(env, argValues);
  }

  /**
   * Evaluates the function.
   */
  public Value callCopy(Env env, Expr []exprs)
  {
    return call(env, exprs).copy();
  }

  /**
   * Evaluates the function.
   */
  public Value callRef(Env env)
  {
    return callRef(env, NULL_ARG_VALUES);
  }

  /**
   * Evaluates the function with an argument .
   */
  public Value callRef(Env env, Value a1)
  {
    return callRef(env, new Value[] { a1 });
  }

  /**
   * Evaluates the function with arguments
   */
  public Value callRef(Env env, Value a1, Value a2)
  {
    return callRef(env, new Value[] { a1, a2 });
  }

  /**
   * Evaluates the function with arguments
   */
  public Value callRef(Env env, Value a1, Value a2, Value a3)
  {
    return callRef(env, new Value[] { a1, a2, a3 });
  }

  /**
   * Evaluates the function with arguments
   */
  public Value callRef(Env env, Value a1, Value a2, Value a3, Value a4)
  {
    return callRef(env, new Value[] { a1, a2, a3, a4 });
  }

  /**
   * Evaluates the function with arguments
   */
  public Value callRef(Env env,
		       Value a1, Value a2, Value a3, Value a4, Value a5)
  {
    return callRef(env, new Value[] { a1, a2, a3, a4, a5 });
  }

  /**
   * Evaluates the function.
   */
  public Value callRef(Env env, Expr []exprs)
  {
    Value []argValues = new Value[exprs.length];
    Arg []args = getArgs();

    for (int i = 0; i < exprs.length; i++) {
      // quercus/0d19
      if (i < args.length && args[i].isReference())
	argValues[i] = exprs[i].evalArg(env);
      else
	argValues[i] = exprs[i].eval(env);
    }

    return callRef(env, argValues);
  }

  /**
   * Evaluates the function as a method call.
   */
  public Value callMethod(Env env, Value obj)
  {
    return callMethod(env, obj, NULL_ARG_VALUES);
  }

  /**
   * Evaluates the function as a method call.
   */
  public Value callMethod(Env env, Value obj, Value a1)
  {
    return callMethod(env, obj, new Value[] { a1 });
  }

  /**
   * Evaluates the function as a method call.
   */
  public Value callMethod(Env env, Value obj, Value a1, Value a2)
  {
    return callMethod(env, obj, new Value[] { a1, a2 });
  }

  /**
   * Evaluates the function as a method call.
   */
  public Value callMethod(Env env, Value obj,
			  Value a1, Value a2, Value a3)
  {
    return callMethod(env, obj, new Value[] { a1, a2, a3 });
  }

  /**
   * Evaluates the function as a method call.
   */
  public Value callMethod(Env env, Value obj,
			  Value a1, Value a2, Value a3, Value a4)
  {
    return callMethod(env, obj, new Value[] { a1, a2, a3, a4 });
  }

  /**
   * Evaluates the function as a method call.
   */
  public Value callMethod(Env env, Value obj,
			  Value a1, Value a2, Value a3, Value a4, Value a5)
  {
    return callMethod(env, obj, new Value[] { a1, a2, a3, a4, a5 });
  }

  /**
   * Evaluates the function.
   */
  public Value callMethod(Env env, Value obj, Expr []exprs)
  {
    Value []argValues = new Value[exprs.length];
    Arg []args = getArgs();

    for (int i = 0; i < exprs.length; i++) {
      if (i < args.length && args[i].isReference()) {
	argValues[i] = exprs[i].evalArg(env);
      }
      else
	argValues[i] = exprs[i].eval(env);
    }

    return callMethod(env, obj, argValues);
  }

  /**
   * Evaluates the function as a method call.
   */
  public Value callMethodRef(Env env, Value obj)
  {
    return callMethodRef(env, obj, NULL_ARG_VALUES);
  }

  /**
   * Evaluates the function as a method call.
   */
  public Value callMethodRef(Env env, Value obj, Value a1)
  {
    return callMethodRef(env, obj, new Value[] { a1 });
  }

  /**
   * Evaluates the function as a method call.
   */
  public Value callMethodRef(Env env, Value obj, Value a1, Value a2)
  {
    return callMethodRef(env, obj, new Value[] { a1, a2 });
  }

  /**
   * Evaluates the function as a method call.
   */
  public Value callMethodRef(Env env, Value obj,
			     Value a1, Value a2, Value a3)
  {
    return callMethodRef(env, obj, new Value[] { a1, a2, a3 });
  }

  /**
   * Evaluates the function as a method call.
   */
  public Value callMethodRef(Env env, Value obj,
			     Value a1, Value a2, Value a3, Value a4)
  {
    return callMethodRef(env, obj, new Value[] { a1, a2, a3, a4 });
  }

  /**
   * Evaluates the function as a method call.
   */
  public Value callMethodRef(Env env, Value obj,
			     Value a1, Value a2, Value a3, Value a4, Value a5)
  {
    return callMethodRef(env, obj, new Value[] { a1, a2, a3, a4, a5 });
  }

  /**
   * Evaluates the function.
   */
  public Value callMethodRef(Env env, Value obj, Expr []exprs)
  {
    Value []argValues = new Value[exprs.length];
    Arg []args = getArgs();

    for (int i = 0; i < exprs.length; i++) {
      if (i < args.length && args[i].isReference())
	argValues[i] = exprs[i].evalArg(env);
      else
	argValues[i] = exprs[i].eval(env);
    }

    return callMethodRef(env, obj, argValues);
  }
}

