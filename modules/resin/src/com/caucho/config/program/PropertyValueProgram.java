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
import com.caucho.config.type.*;
import com.caucho.config.attribute.*;
import com.caucho.util.*;
import com.caucho.xml.*;

/**
 * A saved program for configuring an object.
 */
public class PropertyValueProgram extends ConfigProgram {
  private static final L10N L = new L10N(PropertyValueProgram.class);
  
  private final String _name;
  private final QName _qName;
  private final Object _value;

  public PropertyValueProgram(String name, Object value)
  {
    _name = name;
    _qName = new QName(name);
    _value = value;
  }
  
  /**
   * Returns the injection name.
   */
  public String getName()
  {
    return _name;
  }
  
  //
  // Inject API
  //
  
  /**
   * Injects the bean with the dependencies
   */
  @Override
  public void inject(Object bean, ConfigContext env)
  {
    try {
      ConfigType type = TypeFactory.getType(bean.getClass());

      Attribute attr = type.getAttribute(_qName);

      if (attr != null)
	attr.setValue(bean, _qName, attr.getConfigType().valueOf(_value));
      else
	throw new ConfigException(L.l("'{0}' is an unknown attribute of '{1}'",
				      _qName.getName(), bean.getClass().getName()));
    } catch (Exception e) {
      throw ConfigException.create(e);
    }
  }
}
