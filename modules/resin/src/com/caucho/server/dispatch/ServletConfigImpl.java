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

package com.caucho.server.dispatch;

import com.caucho.config.*;
import com.caucho.config.program.ConfigProgram;
import com.caucho.config.program.ContainerProgram;
import com.caucho.config.program.NodeBuilderProgram;
import com.caucho.config.types.InitParam;
import com.caucho.jmx.Jmx;
import com.caucho.jsp.Page;
import com.caucho.jsp.QServlet;
import com.caucho.naming.Jndi;
import com.caucho.remote.server.*;
import com.caucho.server.connection.StubServletRequest;
import com.caucho.server.connection.StubServletResponse;
import com.caucho.server.webapp.WebApp;
import com.caucho.servlet.comet.CometServlet;
import com.caucho.util.*;
import com.caucho.webbeans.component.*;
import com.caucho.webbeans.manager.*;

import javax.annotation.PostConstruct;
import javax.naming.NamingException;
import javax.servlet.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Configuration for a servlet.
 */
public class ServletConfigImpl implements ServletConfig, AlarmListener
{
  static L10N L = new L10N(ServletConfigImpl.class);
  protected static final Logger log
    = Logger.getLogger(ServletConfigImpl.class.getName());

  private String _location;

  private String _jndiName;
  private String _var;
  
  private String _servletName;
  private String _servletClassName;
  private Class _servletClass;
  private String _jspFile;
  private String _displayName;
  private int _loadOnStartup = Integer.MIN_VALUE;

  private boolean _allowEL = true;
  private HashMap<String,String> _initParams = new HashMap<String,String>();

  private HashMap<String,String> _roleMap;

  private ContainerProgram _init;

  private RunAt _runAt;

  private ServletProtocolConfig _protocolConfig;
  private ProtocolServletFactory _protocolFactory;
  
  private Alarm _alarm;
  private ComponentImpl _comp;

  private ServletContext _servletContext;
  private ServletManager _servletManager;

  private ServletException _initException;
  private long _nextInitTime;

  private Object _servlet;
  private FilterChain _servletChain;

  /**
   * Creates a new servlet configuration object.
   */
  public ServletConfigImpl()
  {
  }

  /**
   * Sets the config location.
   */
  public void setConfigLocation(String location, int line)
  {
    _location = location + ":" + line + ": ";
  }

  /**
   * Sets the id attribute
   */
  public void setId(String id)
  {
  }

  /**
   * Sets the servlet name.
   */
  public void setServletName(String name)
  {
    _servletName = name;
  }

  /**
   * Gets the servlet name.
   */
  public String getServletName()
  {
    return _servletName;
  }

  /**
   * Gets the servlet name.
   */
  public String getServletClassName()
  {
    return _servletClassName;
  }

  /**
   * Sets the servlet class.
   */
  public void setServletClass(String servletClassName)
  {
    _servletClassName = servletClassName;

    // JSF is special
    if ("javax.faces.webapp.FacesServlet".equals(_servletClassName)) {
      if (_loadOnStartup < 0)
	_loadOnStartup = 1;

      if (_servletContext instanceof WebApp)
	((WebApp) _servletContext).createJsp().setLoadTldOnInit(true);
    }
  }

  /**
   * Gets the servlet name.
   */
  public Class getServletClass()
  {
    if (_servletClassName == null)
      return null;
    
    if (_servletClass == null) {
      try {
	Thread thread = Thread.currentThread();
	ClassLoader loader = thread.getContextClassLoader();

        _servletClass = Class.forName(_servletClassName, false, loader);
      } catch (Exception e) {
	throw error(L.l("'{0}' is not a known servlet class.  Servlets belong in the classpath, for example WEB-INF/classes.",
			_servletClassName),
		    e);
      }
    }
      
    return _servletClass;
  }

  /**
   * Sets the JSP file
   */
  public void setJspFile(String jspFile)
  {
    _jspFile = jspFile;
  }

  /**
   * Gets the JSP file
   */
  public String getJspFile()
  {
    return _jspFile;
  }

  /**
   * Sets the allow value.
   */
  public void setAllowEL(boolean allowEL)
  {
    _allowEL = allowEL;
  }

