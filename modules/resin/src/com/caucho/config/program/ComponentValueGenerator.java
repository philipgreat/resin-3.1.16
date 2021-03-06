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

package com.caucho.config.program;

import com.caucho.naming.*;
import com.caucho.util.L10N;
import com.caucho.webbeans.component.ComponentImpl;

import javax.naming.*;
import javax.persistence.*;
import javax.rmi.*;
import java.util.*;
import java.util.logging.Logger;

/**
 * Generator for a component value.
 */
public class ComponentValueGenerator extends ValueGenerator {
  private static final Logger log
    = Logger.getLogger(ComponentValueGenerator.class.getName());
  private static final L10N L = new L10N(ComponentValueGenerator.class);

  private final ComponentImpl _comp;
  
  private final String _location;

  public ComponentValueGenerator(String location, ComponentImpl comp)
  {
    if (comp == null)
      throw new NullPointerException();
    
    _location = location;

    _comp = comp;
  }

  /**
   * Creates the value.
   */
  public Object create()
  {
    return _comp.get();
  }
}
