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

import com.caucho.config.*;
import com.caucho.ejb.cfg.*;
import com.caucho.java.JavaWriter;
import com.caucho.util.L10N;

import javax.annotation.*;
import javax.ejb.*;
import java.io.IOException;
import java.lang.reflect.*;
import java.util.*;

/**
 * Represents any stateless view.
 */
public class StatelessObjectView extends StatelessView {
  private static final L10N L = new L10N(StatelessObjectView.class);

  private String _timeoutMethod;

  private LifecycleInterceptor _postConstructInterceptor;
  private LifecycleInterceptor _preDestroyInterceptor;

  public StatelessObjectView(StatelessGenerator bean, ApiClass api)
  {
    super(bean, api);
  }

  /**
   * Introspects the APIs methods, producing a business method for
   * each.
   */
  public void introspect()
  {
    super.introspect();

    introspectLifecycle(getEjbClass().getJavaClass());
    
    _postConstructInterceptor = new LifecycleInterceptor(PostConstruct.class);
    _postConstructInterceptor.introspect(getEjbClass().getJavaClass());
    
    _preDestroyInterceptor = new LifecycleInterceptor(PreDestroy.class);
    _preDestroyInterceptor.introspect(getEjbClass().getJavaClass());
    
    introspectTimer(getEjbClass());
  }

  /**
   * Introspects the lifecycle methods
   */
  public void introspectLifecycle(Class cl)
  {
    if (cl == null || cl.equals(Object.class))
      return;

    for (Method method : cl.getDeclaredMethods()) {
      if (method.isAnnotationPresent(PostConstruct.class)) {
      }
    }

    introspectLifecycle(cl.getSuperclass());
  }

  /**
   * Introspects the lifecycle methods
   */
  public void introspectTimer(ApiClass apiClass)
  {
    Class cl = apiClass.getJavaClass();
    
    if (cl == null || cl.equals(Object.class))
      return;

    if (TimedObject.class.isAssignableFrom(cl)) {
      _timeoutMethod = "ejbTimeout";
      return;
    }

    for (ApiMethod apiMethod : apiClass.getMethods()) {
      Method method = apiMethod.getMethod();
      
      if (method.isAnnotationPresent(Timeout.class)) {
	if (method.getParameterTypes().length != 1
	    || ! javax.ejb.Timer.class.equals(method.getParameterTypes()[0])) {
	  throw new ConfigException(L.l("{0}: timeout method '{1}' does not have a (Timer) parameter",
					cl.getName(), method.getName()));
	}
	
	_timeoutMethod = method.getName();

	addBusinessMethod(apiMethod);
      }
    }
  }


  /**
   * Generates prologue for the context.
   */
  public void generateContextPrologue(JavaWriter out)
    throws IOException
  {
    String localVar = "_local_" + getApi().getSimpleName();
    
    out.println();
    out.println("private " + getViewClassName() + " " + localVar + ";");
  }

  /**
   * Generates context home's constructor
   */
  @Override
  public void generateContextHomeConstructor(JavaWriter out)
    throws IOException
  {
    String localVar = "_local_" + getApi().getSimpleName();
    
    out.println(localVar + " = new " + getViewClassName() + "(this);");
  }

  /**
   * Generates code to create the provider
   */
  @Override
  public void generateCreateProvider(JavaWriter out, String var)
    throws IOException
  {
    String localVar = "_local_" + getApi().getSimpleName();
    
    out.println();
    out.println("if (" + var + " == " + getApi().getName() + ".class)");
    out.println("  return " + localVar + ";");
  }

  /**
   * Generates code to create the provider
   */
  @Override
  public void generateDestroy(JavaWriter out)
    throws IOException
  {
    String localVar = "_local_" + getApi().getSimpleName();
    
    out.println();
    out.println(localVar + ".destroy();");
  }

  /**
   * Generates the view code.
   */
  public void generate(JavaWriter out)
    throws IOException
  {
    generateBean(out);

    generateProxy(out);
  }