  /**
   * Sets an init-param
   */
  public void setInitParam(String param, String value)
  {
    _initParams.put(param, value);
  }

  /**
   * Sets an init-param
   */
  public InitParam createInitParam()
  {
    InitParam initParam = new InitParam();

    initParam.setAllowEL(_allowEL);

    return initParam;
  }

  /**
   * Sets an init-param
   */
  public void setInitParam(InitParam initParam)
  {
    _initParams.putAll(initParam.getParameters());
  }

  /**
   * Gets the init params
   */
  public Map getInitParamMap()
  {
    return _initParams;
  }

  /**
   * Gets the init params
   */
  public String getInitParameter(String name)
  {
    return _initParams.get(name);
  }

  /**
   * Gets the init params
   */
  public Enumeration getInitParameterNames()
  {
    return Collections.enumeration(_initParams.keySet());
  }

  /**
   * Returns the servlet context.
   */
  public ServletContext getServletContext()
  {
    return _servletContext;
  }

  /**
   * Sets the servlet context.
   */
  public void setServletContext(ServletContext app)
  {
    _servletContext = app;
  }

  /**
   * Returns the servlet manager.
   */
  public ServletManager getServletManager()
  {
    return _servletManager;
  }

  /**
   * Sets the servlet manager.
   */
  public void setServletManager(ServletManager manager)
  {
    _servletManager = manager;
  }

  /**
   * Sets the init block
   */
  public void setInit(ContainerProgram init)
  {
    _init = init;
  }

  /**
   * Gets the init block
   */
  public ContainerProgram getInit()
  {
    return _init;
  }

  /**
   * Sets the load-on-startup
   */
  public void setLoadOnStartup(int loadOnStartup)
  {
    _loadOnStartup = loadOnStartup;
  }

  /**
   * Gets the load-on-startup value.
   */
  public int getLoadOnStartup()
  {
    if (_loadOnStartup > Integer.MIN_VALUE)
      return _loadOnStartup;
    else if (_runAt != null)
      return 0;
    else
      return Integer.MIN_VALUE;
  }

  /**
   * Creates the run-at configuration.
   */
  public RunAt createRunAt()
  {
    if (_runAt == null)
      _runAt = new RunAt();

    return _runAt;
  }

  public void setJndiName(String jndiName)
  {
    _jndiName = jndiName;
  }

  public void setVar(String var)
  {
    _var = var;
  }

  /**
   * Returns the run-at configuration.
   */
  public RunAt getRunAt()
  {
    return _runAt;
  }

  /**
   * Adds a security role reference.
   */
  public void addSecurityRoleRef(SecurityRoleRef ref)
  {
    if (_roleMap == null)
      _roleMap = new HashMap<String,String>(8);

    // server/12h2
    // server/12m0
    _roleMap.put(ref.getRoleName(), ref.getRoleLink());
  }

  /**
   * Adds a security role reference.
   */
  public HashMap<String,String> getRoleMap()
  {
    return _roleMap;
  }

  /**
   * Sets the display name
   */
  public void setDisplayName(String displayName)
  {
    _displayName = displayName;
  }

  /**
   * Gets the display name
   */
  public String getDisplayName()
  {
    return _displayName;
  }

  /**
   * Sets the description
   */
  public void setDescription(String description)
  {
  }

  /**
   * Sets the icon
   */
  public void setIcon(com.caucho.config.types.Icon icon)
  {
  }

  /**
   * Sets the web service protocol.
   */
  public void setProtocol(ServletProtocolConfig protocol)
  {
    _protocolConfig = protocol;
  }

  /**
   * Sets the init exception
   */
  public void setInitException(ServletException exn)
  {
    _initException = exn;

    _nextInitTime = Long.MAX_VALUE / 2;

    if (exn instanceof UnavailableException) {
      UnavailableException unExn = (UnavailableException) exn;

      if (! unExn.isPermanent())
        _nextInitTime = (Alarm.getCurrentTime() +
                         1000L * unExn.getUnavailableSeconds());
    }
  }

