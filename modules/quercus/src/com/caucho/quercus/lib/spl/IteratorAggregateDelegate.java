/*
 * Copyright (c) 1998-2007 Caucho Technology -- all rights reserved
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

package com.caucho.quercus.lib.spl;

import com.caucho.quercus.env.TraversableDelegate;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.ObjectValue;
import com.caucho.quercus.env.StringBuilderValue;
import com.caucho.quercus.env.Value;

import java.util.Iterator;
import java.util.Map;

/**
 * A delegate that intercepts requests for iterator's and delegates
 * them to the iteerator returned by {@link IteratorAggregate@getIterator()}
 */
public class IteratorAggregateDelegate
  implements TraversableDelegate
{
  private static final StringBuilderValue GET_ITERATOR
    = new StringBuilderValue("getIterator");
  
  private final IteratorDelegate _iteratorDelegate = new IteratorDelegate();

  public Iterator<Map.Entry<Value, Value>>
    getIterator(Env env, ObjectValue qThis)
  {
    return _iteratorDelegate.getIterator(env, getTarget(env, qThis));
  }

  public Iterator<Value> getKeyIterator(Env env, ObjectValue qThis)
  {
    return _iteratorDelegate.getKeyIterator(env, getTarget(env, qThis));
  }

  public Iterator<Value> getValueIterator(Env env, ObjectValue qThis)
  {
    return _iteratorDelegate.getValueIterator(env, getTarget(env, qThis));
  }

  private ObjectValue getTarget(Env env, ObjectValue qThis)
  {
    Value iter = qThis.callMethod(env, GET_ITERATOR);

    return (ObjectValue) iter;
  }
}
