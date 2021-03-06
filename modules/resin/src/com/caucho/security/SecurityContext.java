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

package com.caucho.security;

import com.caucho.log.Log;
import com.caucho.util.L10N;

import java.security.*;
import java.util.logging.Logger;

/**
 * Defines a proxy for the current security context.
 */
public class SecurityContext {
  static final Logger log = Logger.getLogger(SecurityContext.class.getName());
  static final L10N L = new L10N(SecurityContext.class);

  /**
   * Mapping from threads to providers.
   */
  private static ThreadLocal<SecurityContextProvider> _providers
    = new ThreadLocal<SecurityContextProvider>();

  /**
   * The context cannot be instantiated.
   */
  private SecurityContext()
  {
  }

  /**
   * Returns the principal for this security context.
   *
   * @return the principal or null of no provider for the thread.
   */
  public static Principal getUserPrincipal()
    throws SecurityContextException
  {
    SecurityContextProvider provider = getProvider();

    if (provider != null)
      return provider.getUserPrincipal();
    else
      return null;
  }

  /**
   * Returns true if the user principal is in the specified role.
   *
   * @param roleName the name of the role to test.
   */
  public static boolean isUserInRole(String roleName)
  {
    SecurityContextProvider provider = getProvider();

    if (provider != null)
      return provider.isUserInRole(roleName);
    else
      return false;
  }

  /**
   * Returns true if the user principal is in the specified role.
   *
   * @param roleSet a set of roles to test.
   */
  public static boolean isUserInRole(String []roleSet)
  {
    SecurityContextProvider provider = getProvider();

    if (provider != null && roleSet != null) {
      for (int i = 0; i < roleSet.length; i++) {
	if (provider.isUserInRole(roleSet[i]))
	  return true;
      }
    }

    return false;
  }

  /**
   * Returns true if the user principal is in the specified role.
   *
   * @param roleSet a set of roles to test.
   */
  public static void checkUserInRole(String []roleSet)
  {
    SecurityContextProvider provider = getProvider();

    if (provider != null && roleSet != null) {
      for (int i = 0; i < roleSet.length; i++) {
	if (provider.isUserInRole(roleSet[i]))
	  return;
      }

      throw new AccessControlException(L.l("permission denied"));
    }
  }

  /**
   * Returns true if the user principal is in the specified role.
   *
   * @param roleSet a set of roles to test.
   */
  public static String runAs(String role)
  {
    SecurityContextProvider provider = getProvider();

    if (provider != null)
      return provider.runAs(role);
    else
      return null;
  }

  /**
   * Returns true if the context is secure (SSL).
   */
  public static boolean isTransportSecure()
    throws SecurityContextException
  {
    SecurityContextProvider provider = getProvider();

    if (provider != null)
      return provider.isTransportSecure();
    else
      return false;
  }

  /**
   * Logs the principal out.
   */
  public static void logout()
    throws SecurityContextException
  {
    SecurityContextProvider provider = getProvider();

    if (provider != null)
      provider.logout();
  }

  /**
   * Gets the provider for the current thread.
   *
   * @return the provider for the thread
   */
  public static SecurityContextProvider getProvider()
  {
    return _providers.get();
  }

  /**
   * Sets the provider for the current thread.
   *
   * @param provider the new provider
   */
  public static SecurityContextProvider setProvider(SecurityContextProvider provider)
  {
    SecurityContextProvider oldProvider = _providers.get();
    
    _providers.set(provider);

    return oldProvider;
  }
}
