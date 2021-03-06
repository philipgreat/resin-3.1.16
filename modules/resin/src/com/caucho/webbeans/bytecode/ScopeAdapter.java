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

package com.caucho.webbeans.bytecode;

import java.io.*;
import java.util.*;
import java.lang.reflect.*;

import com.caucho.bytecode.*;
import com.caucho.config.*;
import com.caucho.loader.*;
import com.caucho.webbeans.cfg.*;
import com.caucho.webbeans.component.*;
import com.caucho.util.*;
import com.caucho.vfs.*;

/**
 * Scope adapting
 */
public class ScopeAdapter {
  private static final L10N L = new L10N(ScopeAdapter.class);
  
  private final Class _cl;
  
  private Class _proxyClass;
  private Constructor _proxyCtor;

  private ScopeAdapter(Class cl)
  {
    _cl = cl;

    generateProxy(cl);
  }
  
  public static ScopeAdapter create(Class cl)
  {
    ScopeAdapter adapter = new ScopeAdapter(cl);
    
    return adapter;
  }
    
  public Object wrap(ComponentImpl comp)
  {
    try {
      Object v = _proxyCtor.newInstance(comp);
      return v;
    } catch (Exception e) {
      throw ConfigException.create(e);
    }
  }

  private void generateProxy(Class cl)
  {
    try {
      Constructor zeroCtor = null;

      for (Constructor ctorItem : cl.getConstructors()) {
	if (ctorItem.getParameterTypes().length == 0) {
	  if (Modifier.isPublic(ctorItem.getModifiers())
	      || Modifier.isProtected(ctorItem.getModifiers())) {
	    zeroCtor = ctorItem;
	    break;
	  }
	}
      }

      if (zeroCtor == null) {
	throw new ConfigException(L.l("'{0}' does not have a zero-arg public or protected constructor.  Scope adapter components need a zero-arg constructor, e.g. @RequestScoped stored in @ApplicationScoped.",
				      cl.getName()));
      }
      
      JavaClassLoader jLoader = new JavaClassLoader(cl.getClassLoader());
      
      JavaClass jClass = new JavaClass(jLoader);
      jClass.setAccessFlags(Modifier.PUBLIC);
      ConstantPool cp = jClass.getConstantPool();

      jClass.setWrite(true);
      
      jClass.setMajor(49);
      jClass.setMinor(0);

      String superClassName = cl.getName().replace('.', '/');
      String thisClassName = superClassName + "$ScopeProxy";

      jClass.setSuperClass(superClassName);
      jClass.setThisClass(thisClassName);

      JavaField jField
	= jClass.createField("_cxt", "Lcom/caucho/webbeans/component/ComponentImpl;");
      jField.setAccessFlags(Modifier.PRIVATE);

      JavaMethod ctor
	= jClass.createMethod("<init>",
			      "(Lcom/caucho/webbeans/component/ComponentImpl;)V");
      ctor.setAccessFlags(Modifier.PUBLIC);
      
      CodeWriterAttribute code = ctor.createCodeWriter();
      code.setMaxLocals(2);
      code.setMaxStack(4);

      code.pushObjectVar(0);
      code.invokespecial(superClassName, "<init>", "()V", 1, 0);
      code.pushObjectVar(0);
      code.pushObjectVar(1);
      code.putField(thisClassName, jField.getName(), jField.getDescriptor());
      code.addReturn();
      code.close();

      for (Method method : _cl.getMethods()) {
	if (Modifier.isStatic(method.getModifiers()))
	  continue;
	if (Modifier.isFinal(method.getModifiers()))
	  continue;
	
	createProxyMethod(jClass, method);
      }
    
      ByteArrayOutputStream bos = new ByteArrayOutputStream();
      WriteStream out = Vfs.openWrite(bos);

      jClass.write(out);
    
      out.close();

      byte []buffer = bos.toByteArray();
      
      /*
      out = Vfs.lookup("file:/tmp/caucho/qa/temp.class").openWrite();
      out.write(buffer, 0, buffer.length);
      out.close();
      */

      String cleanName = thisClassName.replace('/', '.');
      _proxyClass = new ProxyClassLoader().loadClass(cleanName, buffer);
      _proxyCtor = _proxyClass.getConstructors()[0];
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private void createProxyMethod(JavaClass jClass, Method method)
  {
    String descriptor = createDescriptor(method);
    
    JavaMethod jMethod = jClass.createMethod(method.getName(),
					     descriptor);
    jMethod.setAccessFlags(Modifier.PUBLIC);

    Class []parameterTypes = method.getParameterTypes();
      
    CodeWriterAttribute code = jMethod.createCodeWriter();
    code.setMaxLocals(1 + 2 * parameterTypes.length);
    code.setMaxStack(2 + 2 * parameterTypes.length);

    code.pushObjectVar(0);
    code.getField(jClass.getThisClass(), "_cxt",
		  "Lcom/caucho/webbeans/component/ComponentImpl;");
    
    code.invoke("com/caucho/webbeans/component/ComponentImpl",
		"get", "()Ljava/lang/Object;", 1, 1);
    
    code.cast(method.getDeclaringClass().getName().replace('.', '/'));

    int stack = 1;
    int index = 1;
    for (Class type : parameterTypes) {
      if (boolean.class.equals(type)
	  || byte.class.equals(type)
	  || short.class.equals(type)
	  || int.class.equals(type)) {
	code.pushIntVar(index);
	index += 1;
	stack += 1;
      }
      else if (long.class.equals(type)) {
	code.pushLongVar(index);
	index += 2;
	stack += 2;
      }
      else if (float.class.equals(type)) {
	code.pushFloatVar(index);
	index += 1;
	stack += 1;
      }
      else if (double.class.equals(type)) {
	code.pushDoubleVar(index);
	index += 2;
	stack += 2;
      }
      else {
	code.pushObjectVar(index);
	index += 1;
	stack += 1;
      }
    }
    
    code.invoke(method.getDeclaringClass().getName().replace('.', '/'),
		method.getName(),
		createDescriptor(method),
		stack, 1);

    Class retType = method.getReturnType();
    
    if (boolean.class.equals(retType)
	|| byte.class.equals(retType)
	|| short.class.equals(retType)
	|| int.class.equals(retType)) {
      code.addIntReturn();
    }
    else if (long.class.equals(retType)) {
      code.addLongReturn();
    }
    else if (float.class.equals(retType)) {
      code.addFloatReturn();
    }
    else if (double.class.equals(retType)) {
      code.addDoubleReturn();
    }
    else if (void.class.equals(retType)) {
      code.addReturn();
    }
    else {
      code.addObjectReturn();
    }
    
    code.close();
  }

  private String createDescriptor(Method method)
  {
    StringBuilder sb = new StringBuilder();

    sb.append("(");
    
    for (Class param : method.getParameterTypes()) {
      sb.append(createDescriptor(param));
    }
    
    sb.append(")");
    sb.append(createDescriptor(method.getReturnType()));

    return sb.toString();
  }

  private String createDescriptor(Class cl)
  {
    if (cl.isArray())
      return "[" + createDescriptor(cl.getComponentType());

    String primValue = _prim.get(cl);

    if (primValue != null)
      return primValue;

    return "L" + cl.getName().replace('.', '/') + ";";
  }

  private static HashMap<Class,String> _prim = new HashMap<Class,String>();

  static {
    _prim.put(boolean.class, "Z");
    _prim.put(byte.class, "B");
    _prim.put(char.class, "C");
    _prim.put(short.class, "S");
    _prim.put(int.class, "I");
    _prim.put(long.class, "J");
    _prim.put(float.class, "F");
    _prim.put(double.class, "D");
    _prim.put(void.class, "V");
  }
}
