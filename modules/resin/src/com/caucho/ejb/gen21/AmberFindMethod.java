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

package com.caucho.ejb.gen21;

import com.caucho.ejb.cfg.*;
import com.caucho.java.JavaWriter;
import com.caucho.java.gen.BaseMethod;
import com.caucho.util.L10N;

import java.io.IOException;

/**
 * Generates the skeleton for the create method.
 */
public class AmberFindMethod extends BaseMethod {
  private static final L10N L = new L10N(AmberFindMethod.class);

  private ApiMethod _method;
  private String _contextClassName;
  private String _prefix;
  
  public AmberFindMethod(ApiMethod method,
			 String contextClassName,
			 String prefix)
  {
    super(method.getMethod());

    _method = method;
    _contextClassName = contextClassName;
    _prefix = prefix;
  }

  /**
   * Gets the parameter types
   */
  public Class []getParameterTypes()
  {
    return _method.getParameterTypes();
  }

  /**
   * Gets the return type.
   */
  public Class getReturnType()
  {
    return _method.getReturnType();
  }

  /**
   * Prints the create method
   *
   * @param method the create method
   */
  public void generateCall(JavaWriter out, String []args)
    throws IOException
  {
    Class retType = getReturnType();

    if ("RemoteHome".equals(_prefix)) {
      out.print("return (" + retType.getName() + ") _server.getContext(");
      out.printJavaTypeToObject(args[0], getParameterTypes()[0]);
      out.println(", true).getEJBObject();");
    }
    else if ("LocalHome".equals(_prefix)) {
      out.print("return (" + retType.getName() + ") _server.getContext(");
      out.printJavaTypeToObject(args[0], getParameterTypes()[0]);
      out.println(", true).getEJBLocalObject();");
    }
    else
      throw new IllegalStateException(L.l("'{0}' is an unknown type",
					  _prefix));
  }
}
