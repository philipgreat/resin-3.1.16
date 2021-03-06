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

package com.caucho.hmtp.disco;

import com.caucho.hmtp.disco.DiscoIdentity;
import com.caucho.hmtp.disco.DiscoFeature;
import java.util.*;

/**
 * service discovery query
 *
 * http://jabber.org/protocol/disco#info
 *
 * <code><pre>
 * element query {
 *   attribute node?,
 *   identity*,
 *   feature*
 * }
 *
 * element identity {
 *    attribute category,
 *    attribute name?,
 *    attribute type
 * }
 *
 * element feature {
 *    attribute var
 * }
 * </pre></code>
 *
 * Well known nodes:
 * <ul>
 * <li>http://jabber.org/protocol/amp
 * <li>http://jabber.org/protocol/commands
 * <li>http://jabber.org/protocol/muc#rooms
 * <li>http://jabber.org/protocol/muc#traffic
 * <li>pubsub/nodes
 * <li>x-roomuser-item
 * </ul>
 */
public class DiscoInfoQuery implements java.io.Serializable {
  public static final String FEATURE
    = "http://jabber.org/protocol/disco#info";
  
  private String _node;
  
  private DiscoIdentity []_identity;
  private DiscoFeature []_features;
  
  public DiscoInfoQuery()
  {
  }
  
  public DiscoInfoQuery(String node)
  {
    _node = node;
  }
  
  public DiscoInfoQuery(DiscoIdentity []identity,
			DiscoFeature []features)
  {
    _identity = identity;
    _features = features;
  }
  
  public DiscoInfoQuery(String node,
			DiscoIdentity []identity,
			DiscoFeature []features)
  {
    _node = node;
    
    _identity = identity;
    _features = features;
  }
  
  @Override
  public String toString()
  {
    StringBuilder sb = new StringBuilder();
    sb.append(getClass().getSimpleName());
    sb.append("[");

    if (_node != null) {
      sb.append("node=");
      sb.append(_node);
      sb.append(",");
    }

    sb.append("id=[");
    
    if (_identity != null) {
      for (int i = 0; i < _identity.length; i++) {
	if (i != 0)
	  sb.append(",");
	sb.append(_identity[i]);
      }
    }
    sb.append("],features=[");
    
    if (_features != null) {
      for (int i = 0; i < _features.length; i++) {
	if (i != 0)
	  sb.append(",");
	sb.append(_features[i].getVar());
      }
    }
    sb.append("]]");
    
    return sb.toString();
  }
}
