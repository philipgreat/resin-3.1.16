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

package com.caucho.ejb.gen;

import com.caucho.ejb.cfg.*;
import com.caucho.java.JavaWriter;
import com.caucho.util.L10N;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import javax.annotation.security.*;
import javax.ejb.*;
import javax.interceptor.*;

/**
 * Represents a stateless create business method
 */
public class StatelessCreateMethod extends BusinessMethodGenerator
{
  private StatelessGenerator _bean;
  private View _objectView;
  
  public StatelessCreateMethod(StatelessGenerator bean,
			       StatelessView homeView,
			       View objectView,
			       ApiMethod apiMethod,
			       ApiMethod implMethod,
			       int index)
  {
    super(homeView, apiMethod,
	  implMethod != null ? implMethod.getMethod() : null,
	  index);

    _bean = bean;
    _objectView = objectView;

    if (_objectView == null)
      throw new NullPointerException();
  }

  @Override
  public boolean isEnhanced()
  {
    return true;
  }

  protected void generateContent(JavaWriter out)
    throws IOException
  {
    out.println("return new " + _objectView.getViewClassName() + "(_context);");
  }

  protected void generatePreCall(JavaWriter out)
    throws IOException
  {
    out.println("bean = new " + _objectView.getViewClassName() + "(_context);");
  }

  /**
   * Generates the underlying bean instance
   */
  protected void generateThis(JavaWriter out)
    throws IOException
  {
    out.print("bean");
  }

  /**
   * Generates the underlying bean instance
   */
  protected String getSuper()
  {
    return "bean";
  }

  @Override
  protected void generatePostCall(JavaWriter out)
    throws IOException
  {
    out.println("return bean;");
  }
}
