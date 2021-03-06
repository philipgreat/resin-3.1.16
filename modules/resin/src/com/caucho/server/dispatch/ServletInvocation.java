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

import com.caucho.servlet.comet.CometFilterChain;
import com.caucho.util.L10N;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.HashMap;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * A repository for request information gleaned from the uri.
 */
public class ServletInvocation {
  static final Logger log
    = Logger.getLogger(ServletInvocation.class.getName());
  static final L10N L = new L10N(ServletInvocation.class);

  private final boolean _isFiner;

  private static final ThreadLocal<ServletRequest> _requestThreadLocal
    = new ThreadLocal<ServletRequest>();

  private ClassLoader _classLoader;
  
  private String _contextPath = "";
  
  private String _contextUri;
  private String _servletPath;
  private String _pathInfo;
  
  private String _queryString;
  
  private String _servletName;
  private FilterChain _filterChain;

  private AtomicLong _requestCount = new AtomicLong();

  private HashMap<String,String> _securityRoleMap;

  /**
   * Creates a new invocation
   *
   * @param contextUri the section of the URI after the context path
   */
  public ServletInvocation()
  {
    _classLoader = Thread.currentThread().getContextClassLoader();

    _isFiner = log.isLoggable(Level.FINER);
  }

  /**
   * Returns the mapped context-path.
   */
  public final String getContextPath()
  {
    return _contextPath;
  }

  /**
   * Sets the context-path.
   */
  public void setContextPath(String path)
  {
    _contextPath = path;
  }

  public void setContextURI(String contextURI)
  {
    _contextUri = contextURI;
    _servletPath = contextURI;
  }

  /**
   * Returns the URI tail, i.e. everything after the context path.
   */
  public final String getContextURI()
  {
    return _contextUri;
  }

  /**
   * Returns the mapped servlet path.
   */
  public final String getServletPath()
  {
    return _servletPath;
  }

  /**
   * Sets the mapped servlet path.
   */
  public void setServletPath(String servletPath)
  {
    _servletPath = servletPath;
  }

  /**
   * Returns the mapped path info.
   */
  public final String getPathInfo()
  {
    return _pathInfo;
  }

  /**
   * Sets the mapped path info
   */
  public void setPathInfo(String pathInfo)
  {
    _pathInfo = pathInfo;
  }

  /**
   * Returns the query string.  Characters remain unescaped.
   */
  public final String getQueryString()
  {
    return _queryString;
  }

  /**
   * Returns the query string.  Characters remain unescaped.
   */
  public final void setQueryString(String queryString)
  {
    _queryString = queryString;
  }

  /**
   * Sets the class loader.
   */
  public void setClassLoader(ClassLoader loader)
  {
    _classLoader = loader;
  }

  /**
   * Gets the class loader.
   */
  public ClassLoader getClassLoader()
  {
    return _classLoader;
  }

  /**
   * Sets the servlet name
   */
  public void setServletName(String servletName)
  {
    _servletName = servletName;
  }

  /**
   * Gets the servlet name
   */
  public String getServletName()
  {
    return _servletName;
  }

  /**
   * Sets the filter chain
   */
  public void setFilterChain(FilterChain chain)
  {
    _filterChain = chain;
  }

  /**
   * Gets the filter chain
   */
  public FilterChain getFilterChain()
  {
    return _filterChain;
  }

  /**
   * Gets the security role map.
   */
  public HashMap<String,String> getSecurityRoleMap()
  {
    return _securityRoleMap;
  }

  /**
   * Sets the security role map.
   */
  public void setSecurityRoleMap(HashMap<String,String> roleMap)
  {
    _securityRoleMap = roleMap;
  }

  /**
   * Returns the number of requests.
   */
  public long getRequestCount()
  {
    return _requestCount.get();
  }

  /**
   * Returns the thread request.
   */
  public static ServletRequest getContextRequest()
  {
    return _requestThreadLocal.get();
  }

  /**
   * Service a request.
   *
   * @param request the servlet request
   * @param response the servlet response
   */
  public void service(ServletRequest request, ServletResponse response)
    throws IOException, ServletException
  {
    _requestCount.incrementAndGet();

    ThreadLocal<ServletRequest> requestThreadLocal = _requestThreadLocal;
    ServletRequest oldRequest = requestThreadLocal.get();

    try {
      requestThreadLocal.set(request);

      if (_isFiner)
	log.finer("Dispatch '" + _contextUri + "' to " + _filterChain);
      
      _filterChain.doFilter(request, response);
    } finally {
      requestThreadLocal.set(oldRequest);
    }
  }

  /**
   * Resume a request.
   *
   * @param request the servlet request
   * @param response the servlet response
   */
  public boolean doResume(ServletRequest request, ServletResponse response)
    throws IOException, ServletException
  {
    if (_filterChain instanceof CometFilterChain) {
      CometFilterChain filterChain = (CometFilterChain) _filterChain;

      return filterChain.doResume(request, response);
    }
    else
      return false;
  }

  /**
   * Copies from the invocation.
   */
  public void copyFrom(ServletInvocation invocation)
  {
    _classLoader = invocation._classLoader;
    _contextPath = invocation._contextPath;
  
    _contextUri = invocation._contextUri;
    _servletPath = invocation._servletPath;
    _pathInfo = invocation._pathInfo;
  
    _queryString = invocation._queryString;
  
    _servletName = invocation._servletName;
    _filterChain = invocation._filterChain;
    
    _securityRoleMap = invocation._securityRoleMap;
  }

  public String toString()
  {
    return "ServletInvocation[" + _contextUri + "]";
  }
}