  /**
   * Returns the servlet.
   */
  public Object getServlet()
  {
    return _servlet;
  }

  /**
   * Initialize the servlet config.
   */
  @PostConstruct
  public void init()
    throws ServletException
  {
    if (_runAt != null) {
      _alarm = new Alarm(this);
    }

    if (_servletName != null) {
    }
    else if (_protocolConfig != null) {
      String protocolName = _protocolConfig.getUri();
      
      setServletName(_servletClassName + "-" + protocolName);
    }
    else
      setServletName(_servletClassName);

    // XXX: should only be for web services
    if (_jndiName != null) {
      validateClass(true);
      
      Object servlet = createServlet(false);

      try {
	Jndi.bindDeepShort(_jndiName, servlet);
      } catch (NamingException e) {
	throw new ServletException(e);
      }
    }

    if (_var != null) {
      validateClass(true);
      
      Object servlet = createServlet(false);

      WebBeansContainer webBeans = WebBeansContainer.create();
      webBeans.addSingleton(servlet, _var);
    }
  }

  protected void validateClass(boolean requireClass)
    throws ServletException
  {
    if (_runAt != null || _loadOnStartup >= 0)
      requireClass = true;

    Thread thread = Thread.currentThread();
    ClassLoader loader = thread.getContextClassLoader();

    if (_servletClassName == null) {
    }
    else if (_servletClassName.equals("invoker")) {
    }
    else {
      try {
        _servletClass = Class.forName(_servletClassName, false, loader);
      } catch (ClassNotFoundException e) {
	if (e instanceof CompileException)
	  throw error(e);
	
        log.log(Level.FINER, e.toString(), e);
      }

      if (_servletClass != null) {
      }
      else if (requireClass) {
        throw error(L.l("'{0}' is not a known servlet.  Servlets belong in the classpath, often in WEB-INF/classes.", _servletClassName));
      }
      else {
        String location = _location != null ? _location : "";

        log.warning(L.l(location + "'{0}' is not a known servlet.  Servlets belong in the classpath, often in WEB-INF/classes.", _servletClassName));
        return;
      }

      Config.checkCanInstantiate(_servletClass);

      if (Servlet.class.isAssignableFrom(_servletClass)) {
      }
      else if (_protocolConfig != null) {
      }
      /*
      else if (_servletClass.isAnnotationPresent(WebService.class)) {
	// update protocol for "soap"?
      } 
      else if (_servletClass.isAnnotationPresent(WebServiceProvider.class)) {
	// update protocol for "soap"?
      }
      */
      else
        throw error(L.l("'{0}' must implement javax.servlet.Servlet or have a <protocol>.  All servlets must implement the Servlet interface.", _servletClassName));

      /*
      if (Modifier.isAbstract(_servletClass.getModifiers()))
        throw error(L.l("'{0}' must not be abstract.  Servlets must be fully-implemented classes.", _servletClassName));

      if (! Modifier.isPublic(_servletClass.getModifiers()))
        throw error(L.l("'{0}' must be public.  Servlets must be public classes.", _servletClassName));

      checkConstructor();
      */
    }
  }

  /**
   * Checks the class constructor for the public-zero arg.
   */
  public void checkConstructor()
    throws ServletException
  {
    Constructor []constructors = _servletClass.getDeclaredConstructors();

    Constructor zeroArg = null;
    for (int i = 0; i < constructors.length; i++) {
      if (constructors[i].getParameterTypes().length == 0) {
        zeroArg = constructors[i];
        break;
      }
    }

    if (zeroArg == null)
      throw error(L.l("'{0}' must have a zero arg constructor.  Servlets must have public zero-arg constructors.\n{1} is not a valid constructor.", _servletClassName, constructors[0]));


    if (! Modifier.isPublic(zeroArg.getModifiers()))
        throw error(L.l("'{0}' must be public.  '{1}' must have a public, zero-arg constructor.",
                        zeroArg,
                        _servletClassName));
  }

