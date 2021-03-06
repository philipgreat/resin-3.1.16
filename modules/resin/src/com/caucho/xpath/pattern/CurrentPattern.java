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

package com.caucho.xpath.pattern;

import com.caucho.xpath.Env;
import com.caucho.xpath.ExprEnvironment;
import com.caucho.xpath.XPathException;

import org.w3c.dom.Node;

/**
 * Matches the current node.
 */
public class CurrentPattern extends Axis {
  public CurrentPattern()
  {
    super(null);
  }

  /**
   * Matches the current node
   *
   * @param node the starting node
   * @param env the xpath environment
   *
   * @return true if the node is the current node.
   */
  public boolean match(Node node, ExprEnvironment env)
  {
    return (node == env.getCurrentNode());
  }
  
  /**
   * Returns true if the pattern selects a single node
   */
  boolean isSingleSelect()
  {
    return true;
  }

  /**
   * Creates a new node iterator.
   *
   * @param node the starting node
   * @param env the xpath environment
   * @param match the axis match pattern
   *
   * @return the node iterator
   */
  public NodeIterator createNodeIterator(Node node, ExprEnvironment env,
                                         AbstractPattern match)
    throws XPathException
  {
    Node current = env.getCurrentNode();
    
    if (match == null || match.match(current, env))
      return new SingleNodeIterator(env, current);
    else
      return null;
  }

  /**
   * Returns the first node in the selection order.
   *
   * @param node the current node
   *
   * @return the first node
   */
  public Node firstNode(Node node, ExprEnvironment env)
  {
    return env.getCurrentNode();
  }

  /**
   * Returns the next node in the selection order.
   *
   * @param node the current node
   * @param last the last node
   *
   * @return the next node
   */
  public Node nextNode(Node node, Node last)
  {
    return null;
  }
  
  /**
   * There is only a single node in the current
   */
  public int position(Node node, Env env, AbstractPattern pattern)
  {
    return 1;
  }
  /**
   * There is only a single node in the current
   */
  public int count(Node node, Env env, Node context)
  {
    return 1;
  }

  public String toString()
  {
    return "current()";
  }
}
