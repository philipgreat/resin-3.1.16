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
 * Represents a business method
 */
public class BusinessMethodGenerator implements EjbCallChain {
  private static final L10N L = new L10N(BusinessMethodGenerator.class);

  private final View _view;
  
  private final ApiMethod _apiMethod;
  private final Method _implMethod;

  private String _uniqueName;

  private boolean _isEnhanced = true;

  private String []_roles;
  private String _roleVar;

  private String _runAs;

  private XaCallChain _xa;
  private SecurityCallChain _security;
  private InterceptorCallChain _interceptor;
  
  public BusinessMethodGenerator(View view,
				 ApiMethod apiMethod,
				 Method implMethod,
				 int index)
  {
    _view = view;
    
    _apiMethod = apiMethod;
    _implMethod = implMethod;

    _uniqueName = "_" + _apiMethod.getName() + "_" + index;

    _interceptor = new InterceptorCallChain(this, view);
    _xa = createXa(_interceptor);
    _security = new SecurityCallChain(this, _xa);
  }

  protected XaCallChain createXa(EjbCallChain next)
  {
    return new XaCallChain(this, next);
  }

  /**
   * Returns the owning view.
   */
  public View getView()
  {
    return _view;
  }
  
  /**
   * Returns the bean's ejbclass
   */
  protected ApiClass getEjbClass()
  {
    return _view.getEjbClass();
  }

  /**
   * Returns the api method
   */
  public Method getApiMethod()
  {
    return _apiMethod.getMethod();
  }

  /**
   * Returns the implementation method
   */
  public Method getImplMethod()
  {
    return _implMethod;
  }
  
  /**
   * Returns true if the business method has any active XA annotation.
   */
  public boolean hasXA()
  {
    return _xa.isEnhanced();
  }

  /**
   * Set true for a remove method
   */
  public void setRemove(boolean isRemove)
  {
  }

  /**
   * Set true for a remove method
   */
  public void setRemoveRetainIfException(boolean isRemoveRetainIfException)
  {
  }

  /**
   * Returns the xa call chain
   */
  public XaCallChain getXa()
  {
    return _xa;
  }

  /**
   * Returns the security call chain
   */
  public SecurityCallChain getSecurity()
  {
    return _security;
  }

  /**
   * Returns the interceptor call chain
   */
  public InterceptorCallChain getInterceptor()
  {
    return _interceptor;
  }

  /**
   * Returns true if any interceptors enhance the business method
   */
  public boolean isEnhanced()
  {
    if (_security.isEnhanced())
      return true;
    else if (_xa.isEnhanced())
      return true;
    else if (_interceptor.isEnhanced())
      return true;

    return false;
  }

  public void introspect(Method apiMethod, Method implMethod)
  {
    _security.introspect(apiMethod, implMethod);
    _xa.introspect(apiMethod, implMethod);
    _interceptor.introspect(apiMethod, implMethod);
  }

  public final void generatePrologueTop(JavaWriter out, HashMap prologueMap)
    throws IOException
  {
    if (! isEnhanced())
      return;

    _security.generatePrologue(out, prologueMap);

    generateInterceptorTarget(out);
  }

  public final void generateConstructorTop(JavaWriter out, HashMap prologueMap)
    throws IOException
  {
    if (! isEnhanced())
      return;

    _security.generateConstructor(out, prologueMap);
  }

  public final void generate(JavaWriter out, HashMap prologueMap)
    throws IOException
  {
    if (! isEnhanced())
      return;

    generateHeader(out);
    
    out.println("{");
    out.pushDepth();

    generateContent(out);

    out.popDepth();
    out.println("}");
  }

  protected void generateInterceptorTarget(JavaWriter out)
    throws IOException
  {
    if (_interceptor.isEnhanced()) {
      out.println();
      out.print("private ");
      out.printClass(_implMethod.getReturnType());
      out.print(" __caucho_");
      out.print(_apiMethod.getName());
      out.print("(");

      Class []types = _implMethod.getParameterTypes();
      for (int i = 0; i < types.length; i++) {
	Class type = types[i];
	
	if (i != 0)
	  out.print(", ");

	out.printClass(type);
	out.print(" a" + i);
      }
    
      out.println(")");
      generateThrows(out, _implMethod.getExceptionTypes());
      out.println();
      out.println("{");
      out.pushDepth();

      generateCall(out, "super");

      out.popDepth();
      out.println("}");
    }
  }

