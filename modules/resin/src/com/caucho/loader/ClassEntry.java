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

package com.caucho.loader;

import com.caucho.log.Log;
import com.caucho.make.DependencyContainer;
import com.caucho.util.ByteBuffer;
import com.caucho.util.QDate;
import com.caucho.vfs.Depend;
import com.caucho.vfs.Dependency;
import com.caucho.vfs.JarPath;
import com.caucho.vfs.Path;
import com.caucho.vfs.PersistentDependency;
import com.caucho.vfs.ReadStream;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.security.CodeSource;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Describes a cached loaded class entry.
 */
public class ClassEntry implements Dependency {
  private static final Logger log = Log.open(ClassEntry.class);
  
  private static boolean _hasJNIReload;
  private static boolean _hasAnnotations;

  private DynamicClassLoader _loader;
  private String _name;
    
  private Path _classPath;

  private PersistentDependency _depend;

  private boolean _classIsModified;
    
  private Path _sourcePath;
  private long _sourceLastModified;
  private long _sourceLength;

  private ClassPackage _classPackage;

  private CodeSource _codeSource;

  private ClassNotFoundException _exn;
    
  private WeakReference<Class> _clRef;

  /**
   * Create a loaded class entry
   *
   * @param name the classname
   * @param sourcePath path to the source Java file
   * @param classPath path to the compiled class file
   */
  public ClassEntry(DynamicClassLoader loader,
                    String name, Path sourcePath,
                    Path classPath,
		    CodeSource codeSource)
  {
    _loader = loader;
    _name = name;

    _classPath = classPath;

    setDependPath(classPath);

    if (sourcePath != null && ! sourcePath.equals(classPath)) {
      _sourcePath = sourcePath;
      
      _sourceLastModified = sourcePath.getLastModified();
      _sourceLength = sourcePath.getLength();
    }

    _codeSource = codeSource;

    /*
    if (codePath == null)
      codePath = classPath;

    if (_loader.getSecurityManager() != null) {
      try {
        _codeSource = new CodeSource(new URL(codePath.getURL()), null);
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    */
  }

  /**
   * Create a loaded class entry
   *
   * @param name the classname
   * @param sourcePath path to the source Java file
   * @param classPath path to the compiled class file
   */
  public ClassEntry(Loader loader,
                    String name, Path sourcePath,
                    Path classPath)
  {
    this(loader.getLoader(), name, sourcePath, classPath,
	 loader.getCodeSource(classPath));
  }

  public String getName()
  {
    return _name;
  }

  /**
   * returns the class loader.
   */
  public DynamicClassLoader getClassLoader()
  {
    return _loader;
  }

  public CodeSource getCodeSource()
  {
    return _codeSource;
  }

  public Path getSourcePath()
  {
    return _sourcePath;
  }

  /**
   * Sets the depend path.
   */
  protected void setDependPath(Path dependPath)
  {
    if (dependPath instanceof JarPath)
      _depend = ((JarPath) dependPath).getDepend();
    else
      _depend = new Depend(dependPath);
  }

  /**
   * Adds the dependencies, returning true if it's adding itself.
   */
  protected boolean addDependencies(DependencyContainer container)
  {
    if (_classPath instanceof JarPath) {
      container.add(_depend);
      return false;
    }
    else if (_hasJNIReload) {
      container.add(this);
      return true;
    }
    else if (_sourcePath == null) {
      container.add(_depend);
      return false;
    }
    else {
      container.add(this);
      return true;
    }
  }
  
  public void setSourceLength(long length)
  {
    _sourceLength = length;
  }
  
  public void setSourceLastModified(long lastModified)
  {
    _sourceLastModified = lastModified;
  }
  
  public ClassPackage getClassPackage()
  {
    return _classPackage;
  }

  public void setClassPackage(ClassPackage pkg)
  {
    _classPackage = pkg;
  }

  /**
   * Returns true if the source file has been modified.
   */
  public boolean isModified()
  {
    if (_depend.isModified()) {
      if (log.isLoggable(Level.FINE))
        log.fine("class modified: " + _depend);

      return reloadIsModified();
    }
    else if (_sourcePath == null)
      return false;
      
    else if (_sourcePath.getLastModified() != _sourceLastModified) {
      if (log.isLoggable(Level.FINE))
        log.fine("source modified time: " + _sourcePath +
                 " old:" + QDate.formatLocal(_sourceLastModified) +
                 " new:" + QDate.formatLocal(_sourcePath.getLastModified()));

      if (! compileIsModified()) {
	return false;
      }
      
      boolean isModified = reloadIsModified();

      return isModified;
    }
    else if (_sourcePath.getLength() != _sourceLength) {
      if (log.isLoggable(Level.FINE))
        log.fine("source modified length: " + _sourcePath +
                 " old:" + _sourceLength +
                 " new:" + _sourcePath.getLength());

      if (! compileIsModified())
	return false;

      return reloadIsModified();
    }
    else {
      return false;
    }
  }