  /**
   * Handles a cron alarm callback.
   */
  public void handleAlarm(Alarm alarm)
  {
    try {
      log.fine(this + " cron");

      FilterChain chain = createServletChain();

      ServletRequest req = new StubServletRequest();
      ServletResponse res = new StubServletResponse();

      chain.doFilter(req, res);
    } catch (Throwable e) {
      log.log(Level.WARNING, e.toString(), e);
    } finally {
      long nextTime = _runAt.getNextTimeout(Alarm.getCurrentTime());

      Alarm nextAlarm = _alarm;
      if (nextAlarm != null)
	alarm.queue(nextTime - Alarm.getCurrentTime());
    }
  }

  public FilterChain createServletChain()
    throws ServletException
  {
    synchronized (this) {
      // JSP files need to have separate chains created for each JSP
      
      if (_servletChain != null)
	return _servletChain;
      else
        return createServletChainImpl();
    }
  }

  private FilterChain createServletChainImpl()
    throws ServletException
  {
    String jspFile = getJspFile();
    FilterChain servletChain = null;

    if (jspFile != null) {
      QServlet jsp = (QServlet) _servletManager.createServlet("resin-jsp");

      servletChain = new PageFilterChain(_servletContext, jsp, jspFile, this);

      return servletChain;
    }

    validateClass(true);

    Class servletClass = getServletClass();

    if (servletClass == null) {
      throw new IllegalStateException(L.l("servlet class for {0} can't be null",
                                          getServletName()));
    }
    else if (QServlet.class.isAssignableFrom(servletClass)) {
      servletChain = new PageFilterChain(_servletContext, (QServlet) createServlet(false));
    }
    else if (SingleThreadModel.class.isAssignableFrom(servletClass)) {
      servletChain = new SingleThreadServletFilterChain(this);
    }
    else if (_protocolConfig != null) {
      servletChain = new WebServiceFilterChain(this);
    }
    else if (CometServlet.class.isAssignableFrom(servletClass))
      servletChain = new CometServletFilterChain(this);
    else {
      servletChain = new ServletFilterChain(this);
    }

    if (_roleMap != null)
      servletChain = new SecurityRoleMapFilterChain(servletChain, _roleMap);

    // server/10a8.  JSP pages need a fresh PageFilterChain
    // XXX: lock contention issues with JSPs?
    /*
    if (! QServlet.class.isAssignableFrom(servletClass))
      _servletChain = servletChain;
    */

    return servletChain;
  }

  /**
   * Instantiates a web service.
   *
   * @return the initialized servlet.
   */
  /*
  ProtocolServlet createWebServiceSkeleton()
    throws ServletException
  {
    try {
      Object service = createServlet(false);

      ProtocolServlet skeleton
        = (ProtocolServlet) _protocolClass.newInstance();

      skeleton.setService(service);

      if (_protocolInit != null) {
        _protocolInit.configure(skeleton);
      }

      skeleton.init(this);

      return skeleton;
    } catch (RuntimeException e) {
      throw e;
    } catch (ServletException e) {
      throw e;
    } catch (Exception e) {
      throw new ServletException(e);
    }
  }
  */

  /**
   * Instantiates a servlet given its configuration.
   *
   * @param servletName the servlet
   *
   * @return the initialized servlet.
   */
  Object createServlet(boolean isNew)
    throws ServletException
  {
    // server/102e
    if (_servlet != null && ! isNew)
      return _servlet;

    Object servlet = null;

    if (Alarm.getCurrentTime() < _nextInitTime)
      throw _initException;

    try {
      synchronized (this) {
	if (! isNew && _servlet != null)
	  return _servlet;
	  
	// XXX: this was outside of the sync block
	servlet = createServletImpl();

	if (! isNew)
	  _servlet = servlet;
      }

      if (log.isLoggable(Level.FINE))
        log.finer("Servlet[" + _servletName + "] active");

      //J2EEManagedObject.register(new com.caucho.management.j2ee.Servlet(this));

      if (! isNew) {
	// If the servlet has an MBean, register it
	try {
	  Hashtable<String,String> props = new Hashtable<String,String>();

	  props.put("type", _servlet.getClass().getSimpleName());
	  props.put("name", _servletName);
	  Jmx.register(_servlet, props);
	} catch (Exception e) {
	  log.finest(e.toString());
	}

	if (_runAt != null && _alarm != null) {
	  long nextTime = _runAt.getNextTimeout(Alarm.getCurrentTime());
	  _alarm.queue(nextTime - Alarm.getCurrentTime());
	}
      }

      if (log.isLoggable(Level.FINE))
        log.finer("Servlet[" + _servletName + "] active");

      return servlet;
    } catch (ServletException e) {
      throw e;
    } catch (Throwable e) {
      throw new ServletException(e);
    }
  }

