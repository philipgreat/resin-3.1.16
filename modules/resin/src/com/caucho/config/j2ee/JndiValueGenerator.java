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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson;
 */

package com.caucho.config.j2ee;

import com.caucho.config.ConfigException;
import com.caucho.config.program.ValueGenerator;
import com.caucho.util.L10N;

import javax.naming.InitialContext;
import javax.naming.NamingException;

public class JndiValueGenerator extends ValueGenerator {
  private static final L10N L = new L10N(JndiValueGenerator.class);

  private String _jndiName;
  private Class _type;

  public JndiValueGenerator(String jndiName, Class type)
  {
    _jndiName = jndiName;
  }

  @Override
  public Object create()
    throws ConfigException
  {
    try {
      return new InitialContext().lookup(_jndiName);
    } catch (NamingException e) {
      throw ConfigException.create(e);
    }
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _jndiName + "," + _type + "]";
  }
}
