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
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.Value;
import com.caucho.util.L10N;

/**
 * Represents a PHP field reference.
 */
public class FieldVarGetExpr extends AbstractVarExpr {
  private static final L10N L = new L10N(FieldVarGetExpr.class);

  protected final Expr _objExpr;
  protected final Expr _nameExpr;

  public FieldVarGetExpr(Location location, Expr objExpr, Expr nameExpr)
  {
    super(location);
    _objExpr = objExpr;
    
    _nameExpr = nameExpr;
  }

  public FieldVarGetExpr(Expr objExpr, Expr nameExpr)
  {
    _objExpr = objExpr;
    
    _nameExpr = nameExpr;
  }

  /**
   * Evaluates the expression.
   *
   * @param env the calling environment.
   *
   * @return the expression value.
   */
  public Value evalArg(Env env)
  {
    Value value = _objExpr.evalArg(env);

    return value.getFieldArg(env, _nameExpr.evalStringValue(env));
  }

  /**
   * Evaluates the expression.
   *
   * @param env the calling environment.
   *
   * @return the expression value.
   */
  public Value evalRef(Env env)
  {
    // quercus/0d1k
    Value value = _objExpr.evalObject(env);

    return value.getFieldRef(env, _nameExpr.evalStringValue(env));
  }
  
  /**
   * Evaluates the expression.
   *
   * @param env the calling environment.
   *
   * @return the expression value.
   */
  public Value eval(Env env)
  {
    Value obj = _objExpr.eval(env);

    return obj.getField(env, _nameExpr.evalStringValue(env));
  }
  
  /**
   * Evaluates the expression.
   *
   * @param env the calling environment.
   *
   * @return the expression value.
   */
  public void evalAssign(Env env, Value value)
  {
    Value obj = _objExpr.evalObject(env);

    obj.putField(env, _nameExpr.evalStringValue(env), value);
  }

  /**
   * Evaluates the expression, creating an array if the field is unset.
   *
   * @param env the calling environment.
   *
   * @return the expression value.
   */
  public Value evalArray(Env env)
  {
    Value obj = _objExpr.evalObject(env);

    return obj.getFieldArray(env, _nameExpr.evalStringValue(env));
  }

  /**
   * Evaluates the expression, creating an object if the field is unset.
   *
   * @param env the calling environment.
   *
   * @return the expression value.
   */
  public Value evalObject(Env env)
  {
    Value obj = _objExpr.evalObject(env);

    return obj.getFieldObject(env, _nameExpr.evalStringValue(env));
  }
  
  /**
   * Evaluates the expression.
   *
   * @param env the calling environment.
   *
   * @return the expression value.
   */
  public void evalUnset(Env env)
  {
    Value obj = _objExpr.eval(env);

    obj.unsetField(_nameExpr.evalStringValue(env));
  }
  
  public String toString()
  {
    return _objExpr + "->{" + _nameExpr + "}";
  }
}

