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

package com.caucho.loader;

/**
 * Creates a ClassLoader dependent variable.
 * The value of the ClassLoaderLocal
 * variable depends on the context ClassLoader.
 */
public class EnvironmentLocal<E> {
  private static int _varCount;
  
  private String _varName;
  private E _globalValue;

  /**
   * Creates a new environment local variable with an anonymous
   * identifier.
   */
  public EnvironmentLocal()
  {
    _varName = "resin:var-" + _varCount++;
  }

  public EnvironmentLocal(String varName)
  {
    _varName = varName;
  }

  public String getVariable()
  {
    return _varName;
  }

  /**
   * Returns the variable for the context classloader.
   */
  public E get()
  {
    Thread thread = Thread.currentThread();
    ClassLoader loader = thread.getContextClassLoader();

    Object value = null;

    for (; loader != null; loader = loader.getParent()) {
      if (loader instanceof EnvironmentClassLoader) {
        EnvironmentClassLoader envLoader = (EnvironmentClassLoader) loader;

        value = envLoader.getAttribute(_varName);

        if (value != null)
          return (E) value;
      }
    }

    return _globalValue;
  }

  /**
   * Returns the variable for the context classloader.
   */
  public E get(ClassLoader loader)
  {
    Object value = null;

    for (; loader != null; loader = loader.getParent()) {
      if (loader instanceof EnvironmentClassLoader) {
        EnvironmentClassLoader envLoader = (EnvironmentClassLoader) loader;

        value = envLoader.getAttribute(_varName);

        if (value != null)
          return (E) value;
      }
    }

    return _globalValue;
  }

  /**
   * Returns the variable for the context classloader.
   */
  public E getLevel()
  {
    Thread thread = Thread.currentThread();
    ClassLoader loader = thread.getContextClassLoader();

    for (; loader != null; loader = loader.getParent()) {
      if (loader instanceof EnvironmentClassLoader) {
        EnvironmentClassLoader envLoader = (EnvironmentClassLoader) loader;

        return (E) envLoader.getAttribute(_varName);
      }
    }

    return _globalValue;
  }

  /**
   * Returns the variable for the context classloader.
   */
  public E getLevel(ClassLoader loader)
  {
    for (; loader != null; loader = loader.getParent()) {
      if (loader instanceof EnvironmentClassLoader) {
        EnvironmentClassLoader envLoader = (EnvironmentClassLoader) loader;

        return (E) envLoader.getAttribute(_varName);
      }
    }

    return _globalValue;
  }

  /**
   * Sets the variable for the context classloader.
   *
   * @param value the new value
   *
   * @return the old value
   */
  public final E set(E value)
  {
    Thread thread = Thread.currentThread();
    ClassLoader loader = thread.getContextClassLoader();
    
    for (; loader != null; loader = loader.getParent()) {
      if (loader instanceof EnvironmentClassLoader) {
        EnvironmentClassLoader envLoader = (EnvironmentClassLoader) loader;

        return (E) envLoader.setAttribute(_varName, value);
      }
    }

    return setGlobal(value);
  }

  /**
   * Sets the variable for the context classloader.
   *
   * @param value the new value
   *
   * @return the old value
   */
  public final E set(E value, ClassLoader loader)
  {
    for (; loader != null; loader = loader.getParent()) {
      if (loader instanceof EnvironmentClassLoader) {
        EnvironmentClassLoader envLoader = (EnvironmentClassLoader) loader;

        return (E) envLoader.setAttribute(_varName, value);
      }
    }

    return setGlobal(value);
  }

  /**
   * Sets the variable for the context classloader.
   *
   * @param value the new value
   *
   * @return the old value
   */
  public final E remove()
  {
    Thread thread = Thread.currentThread();
    ClassLoader loader = thread.getContextClassLoader();
    
    for (; loader != null; loader = loader.getParent()) {
      if (loader instanceof EnvironmentClassLoader) {
        EnvironmentClassLoader envLoader = (EnvironmentClassLoader) loader;

        return (E) envLoader.removeAttribute(_varName);
      }
    }

    return setGlobal(null);
  }

  /**
   * Sets the variable for the context classloader.
   *
   * @param value the new value
   *
   * @return the old value
   */
  public final E remove(ClassLoader loader)
  {
    for (; loader != null; loader = loader.getParent()) {
      if (loader instanceof EnvironmentClassLoader) {
        EnvironmentClassLoader envLoader = (EnvironmentClassLoader) loader;
	
        return (E) envLoader.removeAttribute(_varName);
      }
    }

    return setGlobal(null);
  }

  /**
   * Sets the global value.
   *
   * @param value the new value
   *
   * @return the old value
   */
  public E setGlobal(E value)
  {
    E oldValue = _globalValue;
    
    _globalValue = value;

    ClassLoader systemLoader = ClassLoader.getSystemClassLoader();
    if (systemLoader instanceof EnvironmentClassLoader)
      ((EnvironmentClassLoader) systemLoader).setAttribute(_varName, value);

    return oldValue;
  }

  /**
   * Returns the global value.
   */
  public E getGlobal()
  {
    ClassLoader systemLoader = ClassLoader.getSystemClassLoader();
    if (systemLoader instanceof EnvironmentClassLoader)
      return (E) ((EnvironmentClassLoader) systemLoader).getAttribute(_varName);
      
    return _globalValue;
  }
}
