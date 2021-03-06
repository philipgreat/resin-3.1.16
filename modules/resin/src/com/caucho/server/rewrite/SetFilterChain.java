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
 * @author Sam
 */

package com.caucho.server.rewrite;

import com.caucho.server.connection.*;
import com.caucho.filters.*;
import com.caucho.server.dispatch.*;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.IOException;

public class SetFilterChain
  extends ContinueMapFilterChain
{
  private final Boolean _requestSecure;
  private final String _requestCharacterEncoding;
  private final String _responseContentType;
  private String _responseCharacterEncoding;

  public SetFilterChain(String uri,
			String queryString,
			FilterChain accept,
			FilterChainMapper nextFilterChainMapper,
			String requestCharacterEncoding,
			Boolean requestSecure,
			String responseCharacterEncoding,
			String responseContentType)
  {
    super(uri, queryString, accept, nextFilterChainMapper);

    _requestCharacterEncoding = requestCharacterEncoding;
    _requestSecure = requestSecure;
    _responseCharacterEncoding = responseCharacterEncoding;
    _responseContentType = responseContentType;
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response)
    throws ServletException, IOException
  {
    if (_requestCharacterEncoding != null)
      request.setCharacterEncoding(_requestCharacterEncoding);

    CauchoRequest oldRequest = null;
    AbstractHttpResponse cauchoResponse = null;
    if (_requestSecure != null) {
      CauchoRequest cauchoRequest
	= new SecureServletRequestWrapper((HttpServletRequest) request);

      if (response instanceof AbstractHttpResponse
	  && cauchoRequest.getWebApp() != null) {
	cauchoResponse = (AbstractHttpResponse) response;

	oldRequest = cauchoResponse.getRequest();
	cauchoResponse.setRequest(cauchoRequest);
      }

      request = cauchoRequest;

      request.setCharacterEncoding(_requestCharacterEncoding);
    }

    if (_responseCharacterEncoding != null)
      response.setCharacterEncoding(_responseCharacterEncoding);

    if (_responseContentType != null)
      response.setContentType(_responseContentType);

    try {
      super.doFilter(request, response);
    } finally {
      if (cauchoResponse != null && request != oldRequest)
	cauchoResponse.setRequest(oldRequest);
    }
  }

  private class SecureServletRequestWrapper
    extends RequestAdapter
  {
    public SecureServletRequestWrapper(HttpServletRequest request)
    {
      setRequest(request);

      if (request instanceof CauchoRequest)
	setWebApp(((CauchoRequest) request).getWebApp());
    }

    public boolean isSecure()
    {
      return _requestSecure;
    }

    /**
     * Returns the request's scheme.
     */
    public String getScheme()
    {
      return isSecure() ? "https" : "http";
    }
  }
}
