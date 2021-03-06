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

package com.caucho.webbeans.context;

import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.util.*;
import javax.servlet.http.*;

import com.caucho.loader.*;
import com.caucho.server.connection.ScopeRemoveListener;
import com.caucho.webbeans.component.*;

/**
 * Contains the objects which need destruction for a given scope.
 */
public class DestructionListener
  implements ScopeRemoveListener,
	     HttpSessionBindingListener,
	     ClassLoaderListener,
	     Serializable {
  private transient ArrayList<ComponentImpl> _componentList
    = new ArrayList<ComponentImpl>();
  
  private transient ArrayList<WeakReference<Object>> _valueList
    = new ArrayList<WeakReference<Object>>();

  public void addValue(ComponentImpl comp, Object value)
  {
    _componentList.add(comp);
    _valueList.add(new WeakReference<Object>(value));
  }

  public void removeEvent(Object scope, String name)
  {
    close();
  }
  
  public void valueBound(HttpSessionBindingEvent event)
  {
  }
  
  public void valueUnbound(HttpSessionBindingEvent event)
  {
    close();
  }
  
  public void classLoaderInit(DynamicClassLoader loader)
  {
  }
  
  public void classLoaderDestroy(DynamicClassLoader loader)
  {
    close();
  }

  private void close()
  {
    ArrayList<ComponentImpl> componentList = _componentList;
    _componentList = null;
    
    ArrayList<WeakReference<Object>> valueList = _valueList;
    _valueList = null;

    if (valueList == null || componentList == null)
      return;

    for (int i = componentList.size() - 1; i >= 0; i--) {
      ComponentImpl comp = componentList.get(i);
      WeakReference<Object> ref = valueList.get(i);
      Object value = null;

      if (ref != null) {
	value = ref.get();

	if (value != null)
	  comp.destroy(value, null);
      }
    }
  }
}
