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

package com.caucho.server.dispatch;

import com.caucho.config.Config;
import com.caucho.config.program.ContainerProgram;
import com.caucho.log.Log;
import com.caucho.util.L10N;
import com.caucho.webbeans.component.*;
import com.caucho.webbeans.manager.*;

import javax.annotation.PostConstruct;
import javax.servlet.Filter;
import javax.servlet.ServletException;
import javax.webbeans.*;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages the servlets.
 */
public class FilterManager {
  static final Logger log = Logger.getLogger(FilterManager.class.getName());
  
  static final L10N L = new L10N(FilterManager.class);

  private Hashtable<String,FilterConfigImpl> _filters
    = new Hashtable<String,FilterConfigImpl>();

  private ComponentImpl _comp;
  
  private Hashtable<String,Filter> _instances
    = new Hashtable<String,Filter>();

  /**
   * Adds a filter to the filter manager.
   */
  public void addFilter(FilterConfigImpl config)
  {
    if (config.getServletContext() == null)
      throw new NullPointerException();
    
    _filters.put(config.getFilterName(), config);
  }

  /**
   * Adds a filter to the filter manager.
   */
  public FilterConfigImpl getFilter(String filterName)
  {
    return _filters.get(filterName);
  }

  /**
   * Initialize filters that need starting at server start.
   */
  @PostConstruct
  public void init()
  {
    for (String name : _filters.keySet()) {
      try {
	createFilter(name);
      } catch (Exception e) {
	log.log(Level.WARNING, e.toString(), e);
      }
    }
  }

  /**
   * Instantiates a filter given its configuration.
   *
   * @param filterName the filter
   *
   * @return the initialized filter.
   */
  public Filter createFilter(String filterName)
    throws ServletException
  {
    FilterConfigImpl config = _filters.get(filterName);

    if (config == null) {
      throw new ServletException(L.l("`{0}' is not a known filter.  Filters must be defined by <filter> before being used.", filterName));
    }
    
    Class filterClass = config.getFilterClass();

    /* XXX:
    if (! config.isAvailable(Alarm.getCurrentTime()))
      throw config.getInitException();
    */
    
    synchronized (config) {
      try {
        Filter filter = _instances.get(filterName);

        if (filter != null)
          return filter;
	
	WebBeansContainer webBeans = WebBeansContainer.create();
      
	_comp = (ComponentImpl) webBeans.createTransient(filterClass);
      
	filter = (Filter) _comp.createNoInit();

	// InjectIntrospector.configure(filter);

        // Initialize bean properties
        ContainerProgram init = config.getInit();
        
        if (init != null)
          init.configure(filter);

	Config.init(filter);

        filter.init(config);
        
        _instances.put(filterName, filter);

        /*
        // If the filter has an MBean, register it
        try {
          String domain = "web-app";
          String jmxName = (domain + ":" +
                            "j2eeType=Filter," +
                            "WebModule=" + getContextPath() + "," +
                            "J2EEApplication=default," + 
                            "J2EEServer=" + getJ2EEServerName() + "," + 
                            "name=" + filterName);
          getJMXServer().conditionalRegisterObject(filter, jmxName);
        } catch (Throwable e) {
          dbg.log(e);
        }
        */
        
        return filter;
      } catch (ServletException e) {
        // XXX: log(e.getMessage(), e);
      
        // XXX: config.setInitException(e);
      
        throw e;
      } catch (Throwable e) {
        // XXX: log(e.getMessage(), e);
      
        throw new ServletException(e);
      }
    }
  }

  public void destroy()
  {
    ArrayList<Filter> filterList = new ArrayList<Filter>();
    
    if (_instances != null) {
      synchronized (_instances) {
        Enumeration<Filter> en = _instances.elements();
        while (en.hasMoreElements()) {
          Filter filter = en.nextElement();

          filterList.add(filter);
        }
      }
    }
    
    for (int i = 0; i < filterList.size(); i++) {
      Filter filter = filterList.get(i);

      try {
	if (_comp != null)
	  _comp.destroy(filter);

        filter.destroy();
      } catch (Throwable e) {
        log.log(Level.WARNING, e.toString(), e);
      }
    }
  }
}
