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

package com.caucho.quercus.expr;

import com.caucho.quercus.Location;
import com.caucho.quercus.env.ArrayValueImpl;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.NullValue;
import com.caucho.quercus.env.Value;
import com.caucho.quercus.parser.QuercusParser;

/**
 * Represents a PHP variable expression.
 */
public class VarExpr
  extends AbstractVarExpr
{
  private static final NullValue NULL = NullValue.create();

  private final VarInfo _var;
  protected final String _name;

  private VarState _varState = VarState.INIT;

  protected VarExpr(Location location, VarInfo var)
  {
    super(location);
    _var = var;
    _name = var.getName();
  }

  protected VarExpr(VarInfo var)
  {
    _var = var;
    _name = var.getName();
  }

  /**
   * Returns the variable info.
   */
  public VarInfo getVarInfo()
  {
    return _var;
  }

  /**
   * Returns the variable name.
   */
  public String getName()
  {
    return _name;
  }

  /**
   * Returns the java variable name.
   */
  public String getJavaVar()
  {
    return "v_" + _name;
  }

  /**
   * Copy for things like $a .= "test";
   * @param location
   */
  public Expr copy(Location location)
  {
    return new VarExpr(location, _var);
  }

  /**
   * Creates the assignment.
   */
  public Expr createAssign(QuercusParser parser, Expr value)
  {
    _var.setAssigned();

    return super.createAssign(parser, value);
  }

  /**
   * Creates the assignment.
   */
  public void assign(QuercusParser parser)
  {
    _var.setAssigned();
  }

  /**
   * Creates the assignment.
   */
  public Expr createAssignRef(QuercusParser parser,
                              Expr value
  )
  {
    _var.setAssigned();

    return super.createAssignRef(parser, value);
  }

  /**
   * Evaluates the expression.
   *
   * @param env the calling environment.
   * @return the expression value.
   */
  public Value eval(Env env)
  {
    Value value;

    if (_var.isGlobal())
      value = env.getGlobalValue(_name);
    else
      value = env.getValue(_name);

    if (value != null)
      return value;
    else
      return NULL;
  }

  /**
   * Evaluates the expression.
   *
   * @param env the calling environment.
   * @return the expression value.
   */
  public Value evalCopy(Env env)
  {
    Value v = eval(env);
    
    return v.copy();
  }

  /**
   * Evaluates the expression, converting to an array if unset.
   *
   * @param env the calling environment.
   * @return the expression value.
   */
  public Value evalArray(Env env)
  {
    Value value;

    if (_var.isGlobal()) {
      value = env.getGlobalValue(_name);

      if (value == null) {
        value = new ArrayValueImpl();

        env.setGlobalValue(_name, value);
      }
      else {
        Value array = value.toAutoArray();

	if (array != value) {
	  env.setGlobalValue(_name, array);

	  value = array;
	}
      }
    } else {
      value = env.getVar(_name);

      if (value == null) {
        value = new ArrayValueImpl();

        env.setValue(_name, value);
      }
      else {
	value = value.toAutoArray();
      }
    }

    return value;
  }

  /**
   * Evaluates the expression, converting to an object if is unset, NULL,
   * or is a string.
   *
   * @param env the calling environment.
   * @return the expression value.
   */
  public Value evalObject(Env env)
  {
    Value value;

    if (_var.isGlobal()) {
      value = env.getGlobalValue(_name);

      if (value == null || value.isString() || value.isNull()) {
        value = env.createObject();

        env.setGlobalValue(_name, value);
      }
    } else {
      value = env.getValue(_name);

      if (value == null || value.isString() || value.isNull()) {
        value = env.createObject();

        env.setValue(_name, value);
      }
    }

    return value;
  }

  /**
   * Evaluates the expression.
   *
   * @param env the calling environment.
   * @return the expression value.
   */
  public Value evalRef(Env env)
  {
    if (getVarInfo().isGlobal())
      return env.getGlobalVar(_name);
    else
      return env.getVar(_name);
  }

  /**
   * Evaluates the expression.
   *
   * @param env the calling environment.
   * @return the expression value.
   */
  public Value evalArg(Env env)
  {
    // quercus/043k
    // quercus/0443

    if (getVarInfo().isGlobal())
      return env.getGlobalVar(_name);
    else
      return env.getVar(_name);
  }

  /**
   * Evaluates the expression.
   *
   * @param env the calling environment.
   */
  public void evalAssign(Env env, Value value)
  {
    if (getVarInfo().isGlobal())
      env.setGlobalValue(_name, value);
    else
      env.setValue(_name, value);
  }

  /**
   * Evaluates the expression.
   *
   * @param env the calling environment.
   */
  public void evalUnset(Env env)
  {
    if (getVarInfo().isGlobal())
      env.unsetGlobalVar(_name);
    else
      env.unsetLocalVar(_name);
  }

  public int hashCode()
  {
    return _name.hashCode();
  }

  public boolean equals(Object o)
  {
    if (this == o)
      return true;
    else if (getClass() != o.getClass())
      return false;

    VarExpr var = (VarExpr) o;

    return _var == var._var;
  }

  public String toString()
  {
    return "$" + _name;
  }
}