  public void generateHeader(JavaWriter out)
    throws IOException
  {
    out.println();
    if (_apiMethod.isPublic())
      out.print("public ");
    else if (_apiMethod.isProtected())
      out.print("protected ");
    else
      throw new IllegalStateException(_apiMethod.toString() + " must be public or protected");

    out.printClass(_apiMethod.getReturnType());
    out.print(" ");
    out.print(_apiMethod.getName());
    out.print("(");

    Class []types = _apiMethod.getParameterTypes();
    for (int i = 0; i < types.length; i++) {
      Class type = types[i];
      
      if (i != 0)
	out.print(", ");

      if (i == types.length - 1 && type.isArray() && _apiMethod.isVarArgs()) {
	out.printClass(type.getComponentType());
	out.print("...");
      }
      else
	out.printClass(type);
      
      out.print(" a" + i);
    }
    
    out.println(")");
    if (_implMethod != null)
      generateThrows(out, _implMethod.getExceptionTypes());
    else
      generateThrows(out, _apiMethod.getExceptionTypes());
  }

  protected void generateContent(JavaWriter out)
    throws IOException
  {
    generatePreCall(out);
    
    _security.generateCall(out);

    generatePostCall(out);
  }

  /**
   * Generates any additional configuration in the constructor
   */
  public void generateConstructor(JavaWriter out, HashMap map)
    throws IOException
  {
  }

  public void generatePrologue(JavaWriter out, HashMap map)
    throws IOException
  {
  }

  protected void generateThrows(JavaWriter out, Class []exnCls)
    throws IOException
  {
    if (exnCls.length == 0)
      return;

    out.print(" throws ");
    
    for (int i = 0; i < exnCls.length; i++) {
      if (i != 0)
	out.print(", ");

      out.printClass(exnCls[i]);
    }
    out.println();
  }

  public void generateCall(JavaWriter out)
    throws IOException
  {
    generateCall(out, getSuper());
  }

  public void generateCall(JavaWriter out, String superVar)
    throws IOException
  {
    if (! void.class.equals(_apiMethod.getReturnType())) {
      out.printClass(_apiMethod.getReturnType());
      out.println(" result;");
    }
    
    if (! void.class.equals(_implMethod.getReturnType()))
      out.print("result = ");

    out.print(superVar + "." + _implMethod.getName() + "(");

    Class []types = _implMethod.getParameterTypes();
    for (int i = 0; i < types.length; i++) {
      if (i != 0)
	out.print(", ");

      out.print(" a" + i);
    }
    
    out.println(");");

    // ejb/12b0
    if (! "super".equals(superVar))
      generatePreReturn(out);
    
    if (! void.class.equals(_implMethod.getReturnType()))
      out.println("return result;");
  }

  /**
   * Generates the underlying bean instance
   */
  protected void generatePreCall(JavaWriter out)
    throws IOException
  {
  }

  /**
   * Generates the underlying bean instance
   */
  protected String getSuper()
    throws IOException
  {
    return "super";
  }

  /**
   * Generates the underlying bean instance
   */
  protected void generateThis(JavaWriter out)
    throws IOException
  {
    out.print("this");
  }

  /**
   * Generates the underlying bean instance
   */
  protected void generatePreReturn(JavaWriter out)
    throws IOException
  {
  }

  /**
   * Generates the underlying bean instance
   */
  protected void generatePostCall(JavaWriter out)
    throws IOException
  {
  }

  protected boolean hasException(Class exn)
  {
    for (Class apiExn : _implMethod.getExceptionTypes()) {
      if (apiExn.isAssignableFrom(exn))
	return true;
    }

    return false;
  }

  boolean matches(String name, Class[] parameterTypes)
  {
    if (! _apiMethod.getName().equals(name))
      return false;
    
    Class []methodTypes = _apiMethod.getParameterTypes();
    if (methodTypes.length != parameterTypes.length)
      return false;
    
    for (int i = 0; i < parameterTypes.length; i++) {
      if (! methodTypes[i].equals(parameterTypes[i]))
        return false;
    }
    
    return true;
  }

  public String toString()
  {
    return getClass().getSimpleName() + "[" + _apiMethod + "]";
  }
}
