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

package com.caucho.webbeans.component;

import com.caucho.config.*;
import com.caucho.config.j2ee.*;
import com.caucho.config.types.*;
import com.caucho.util.*;
import com.caucho.webbeans.*;
import com.caucho.webbeans.cfg.*;
import com.caucho.webbeans.context.*;
import com.caucho.webbeans.manager.WebBeansContainer;

import java.lang.reflect.*;
import java.lang.annotation.*;
import java.util.ArrayList;

import javax.annotation.*;
import javax.webbeans.*;

/**
 * Configuration for a @Produces method
 */
public class ProducesComponent extends ComponentImpl {
  private static final L10N L = new L10N(ProducesComponent.class);

  private static final Object []NULL_ARGS = new Object[0];
  private final ComponentImpl _producer;
  private final Method _method;

  private ComponentImpl []_args;

  private boolean _isBound;

  public ProducesComponent(WbWebBeans webbeans,
			   ComponentImpl producer,
			   Method method)
  {
    super(webbeans);

    _producer = producer;
    _method = method;

    setTargetType(method.getGenericReturnType());
  }

  /**
   * Initialization.
   */
  public void init()
  {
    super.init();
  }

  public void introspect()
  {
    ArrayList<WbBinding> bindingList = new ArrayList<WbBinding>();
    
    for (Annotation ann : _method.getAnnotations()) {
      if (ann instanceof Named) {
	setName(((Named) ann).value());
      }
      
      if (ann.annotationType().isAnnotationPresent(ComponentType.class)) {
	if (getType() == null)
	  setType(_webbeans.createComponentType(ann.annotationType()));
      }
	
      if (ann.annotationType().isAnnotationPresent(ScopeType.class)) {
	if (getScope() == null)
	  setScope(_webbeans.getScopeContext(ann.annotationType()));
      }
	
      if (ann.annotationType().isAnnotationPresent(BindingType.class)) {
	bindingList.add(new WbBinding(ann));
      }
    }

    if (getName() == null) {
      String methodName = _method.getName();
      
      if (methodName.startsWith("get") && methodName.length() > 3) {
	setName(Character.toLowerCase(methodName.charAt(3))
		+ methodName.substring(4));
      }
      else
	setName(methodName);
    }

    if (bindingList.size() > 0)
      setBindingList(bindingList);
    
    if (getType() == null)
      setType(_producer.getType());
  }

  @Override
  public Object createNew(ConfigContext env)
  {
    try {
      Object factory = _producer.get();

      if (_args == null)
	bind();

      Object []args;
      if (_args.length > 0) {
	args = new Object[_args.length];

	for (int i = 0; i < args.length; i++)
	  args[i] = _args[i].get();
      }
      else
	args = NULL_ARGS;
      
      Object value = _method.invoke(factory, args);

      if (env != null)
	env.put(this, value);

      return value;
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void bind()
  {
    synchronized (this) {
      if (_isBound)
	return;

      _isBound = true;
      
      String loc = WebBeansContainer.location(_method);
    
      Type []param = _method.getGenericParameterTypes();
      Annotation [][]paramAnn = _method.getParameterAnnotations();

      _args = new ComponentImpl[param.length];

      for (int i = 0; i < param.length; i++) {
	_args[i] = _webbeans.bindParameter(loc, param[i], paramAnn[i]);

	if (_args[i] == null)
	  throw error(_method, L.l("Type '{0}' for method parameter #{1} has no matching component.",
				   getSimpleName(param[i]), i));
      }
    }
  }

  public String toString()
  {
    StringBuilder sb = new StringBuilder();

    sb.append(getClass().getSimpleName());
    sb.append("[");
    
    if (getName() != null) {
      sb.append("name=");
      sb.append(getName());
      sb.append(", ");
    }

    sb.append(getTargetSimpleName());
    sb.append(", ");
    sb.append(_method.getDeclaringClass().getSimpleName());
    sb.append(".");
    sb.append(_method.getName());
    sb.append("(), @");
    sb.append(getType().getType().getSimpleName());
    sb.append("]");

    return sb.toString();
  }
}
