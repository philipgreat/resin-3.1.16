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
 * @author Scott Ferguson
 */

package com.caucho.ejb.gen21;

import com.caucho.ejb.cfg21.CmrMap;
import com.caucho.ejb.cfg21.EjbEntityBean;
import com.caucho.java.JavaWriter;
import com.caucho.java.gen.BaseClass;
import com.caucho.util.L10N;

import java.io.IOException;

/**
 * Generates the skeleton for an Amber-based entity bean.
 */
public class MapClass extends BaseClass {
  private final static L10N L = new L10N(MapClass.class);

  private CmrMap _map;
  
  public MapClass(CmrMap map,
			 String className)
  {
    _map = map;

    setClassName(className);
    setSuperClassName("com.caucho.ejb.entity.CmpMapImpl");
  }

  /**
   * Generates the list's class content.
   */
  public void generateClassContent(JavaWriter out)
    throws IOException
  {
    generateConstructor(out);
    
    super.generateClassContent(out);
  }

  /**
   * Generates the list's class content.
   */
  public void generateConstructor(JavaWriter out)
    throws IOException
  {
    EjbEntityBean sourceBean = _map.getBean();
    String sourceType = sourceBean.getLocal().getName();

    out.println();
    out.println("Bean _bean;");
    out.println(sourceType + " _beanLocal;");
    
    out.println();
    out.println("public " + getClassName() + "(Bean bean, com.caucho.amber.AmberQuery query)");
    out.println("{");
    out.pushDepth();
    out.println("_bean = bean;");
    out.println("_beanLocal = bean._ejb_context._viewLocal;");
    out.println("fill(query);");
    out.popDepth();
    out.println("}");
  }
}
