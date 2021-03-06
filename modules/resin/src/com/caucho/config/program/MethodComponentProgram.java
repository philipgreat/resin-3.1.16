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
 * @author Scott Ferguson;
 */

package com.caucho.config.program;

import com.caucho.config.*;
import com.caucho.config.j2ee.*;
import com.caucho.config.program.ConfigProgram;
import com.caucho.webbeans.component.*;
import com.caucho.webbeans.context.DependentScope;

import java.util.logging.*;
import java.lang.reflect.*;

public class MethodComponentProgram extends ConfigProgram
{
  private static final Logger log
    = Logger.getLogger(MethodComponentProgram.class.getName());

  private static final Object []NULL_ARGS = new Object[0];

  private Method _method;
  private ComponentImpl []_args;

  public MethodComponentProgram(Method method,
			     ComponentImpl []args)
  {
    _method = method;
    _method.setAccessible(true);
    _args = args;
    
    if (_method == null)
      throw new NullPointerException();

    for (int i = 0; i < args.length; i++)
      if (args[i] == null)
	throw new NullPointerException();
  }

  @Override
  public void inject(Object bean, ConfigContext env)
    throws ConfigException
  {
    try {
      Object []args;

      if (_args.length > 0) {
	args = new Object[_args.length];
	
	for (int i = 0; i < args.length; i++)
	  args[i] = _args[i].get(env);
      }
      else
	args = NULL_ARGS;
      
      _method.invoke(bean, args);
    } catch (InvocationTargetException e) {
      throw LineConfigException.create(loc(), e);
    } catch (Exception e) {
      throw LineConfigException.create(loc(), e);
    }
  }

  private String loc()
  {
    return (_method.getDeclaringClass().getSimpleName()
	    + "." + _method.getName() + ": ");
  }
}