  /**
   * Generates the view code.
   */
  public void generateBean(JavaWriter out)
    throws IOException
  {
    out.println();
    out.println("public static class " + getBeanClassName());
    out.println("  extends " + getEjbClass().getName());
    out.println("{");
    out.pushDepth();

    out.println("private transient " + getViewClassName() + " _context;");

    HashMap map = new HashMap();
    generateBusinessPrologue(out, map);    
    _postConstructInterceptor.generatePrologue(out, map);
    _preDestroyInterceptor.generatePrologue(out, map);

    out.println();
    out.println(getBeanClassName() + "(" + getViewClassName() + " context)");
    out.println("{");
    out.pushDepth();
    out.println("_context = context;");

    map = new HashMap();
    generateBusinessConstructor(out, map);    
    _postConstructInterceptor.generateConstructor(out, map);
    _preDestroyInterceptor.generateConstructor(out, map);

    _postConstructInterceptor.generateCall(out);

    out.popDepth();
    out.println("}");

    // generateBusinessMethods(out);
    
    out.popDepth();
    out.println("}");
  }

  /**
   * Generates the view code.
   */
  public void generateProxy(JavaWriter out)
    throws IOException
  {
    out.println();
    out.println("public static class " + getViewClassName());
    generateExtends(out);
    out.print("  implements " + getApi().getDeclarationName());
    out.println(", StatelessProvider");
    out.println("{");
    out.pushDepth();

    // out.println();
    // out.println("com.caucho.ejb.xa.EjbTransactionManager _xaManager;");

    out.println();
    out.println("private static final com.caucho.ejb3.xa.XAManager _xa");
    out.println("  = new com.caucho.ejb3.xa.XAManager();");
    
    
    out.println();
    out.println("private " + getBean().getClassName() + " _context;");
    out.println("private " + getBeanClassName() + " []_freeBeanStack"
		+ " = new " + getBeanClassName() + "[16];");
    out.println("private int _freeBeanTop;");

    out.println();
    out.println(getViewClassName() + "(" + getBean().getClassName() + " context)");
    out.println("{");
    generateSuper(out, "context.getStatelessServer(), "
		  + getApi().getName() + ".class");
    out.println("  _context = context;");

    out.println("}");

    out.println("public Object __caucho_get()");
    out.println("{");
    out.println("  return this;");
    out.println("}");

    generateProxyPool(out);

    generateBusinessMethods(out);

    /*
    for (BusinessMethodGenerator bizMethod : getMethods()) {
      out.println();

      bizMethod.generateHeader(out);
      out.println("{");
      out.pushDepth();

      out.println("Thread thread = Thread.currentThread();");
      out.println("ClassLoader oldLoader = thread.getContextClassLoader();");
      out.println();
      out.println("try {");
      out.pushDepth();
      out.println("thread.setContextClassLoader(getStatelessServer().getClassLoader());");
      out.println();

      generateProxyCall(out, bizMethod.getImplMethod());

      out.popDepth();
      out.println("} finally {");
      out.println("  thread.setContextClassLoader(oldLoader);");
      out.println("}");
      
      out.popDepth();
      out.println("}");
    }
    */
    
    out.popDepth();
    out.println("}");
  }
  
  protected void generateExtends(JavaWriter out)
    throws IOException
  {
    out.println("extends StatelessObject");
  }

