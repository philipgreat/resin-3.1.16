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

package com.caucho.quercus.env;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Iterator;

/**
 * Represents a PHP array value.
 */
public class ArrayValueWrapper extends ArrayValue {
  private ArrayValue _array;

  protected ArrayValueWrapper(ArrayValue array)
  {
    _array = array;
  }

  /**
   * Returns the wrapped array.
   */
  @Override
  public ArrayValue getArray()
  {
    return _array;
  }
  
  /**
   * Copy for assignment.
   */
  @Override
  public Value copy()
  {
    return _array.copy();
  }
  
  /**
   * Copy for serialization
   */
  @Override
  public Value copy(Env env, IdentityHashMap<Value,Value> map)
  {
    return _array.copy(env, map);
  }

  /**
   * Returns the size.
   */
  @Override
  public int getSize()
  {
    return _array.getSize();
  }

  /**
   * Clears the array
   */
  @Override
  public void clear()
  {
    _array.clear();
  }
  
  /**
   * Adds a new value.
   */
  @Override
  public Value put(Value key, Value value)
  {
    return _array.put(key, value);
  }

  /**
   * Add
   */
  @Override
  public Value put(Value value)
  {
    return _array.put(value);
  }

  /**
   * Add to front.
   */
  @Override
  public ArrayValue unshift(Value value)
  {
    return _array.unshift(value);
  }

  /**
   * Splices values
   */
  @Override
  public ArrayValue splice(int start, int end, ArrayValue replace)
  {
    return _array.splice(start, end, replace);
  }

  /**
   * Returns the value as an argument which may be a reference.
   */
  @Override
  public Value getArg(Value index)
  {
    return _array.getArg(index);
  }

  /**
   * Sets the array ref.
   */
  @Override
  public Value putRef()
  {
    return _array.putRef();
  }

  /**
   * Creatse a tail index.
   */
  @Override
  public Value createTailKey()
  {
    return _array.createTailKey();
  }

  /**
   * Gets a new value.
   */
  @Override
  public Value get(Value key)
  {
    return _array.get(key);
  }

  /**
   * Removes a value.
   */
  @Override
  public Value remove(Value key)
  {
    return _array.remove(key);
  }

  /**
   * Returns the array ref.
   */
  @Override
  public Var getRef(Value index)
  {
    return _array.getRef(index);
  }
  
  /**
   * Pops the top value.
   */
  @Override
  public Value pop()
  {
    return _array.pop();
  }

  /**
   * Shuffles the array
   */
  @Override
  public void shuffle()
  {
    _array.shuffle();
  }

  /**
   * Returns the head.
   */
  @Override
  public Entry getHead()
  {
    return _array.getHead();
  }

  /**
   * Returns the tail.
   */
  @Override
  protected Entry getTail()
  {
    return _array.getTail();
  }
  
  /**
   * Returns the current value.
   */
  @Override
  public Value current()
  {
    return _array.current();
  }

  /**
   * Returns the current key
   */
  @Override
  public Value key()
  {
    return _array.key();
  }

  /**
   * Returns true if there are more elements.
   */
  @Override
  public boolean hasCurrent()
  {
    return _array.hasCurrent();
  }

  /**
   * Returns the next value.
   */
  @Override
  public Value next()
  {
    return _array.next();
  }

  /**
   * Returns the previous value.
   */
  @Override
  public Value prev()
  {
    return _array.prev();
  }

  /**
   * The each iterator
   */
  @Override
  public Value each()
  {
    return _array.each();
  }

  /**
   * Returns the first value.
   */
  @Override
  public Value reset()
  {
    return _array.reset();
  }

  /**
   * Returns the last value.
   */
  @Override
  public Value end()
  {
    return _array.end();
  }
  
  /**
   * Returns the corresponding valeu if this array contains the given key
   * 
   * @param key  the key to search for in the array
   * 
   * @return the value if it is found in the array, NULL otherwise
   * 
   * @throws NullPointerException
   */
  @Override
  public Value containsKey(Value key)
  {
    return _array.containsKey(key);
  }

  @Override
  public Value add(Value rValue)
  {
    return _array.add(rValue);
  }

  @Override
  public Iterator<Map.Entry<Value, Value>> getIterator(Env env)
  {
    return _array.getIterator(env);
  }

  @Override
  public Iterator<Value> getKeyIterator(Env env)
  {
    return _array.getKeyIterator(env);
  }

  @Override
  public Iterator<Value> getValueIterator(Env env)
  {
    return _array.getValueIterator(env);
  }
}

