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
import com.caucho.quercus.env.NullValue;
import com.caucho.quercus.env.QuercusClass;
import com.caucho.quercus.env.Value;
import com.caucho.util.L10N;

import java.util.ArrayList;

/**
 * Represents a PHP function expression of the form "new ClassName()".
 */
public class NewExpr extends Expr {
  private static final L10N L = new L10N(NewExpr.class);
  protected final String _name;
  protected final Expr []_args;

  public NewExpr(Location location, String name, ArrayList<Expr> args)
  {
    super(location);
    _name = name.intern();

    _args = new Expr[args.size()];
    args.toArray(_args);
  }

  public NewExpr(Location location, String name, Expr []args)
  {
    super(location);
    _name = name.intern();
    _args = args;
  }

  public NewExpr(String name, ArrayList<Expr> args)
  {
    this(Location.UNKNOWN, name, args);
  }

  public NewExpr(String name, Expr []args)
  {
    this(Location.UNKNOWN, name, args);
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
    env.pushCall(this, NullValue.NULL);
    
    try {
      QuercusClass cl = env.findAbstractClass(_name);

      Value []args = new Value[_args.length];

      for (int i = 0; i < args.length; i++) {
        args[i] = _args[i].eval(env);
      }

      env.checkTimeout();

      return cl.callNew(env, args);
    } finally {
      env.popCall();
    }
  }
  
  public String toString()
  {
    return _name + "()";
  }
}

