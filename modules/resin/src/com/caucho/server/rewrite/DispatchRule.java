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

package com.caucho.server.rewrite;

import com.caucho.server.dispatch.*;
import com.caucho.server.webapp.*;
import com.caucho.config.ConfigException;

import javax.servlet.FilterChain;
import java.util.regex.Matcher;

public class DispatchRule
  extends AbstractRuleWithConditions
{
  private String _target;
  private String _absoluteTarget;
  private String _targetHost;

  private boolean _isAbsolute;

  protected DispatchRule(RewriteDispatch rewriteDispatch)
  {
    super(rewriteDispatch);
  }

  public String getTagName()
  {
    return "dispatch";
  }

  public void setAbsoluteTarget(String target)
  {
    _target = target;

    _isAbsolute = true;
  }

  public void setTarget(String target)
  {
    _target = target;
  }

  public void setTargetHost(String target)
  {
    _targetHost = target;
  }

  @Override
  public String rewrite(String uri, Matcher matcher)
  {
    if (_target != null)
      return matcher.replaceAll(_target);
    else
      return uri;
  }

  @Override
  public FilterChain dispatch(String uri,
			      String queryString,
                              FilterChain accept,
                              FilterChainMapper next)
  {
    String uriArg = null;
    
    if (queryString == null)
      uriArg = uri;
    else if (uri.indexOf('?') >= 0)
      uriArg = uri + "&" + queryString;
    else
      uriArg = uri + "?" + queryString;

    /*
    if (_isAbsolute)
      return new DispatchAbsoluteFilterChain(uriArg, WebApp.getCurrent());
    else
    */
    
    if (_target != null)
      return new RewriteDispatchFilterChain(uriArg);
    else
      return accept;
  }

  @Override
  public void init()
    throws ConfigException
  {
    super.init();

    // required(_target, "target");
  }
}
