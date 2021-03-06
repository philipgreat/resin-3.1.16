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

import com.caucho.ejb.cfg21.EjbEntityBean;
import com.caucho.ejb.gen21.EntityCreateCall;
import com.caucho.ejb.cfg.*;
import com.caucho.java.JavaWriter;
import com.caucho.java.gen.BaseMethod;
import com.caucho.java.gen.CallChain;
import com.caucho.util.L10N;

import javax.ejb.EJBLocalObject;
import javax.ejb.EJBObject;
import java.io.IOException;

/**
 * Generates the skeleton for the create method.
 */
public class EntityCreateMethod extends BaseMethod {
  private static L10N L = new L10N(EntityCreateMethod.class);

  private ApiMethod _apiMethod;
  private String _contextClassName;

  protected EntityCreateMethod(ApiMethod apiMethod, CallChain call)
  {
    super(apiMethod.getMethod(), call);
  }

  public EntityCreateMethod(EjbEntityBean bean,
			    ApiMethod apiMethod,
			    ApiMethod beanCreateMethod,
			    ApiMethod beanPostCreateMethod,
			    String contextClassName)
  {
    this(apiMethod, new EntityCreateCall(bean,
					 beanCreateMethod,
					 beanPostCreateMethod,
					 contextClassName));

    _apiMethod = apiMethod;

    _contextClassName = contextClassName;
  }

  /**
   * Prints the create method
   *
   * @param method the create method
   */
  public void generateCall(JavaWriter out, String []args)
    throws IOException
  {
    /*
    out.println("Thread thread = Thread.currentThread();");
    out.println("ClassLoader oldLoader = thread.getContextClassLoader();");
    out.println();
    out.println("try {");
    out.pushDepth();
    out.println("thread.setContextClassLoader(_server.getClassLoader());");
    out.println();
    */

    out.println(_contextClassName + " cxt;");
    out.println("cxt = new " + _contextClassName + "(_server);");

    getCall().generateCall(out, null, "cxt", args);

    out.println();
    
    Class retType = _apiMethod.getReturnType();
    if (EJBObject.class.isAssignableFrom(retType))
      out.println("return (" + retType.getName() + ") cxt.getEJBObject();");
    else if (EJBLocalObject.class.isAssignableFrom(retType))
      out.println("return (" + retType.getName() + ") cxt.getEJBLocalObject();");
    else
      throw new RuntimeException(L.l("trying to create unknown type {0}",
                                     retType.getName()));

    /*
    out.popDepth();
    out.println("} finally {");
    out.println("  thread.setContextClassLoader(oldLoader);");
    out.println("}");
    */
  }
}