  public void generateProxyPool(JavaWriter out)
    throws IOException
  {
    String beanClass = getBeanClassName();
    
    out.println();
    out.println(beanClass + " _ejb_begin()");
    out.println("{");
    out.pushDepth();
    out.println(beanClass + " bean;");
    out.println("synchronized (this) {");
    out.println("  if (_freeBeanTop > 0) {");
    out.println("    bean = _freeBeanStack[--_freeBeanTop];");
    out.println("    return bean;");
    out.println("  }");
    out.println("}");
    out.println();
    out.println("try {");
    out.println("  bean = new " + beanClass + "(this);");

    Class implClass = getBean().getEjbClass().getJavaClass();

    if (SessionBean.class.isAssignableFrom(implClass)) {
      out.println("  bean.setSessionContext(_context);");
    }
    
    out.println("  getStatelessServer().initInstance(bean);");

    

    if (getBean().hasMethod("ejbCreate", new Class[0])) {
      // ejb/0fe0: ejbCreate can be private, out.println("  bean.ejbCreate();");
      out.println("  bean.ejbCreate();");
    }

    out.println("  return bean;");
    out.println("} catch (Exception e) {");
    out.println("  throw com.caucho.ejb.EJBExceptionWrapper.create(e);");
    out.println("}");
    out.popDepth();
    out.println("}");

    out.println();
    out.println("void _ejb_free(" + beanClass + " bean)");
    out.println("  throws javax.ejb.EJBException");
    out.println("{");
    out.pushDepth();
    out.println("if (bean == null)");
    out.println("  return;");
    out.println();
    out.println("synchronized (this) {");
    out.println("  if (_freeBeanTop < _freeBeanStack.length) {");
    out.println("    _freeBeanStack[_freeBeanTop++] = bean;");
    out.println("    return;");
    out.println("  }");
    out.println("}");

    out.println("_server.destroyInstance(bean);");

    out.popDepth();
    out.println("}");

    out.println();
    out.println("public void destroy()");
    out.println("{");
    out.pushDepth();
    out.println(beanClass + " ptr;");
    out.println(beanClass + " []freeBeanStack;");
    out.println("int freeBeanTop;");

    out.println("synchronized (this) {");
    out.println("  freeBeanStack = _freeBeanStack;");
    out.println("  freeBeanTop = _freeBeanTop;");
    out.println("  _freeBeanStack = null;");
    out.println("  _freeBeanTop = 0;");
    out.println("}");

    out.println();
    out.println("for (int i = 0; i < freeBeanTop; i++) {");
    out.pushDepth();

    out.println("try {");
    out.println("  if (freeBeanStack[i] != null)");
    out.println("    _server.destroyInstance(freeBeanStack[i]);");
    out.println("} catch (Throwable e) {");
    out.println("  __caucho_log.log(java.util.logging.Level.WARNING, e.toString(), e);");
    out.println("}");

    out.popDepth();
    out.println("}");
    
    out.popDepth();
    out.println("}");
  }

  public void generateProxyCall(JavaWriter out, Method implMethod)
    throws IOException
  {
    if (! void.class.equals(implMethod.getReturnType())) {
      out.printClass(implMethod.getReturnType());
      out.println(" result;");
    }
    
    out.println(getBeanClassName() + " bean = _ejb_begin();");
    
    if (! void.class.equals(implMethod.getReturnType()))
      out.print("result = ");

    out.print("bean." + implMethod.getName() + "(");

    Class []types = implMethod.getParameterTypes();
    for (int i = 0; i < types.length; i++) {
      if (i != 0)
	out.print(", ");

      out.print(" a" + i);
    }
    
    out.println(");");
    
    out.println("_ejb_free(bean);");
    
    if (! void.class.equals(implMethod.getReturnType()))
      out.println("return result;");
  }


  protected void generateSuper(JavaWriter out, String serverVar)
    throws IOException
  {
    out.println("super(" + serverVar + ");");
  }

  @Override
    public void generateTimer(JavaWriter out)
    throws IOException
  {
    if (_timeoutMethod != null) {
      String localVar = "_local_" + getApi().getSimpleName();

      out.println(getBeanClassName() + " bean = " + localVar + "._ejb_begin();");
      out.println("bean." + _timeoutMethod + "(timer);");
      out.println(localVar + "._ejb_free(bean);");
    }
  }

  protected ApiMethod findImplMethod(ApiMethod apiMethod)
  {
    return getEjbClass().getMethod(apiMethod);
  }
}