  Servlet createProtocolServlet()
    throws ServletException
  {
    try {
      Object service = createServletImpl();

      if (_protocolFactory == null)
	_protocolFactory = _protocolConfig.createFactory();

      Servlet servlet = _protocolFactory.createServlet(_servletClass, service);

      servlet.init(this);

      return servlet;
    } catch (RuntimeException e) {
      throw e;
    } catch (ServletException e) {
      throw e;
    } catch (Exception e) {
      throw new ServletException(e);
    }
  }

  private Object createServletImpl()
    throws Exception
  {
    Class servletClass = getServletClass();

    Object servlet;
    if (_jspFile != null) {
      servlet = createJspServlet(_servletName, _jspFile);

      if (servlet == null)
        throw new ServletException(L.l("'{0}' is a missing JSP file.",
                                       _jspFile));
    }

    else if (servletClass != null) {
      WebBeansContainer webBeans = WebBeansContainer.create();
      
      _comp = (ComponentImpl) webBeans.createTransient(servletClass);
      
      servlet = _comp.createNoInit();
    }
    else
      throw new ServletException(L.l("Null servlet class for '{0}'.",
                                     _servletName));

    configureServlet(servlet);

    try {
      if (servlet instanceof Page) {
	// server/102i
	// page already configured
      }
      else if (servlet instanceof Servlet) {
	Servlet servletObj = (Servlet) servlet;
	
	servletObj.init(this);
      }
    } catch (UnavailableException e) {
      setInitException(e);
      throw e;
    }

    return servlet;
  }

  /**
   *  Configure the servlet (everything that is done after
   *  instantiation but before servlet.init()
   */
  void configureServlet(Object servlet)
  {
    //InjectIntrospector.configure(servlet);

    // Initialize bean properties
    ConfigProgram init = getInit();

    if (init != null)
      init.configure(servlet);

    Config.init(servlet);
  }

  /**
   * Instantiates a servlet given its configuration.
   *
   * @param servletName the servlet
   *
   * @return the initialized servlet.
   */
  private Servlet createJspServlet(String servletName, String jspFile)
    throws ServletException
  {
    try {
      ServletConfigImpl jspConfig = _servletManager.getServlet("resin-jsp");

      QServlet jsp = (QServlet) jspConfig.createServlet(false);

      // server/105o
      Page page = jsp.getPage(servletName, jspFile, this);

      return page;
    } catch (ServletException e) {
      throw e;
    } catch (Exception e) {
      throw new ServletException(e);
    }
  }

  void killServlet()
  {
    Object servlet = _servlet;
    _servlet = null;

    Alarm alarm = _alarm;
    _alarm = null;
    
    if (alarm != null)
      alarm.dequeue();
    
    if (_comp != null)
      _comp.destroy(servlet);

    if (servlet instanceof Servlet) {
      ((Servlet) servlet).destroy();
    }
  }

  public void close()
  {
    killServlet();

    _alarm = null;
  }

  protected ConfigException error(String msg)
  {
    if (_location != null)
      return new LineConfigException(_location + msg);
    else
      return new ConfigException(msg);
  }

  protected ConfigException error(String msg, Throwable e)
  {
    if (_location != null)
      return new LineConfigException(_location + msg, e);
    else
      return new ConfigException(msg, e);
  }

  protected RuntimeException error(Throwable e)
  {
    if (_location != null)
      return new LineConfigException(_location + e.getMessage(), e);
    else
      return ConfigException.create(e);
  }

  /**
   * Returns a printable representation of the servlet config object.
   */
  public String toString()
  {
    return "ServletConfigImpl[name=" + _servletName + ",class=" + _servletClass + "]";
  }
}
