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

package com.caucho.server.e_app;

import com.caucho.util.L10N;

import javax.annotation.PostConstruct;

/**
 * Configuration for the application.xml file.
 */
public class AppClientBinding {
  private static final L10N L = new L10N(AppClientBinding.class);

  private EntAppClient _appClient;
  
  AppClientBinding(EntAppClient appClient)
  {
    _appClient = appClient;
  }

  /**
   * Adds an ejb-link
   */
  public void addEjbLink(EjbLink link)
    throws Exception
  {
  }

  class EjbLink {
    public String _ejbName;
    public String _jndiName;

    public void setEjbName(String ejbName)
    {
      _ejbName = ejbName;
    }

    public void setJndiName(String jndiName)
    {
      _jndiName = jndiName;
    }

    @PostConstruct
    public void init()
    {
      System.out.println("ENB: " + _ejbName + " " + _jndiName);
    }
  }
}
