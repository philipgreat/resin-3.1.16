/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001-2004 Caucho Technology, Inc.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:
 *       "This product includes software developed by the
 *        Caucho Technology (http://www.caucho.com/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "Hessian", "Resin", and "Caucho" must not be used to
 *    endorse or promote products derived from this software without prior
 *    written permission. For written permission, please contact
 *    info@caucho.com.
 *
 * 5. Products derived from this software may not be called "Resin"
 *    nor may "Resin" appear in their names without prior written
 *    permission of Caucho Technology.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL CAUCHO TECHNOLOGY OR ITS CONTRIBUTORS
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY,
 * OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT
 * OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
 * BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
 * IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * @author Sam
 */

package com.caucho.portal.generic;

import javax.portlet.PortletRequest;
import java.io.IOException;
import java.util.Map;

/**
 * Factory that returns a Map<String, String[]>
 * that is the store for portlet preferences.
 * The factory is used once for each namespace if needed.
 */
public interface PreferencesStore {
  /**
   * Return a map of preferences.  Implementing classes use the information
   * available from <i>request</i> to determine the identity
   * of the user.
   *
   * @return a Map of preference names to values, or null if no preferences
   * are available. 
   */
  public Map<String, String[]> getPreferencesMap( PortletRequest request,
                                                  String namespace )
    throws IOException;

  /**
   * Called when a Map previously returned by getPreferencesMap()
   * is no longer needed. 
   *
   * A call to finish() is not guaranteed to occur for every Map
   * returned by getPreferencesMap().  If an error occurs when processing a
   * request, the portal may not have an opportunity to call finish().
   */
  public void finish( Map<String, String[]> preferencesMap )
    throws IOException;
}

