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

package com.caucho.ejb3.gen;

import com.caucho.config.ConfigException;
import com.caucho.ejb.cfg.*;
import com.caucho.ejb.gen.*;
import com.caucho.java.JavaWriter;
import com.caucho.java.gen.GenClass;
import com.caucho.java.gen.JavaClassGenerator;
import com.caucho.util.L10N;
import com.caucho.webbeans.component.*;

import java.io.*;
import java.lang.reflect.*;
import java.lang.annotation.*;
import java.util.*;
import javax.ejb.*;
import javax.webbeans.*;

/**
 * Generates the skeleton for a session bean.
 */
public class PojoBean extends BeanGenerator {
  private static final L10N L = new L10N(PojoBean.class);

  private ApiClass _beanClass;

  private PojoView _view;

  private ArrayList<BusinessMethodGenerator> _businessMethods
    = new ArrayList<BusinessMethodGenerator>();

  private boolean _isEnhanced;
  private boolean _hasXA;
  private boolean _hasReadResolve;
  private boolean _isReadResolveEnhanced;
  private boolean _isSingleton;
  
  public PojoBean(Class beanClass)
  {
    super(beanClass.getName() + "$ResinWebBean", new ApiClass(beanClass));

    setSuperClassName(beanClass.getName());
    addInterfaceName("java.io.Serializable");
    
    addImport("javax.transaction.*");
    
    _view = new PojoView(this, getEjbClass());
    
    _beanClass = new ApiClass(beanClass);
  }

  public void setSingleton(boolean isSingleton)
  {
    _isSingleton = isSingleton;
  }

  public void introspect()
  {
    for (ApiMethod method : _beanClass.getMethods()) {
      if (Object.class.equals(method.getDeclaringClass()))
	continue;

      if (method.getName().equals("readResolve")
	  && method.getParameterTypes().length == 0) {
	_hasReadResolve = true;
      }

      int index = _businessMethods.size();
      BusinessMethodGenerator bizMethod
	= new BusinessMethodGenerator(_view, method, method.getMethod(), index);

      bizMethod.introspect(method.getMethod(), method.getMethod());

      if (! bizMethod.isEnhanced())
	continue;
      
      if (! method.isPublic() && ! method.isProtected())
	throw new ConfigException(L.l("{0}: Resin-IoC/WebBeans annotations are not allowed on private methods.", bizMethod));
      if (method.isStatic())
	throw new ConfigException(L.l("{0}: Resin-Ioc/WebBeans annotations are not allowed on static methods.", bizMethod));
      if (method.isFinal())
	throw new ConfigException(L.l("{0}: Resin-Ioc/WebBeans annotations are not allowed on final methods.", bizMethod));

      if (bizMethod.isEnhanced()) {
	_isEnhanced = true;
	_businessMethods.add(bizMethod);
      }
    }
    
    if (Serializable.class.isAssignableFrom(_beanClass.getJavaClass())
	&& ! _hasReadResolve
	&& hasTransientInject(_beanClass.getJavaClass())) {
      _isReadResolveEnhanced = true;
      _isEnhanced = true;
    }
  }

  private boolean hasTransientInject(Class cl)
  {
    if (cl == null || Object.class.equals(cl))
      return false;

    for (Field field : cl.getDeclaredFields()) {
      if (! Modifier.isTransient(field.getModifiers()))
	continue;
      if (Modifier.isStatic(field.getModifiers()))
	continue;

      Annotation []annList = field.getDeclaredAnnotations();
      if (annList == null)
	continue;

      for (Annotation ann : annList) {
	if (ann.annotationType().isAnnotationPresent(BindingType.class))
	  return true;

	if (In.class.equals(ann.annotationType()))
	  return true;
      }
    }

    return hasTransientInject(cl.getSuperclass());
  }

  public Class generateClass()
  {
    if (! isEnhanced())
      return _beanClass.getJavaClass();
    
    try {
      JavaClassGenerator gen = new JavaClassGenerator();

      Class cl = gen.preload(getFullClassName());

      if (cl != null)
	return cl;

      gen.generate(this);

      gen.compilePendingJava();

      return gen.loadClass(getFullClassName());
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  protected boolean isEnhanced()
  {
    return _isEnhanced;
  }

  @Override
  protected void generateClassContent(JavaWriter out)
    throws IOException
  {
    generateHeader(out);

    HashMap map = new HashMap();
    for (BusinessMethodGenerator method : _businessMethods) {
      method.generatePrologueTop(out, map);
    }

    for (Constructor ctor : _beanClass.getJavaClass().getDeclaredConstructors()) {
      if (Modifier.isPublic(ctor.getModifiers()))
	generateConstructor(out, ctor);
    }

    map = new HashMap();
    for (BusinessMethodGenerator method : _businessMethods) {
      method.generate(out, map);
    }

    generateWriteReplace(out);
  }

  /**
   * Generates header and prologue data.
   */
  protected void generateHeader(JavaWriter out)
    throws IOException
  {
    out.println("private static final java.util.logging.Logger __log");
    out.println("  = java.util.logging.Logger.getLogger(\"" + getFullClassName() + "\");");
    out.println("private static final boolean __isFiner");
    out.println("  = __log.isLoggable(java.util.logging.Level.FINER);");

    if (_hasXA) {
      out.println();
      out.println("private static final com.caucho.ejb3.xa.XAManager _xa");
      out.println("  = new com.caucho.ejb3.xa.XAManager();");
    }

    /*
    if (_isReadResolveEnhanced)
      generateReadResolve(out);
    */
  }

  protected void generateReadResolve(JavaWriter out)
    throws IOException
  {
    out.println();
    out.println("private Object readResolve()");
    out.println("{");
    out.println("  System.out.println(\"resolve-me\");");
    
    out.println("  return this;");
    out.println("}");
  }

  protected void generateWriteReplace(JavaWriter out)
    throws IOException
  {
    if (_isSingleton) {
      out.println("private transient Object __caucho_handle;");
      out.println();
      out.println("private Object writeReplace()");
      out.println("{");
      out.println("  return __caucho_handle;");
      out.println("}");
    }
    else {
      // XXX: need a handle or serialize to the base class (?)
    }
  }
  
  protected void generateConstructor(JavaWriter out, Constructor ctor)
    throws IOException
  {
    Class []paramTypes = ctor.getParameterTypes();
    
    out.print("public " + getClassName() + "(");

    for (int i = 0; i < paramTypes.length; i++) {
      if (i != 0)
	out.print(", ");

      out.printClass(paramTypes[i]);
      out.print(" a" + i);
    }
    
    out.println(")");

    generateThrows(out, ctor.getExceptionTypes());
    
    out.println("{");
    out.pushDepth();

    out.print("super(");

    for (int i = 0; i < paramTypes.length; i++) {
      if (i != 0)
	out.print(", ");

      out.print("a" + i);
    }
    out.println(");");

    HashMap map = new HashMap();
    for (BusinessMethodGenerator method : _businessMethods) {
      method.generateConstructorTop(out, map);
    }
    
    out.popDepth();
    out.println("}");
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
  }
}
