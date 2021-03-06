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

import com.caucho.config.ConfigContext;
import java.io.Closeable;
import java.lang.annotation.*;
import javax.webbeans.*;

import com.caucho.webbeans.cfg.WbWebBeans;
import com.caucho.webbeans.context.*;
import com.caucho.webbeans.manager.*;

/**
 * Component for a singleton beans
 */
public class SingletonComponent extends ClassComponent
  implements Closeable
{
  private static final Object []NULL_ARGS = new Object[0];

  private Object _value;

  public SingletonComponent(WbWebBeans webBeans, Object value)
  {
    super(webBeans);
    
    _value = value;

    setInstanceClass(value.getClass());
    setTargetType(value.getClass());

    super.setScope(new SingletonScope());
  }

  public SingletonComponent(WebBeansContainer webBeans, Object value)
  {
    super(webBeans.getWbWebBeans());
    
    _value = value;

    setInstanceClass(value.getClass());
    setTargetType(value.getClass());
    
    super.setScope(new SingletonScope());
  }

  @Override
  public void bind()
  {
  }
  
  @Override
  public void introspectConstructor()
  {
  }

  /**
   * Complete initialization
   */
  @Override
  public void init()
  {
    super.init();

    if (_value instanceof HandleAware)
      ((HandleAware) _value).setSerializationHandle(getHandle());
  }

  @Override
  public void setScope(ScopeContext scope)
  {
  }

  @Override
  public Object get()
  {
    return _value;
  }

  @Override
  public Object get(ConfigContext env)
  {
    return _value;
  }

  @Override
  public Object create()
  {
    return _value;
  }

  @Override
  protected Object createNew(ConfigContext env)
  {
    throw new IllegalStateException();
  }

  /**
   * Frees the singleton on environment shutdown
   */
  public void close()
  {
    if (_value != null)
      destroy(_value);
  }
}