  /**
   * Returns true if the source file has been modified.
   */
  public boolean logModified(Logger log)
  {
    if (_depend.logModified(log)) {
      return true;
    }
    else if (_sourcePath == null)
      return false;
      
    else if (_sourcePath.getLastModified() != _sourceLastModified) {
      log.info("source modified time: " + _sourcePath +
	       " old:" + QDate.formatLocal(_sourceLastModified) +
	       " new:" + QDate.formatLocal(_sourcePath.getLastModified()));

      return true;
    }
    else if (_sourcePath.getLength() != _sourceLength) {
      log.info("source modified length: " + _sourcePath +
	       " old:" + _sourceLength +
	       " new:" + _sourcePath.getLength());

      return true;
    }
    else {
      return false;
    }
  }

  /**
   * Returns true if the compile doesn't avoid the dependency.
   */
  public boolean compileIsModified()
  {
    return true;
  }

  /**
   * Returns true if the reload doesn't avoid the dependency.
   */
  public boolean reloadIsModified()
  {
    if (_classIsModified) {
      return true;
    }

    if (! _hasJNIReload || ! _classPath.canRead()) {
      return true;
    }
    
    try {
      long length = _classPath.getLength();

      Class cl = _clRef != null ? _clRef.get() : null;
	
      if (cl == null) {
	return false;
      }

      if (_hasAnnotations && requireReload(cl))
	return true;
	    
      ReadStream is = _classPath.openRead();

      byte []bytecode = new byte[(int) length];
	    
      try {
	is.readAll(bytecode, 0, bytecode.length);
      } finally {
	is.close();
      }

      int result = reloadNative(cl, bytecode, 0, bytecode.length);
	
      if (result != 0) {
	_classIsModified = true;
	return true;
      }

      setDependPath(_classPath);
      
      if (_sourcePath != null) {
	_sourceLastModified = _sourcePath.getLastModified();
	_sourceLength = _sourcePath.getLength();
      }

      log.info("Reloading " + cl.getName());

      return false;
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);
      _classIsModified = true;

      return true;
    }
  }

  /**
   * Returns the path to the class file.
   */
  public Path getClassPath()
  {
    return _classPath;
  }

  public Class getEntryClass()
  {
    return _clRef != null ? _clRef.get() : null;
  }

  public void setEntryClass(Class cl)
  {
    _clRef = new WeakReference<Class>(cl);
  }

  /**
   * preload actions.
   */
  public void preLoad()
    throws ClassNotFoundException
  {
  }

  /**
   * Loads the contents of the class file into the buffer.
   */
  public void load(ByteBuffer buffer)
    throws IOException
  {
    synchronized (this) {
      Path classPath = getClassPath();
    
      buffer.clear();
      long length = classPath.getLength();
      if (length < 0)
	throw new IOException("missing class: " + classPath);
      ReadStream is = classPath.openRead();
      try {
	buffer.setLength((int) length);
	if (is.readAll(buffer.getBuffer(), 0, (int) length) != length)
	  throw new IOException("class file length mismatch");
      } finally {
	is.close();
      }
    }
  }

  /**
   * post-load actions.
   */
  public boolean postLoad()
  {
    return false;
  }

  public static boolean hasJNIReload()
  {
    return _hasJNIReload;
  }

  private static boolean requireReload(Class cl)
  {
    return cl.isAnnotationPresent(RequireReload.class);
  }

  public String toString()
  {
    if (_sourcePath == null)
      return "ClassEntry[" + _classPath + "]";
    else
      return "ClassEntry[" + _classPath + ", src=" + _sourcePath + "]";
  }

  public static native boolean canReloadNative();

  public static native int reloadNative(Class cl,
		  		        byte []bytes, int offset, int length);

  class ReloadThread implements Runnable {
    private volatile boolean _isDone;

    public boolean isDone()
    {
      return _isDone;
    }
    
    public void run()
    {
    }
  }
  
  static {
    try {
      System.loadLibrary("resin_os");
      _hasJNIReload = canReloadNative();
      if (_hasJNIReload)
	log.config("In-place class redefinition (HotSwap) is available.");
      else
	log.config("In-place class redefinition (HotSwap) is not available.  In-place class reloading during development requires a compatible JDK and -Xdebug.");
    } catch (Throwable e) {
      log.fine(e.toString());
      
      log.log(Level.FINEST, e.toString(), e);
      // log.config("In-place class redefinition (HotSwap) is not available.\n" + e);
    }
    
    try {
      Class reloadClass = Class.forName("com.caucho.loader.RequireReload");
      
      Object.class.getAnnotation(reloadClass);

      _hasAnnotations = true;
    } catch (Throwable e) {
    }
  }
}
