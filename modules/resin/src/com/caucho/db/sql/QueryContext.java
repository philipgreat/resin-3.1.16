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

package com.caucho.db.sql;

import com.caucho.db.jdbc.GeneratedKeysResultSet;
import com.caucho.db.store.Block;
import com.caucho.db.store.Transaction;
import com.caucho.db.table.TableIterator;
import com.caucho.log.Log;
import com.caucho.util.FreeList;
import com.caucho.util.L10N;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Represents the state of the query at any particular time.
 */
public class QueryContext {
  private static final Logger log = Log.open(QueryContext.class);
  private static final L10N L = new L10N(QueryContext.class);

  private static final long LOCK_TIMEOUT = 120000;

  private static final FreeList<QueryContext> _freeList
    = new FreeList<QueryContext>(64);

  private Transaction _xa;
  private TableIterator []_tableIterators;
  private boolean _isWrite;

  private Data []_parameters = new Data[8];

  private GroupItem _tempGroupItem;
  private GroupItem _groupItem;

  private boolean _isReturnGeneratedKeys;
  private SelectResult _result;
  private GeneratedKeysResultSet _generatedKeys;
  private int _rowUpdateCount;

  private int _limit = -1;

  private Block []_blockLocks;
  private boolean _isLocked;

  private HashMap<GroupItem,GroupItem> _groupMap;

  private byte []_buffer = new byte[256];

  private QueryContext()
  {
    _tempGroupItem = GroupItem.allocate(new boolean[8]);
  }

  /**
   * Returns a new query context.
   */
  public static QueryContext allocate()
  {
    QueryContext queryContext = _freeList.allocate();
    
    if (queryContext == null)
      queryContext = new QueryContext();

    queryContext.clearParameters();

    queryContext._limit = -1;

    return queryContext;
  }

  public void clearParameters()
  {
    for (int i = _parameters.length - 1; i >= 0; i--) {
      if (_parameters[i] == null)
	_parameters[i] = new Data();
      
      _parameters[i].clear();
    }
  }

  /**
   * Initializes the query state.
   */
  public void init(Transaction xa,
		   TableIterator []tableIterators,
		   boolean isReadOnly)
  {
    _xa = xa;
    _isWrite = ! isReadOnly;
    _tableIterators = tableIterators;
    _blockLocks = new Block[_tableIterators.length];
    _isLocked = false;

    _rowUpdateCount = 0;
    _groupItem = _tempGroupItem;
    _groupItem.init(0, null);
  }

  /**
   * Initializes the group.
   */
  public void initGroup(int size, boolean []isGroupByFields)
  {
    _groupItem = _tempGroupItem;
    
    _groupItem.init(size, isGroupByFields);

    if (_groupMap == null)
      _groupMap = new HashMap<GroupItem,GroupItem>();
  }

  /**
   * Selects the actual group item.
   */
  public void selectGroup()
  {
    GroupItem item = _groupMap.get(_groupItem);

    if (item == null) {
      item = _groupItem.allocateCopy();

      _groupMap.put(item, item);
    }

    _groupItem = item;
  }

  /**
   * Returns the group results.
   */
  Iterator<GroupItem> groupResults()
  {
    if (_groupMap == null)
      return com.caucho.util.NullIterator.create();
    
    Iterator<GroupItem> results = _groupMap.values().iterator();
    _groupMap = null;

    return results;
  }
  
  /**
   * Sets the current result.
   */
  void setGroupItem(GroupItem item)
  {
    _groupItem = item;
  }


  /**
   * Returns the table iterator.
   */
  public TableIterator []getTableIterators()
  {
    return _tableIterators;
  }

  /**
   * Sets the transaction.
   */
  public void setTransaction(Transaction xa)
  {
    _xa = xa;
  }

  /**
   * Returns the transaction.
   */
  public Transaction getTransaction()
  {
    return _xa;
  }

  /**
   * Returns the temp buffer.
   */
  public byte []getBuffer()
  {
    return _buffer;
  }

  /**
   * Returns the number of rows updated.
   */
  public int getRowUpdateCount()
  {
    return _rowUpdateCount;
  }

  /**
   * Sets the number of rows updated.
   */
  public void setRowUpdateCount(int count)
  {
    _rowUpdateCount = count;
  }

  /**
   * Set if the query should return the generated keys.
   */
  public boolean isReturnGeneratedKeys()
  {
    return _isReturnGeneratedKeys;
  }

  /**
   * Set if the query should return the generated keys.
   */
  public void setReturnGeneratedKeys(boolean isReturnGeneratedKeys)
  {
    _isReturnGeneratedKeys = isReturnGeneratedKeys;
    
    if (_isReturnGeneratedKeys && _generatedKeys != null)
      _generatedKeys.init();
  }

  /**
   * The max rows returned in a select
   */
  public void setLimit(int limit)
  {
    _limit = limit;
  }

  /**
   * The max rows returned in a select
   */
  public int getLimit()
  {
    return _limit;
  }

  /**
   * Sets the indexed group field.
   */
  public boolean isGroupNull(int index)
  {
    return _groupItem.isNull(index);
  }

  /**
   * Sets the indexed group field.
   */
  public void setGroupString(int index, String value)
  {
    _groupItem.setString(index, value);
  }

  /**
   * Sets the indexed group field.
   */
  public String getGroupString(int index)
  {
    return _groupItem.getString(index);
  }

  /**
   * Sets the indexed group field as a long.
   */
  public void setGroupLong(int index, long value)
  {
    _groupItem.setLong(index, value);
  }

  /**
   * Sets the indexed group field as a long.
   */
  public long getGroupLong(int index)
  {
    return _groupItem.getLong(index);
  }

  /**
   * Sets the indexed group field as a double.
   */
  public void setGroupDouble(int index, double value)
  {
    _groupItem.setDouble(index, value);
  }

  /**
   * Sets the indexed group field as a double.
   */
  public double getGroupDouble(int index)
  {
    return _groupItem.getDouble(index);
  }

  /**
   * Returns the indexed group field.
   */
  public Data getGroupData(int index)
  {
    return _groupItem.getData(index);
  }

  /**
   * Set a null parameter.
   */
  public void setNull(int index)
  {
    _parameters[index].setString(null);
  }

  /**
   * Returns the null parameter.
   */
  public boolean isNull(int index)
  {
    return _parameters[index].isNull();
  }

  /**
   * Set a long parameter.
   */
  public void setLong(int index, long value)
  {
    _parameters[index].setLong(value);
  }

  /**
   * Returns the boolean parameter.
   */
  public int getBoolean(int index)
  {
    return _parameters[index].getBoolean();
  }

  /**
   * Set a boolean parameter.
   */
  public void setBoolean(int index, boolean value)
  {
    _parameters[index].setBoolean(value);
  }

  /**
   * Returns the long parameter.
   */
  public long getLong(int index)
  {
    return _parameters[index].getLong();
  }

  /**
   * Returns the long parameter.
   */
  public long getDate(int index)
  {
    return _parameters[index].getDate();
  }

  /**
   * Set a double parameter.
   */
  public void setDouble(int index, double value)
  {
    _parameters[index].setDouble(value);
  }

  /**
   * Returns the double parameter.
   */
  public double getDouble(int index)
  {
    return _parameters[index].getDouble();
  }

  /**
   * Set a string parameter.
   */
  public void setString(int index, String value)
  {
    _parameters[index].setString(value);
  }

  /**
   * Returns the string parameter.
   */
  public String getString(int index)
  {
    return _parameters[index].getString();
  }

  /**
   * Sets the result set.
   */
  public void setResult(SelectResult result)
  {
    _result = result;
  }

  /**
   * Gets the result set.
   */
  public SelectResult getResult()
  {
    return _result;
  }

  /**
   * Gets the generated keys result set.
   */
  public GeneratedKeysResultSet getGeneratedKeysResultSet()
  {
    if (! _isReturnGeneratedKeys)
      return null;

    if (_generatedKeys == null)
      _generatedKeys = new GeneratedKeysResultSet();
    
    return _generatedKeys;
  }

  /**
   * Lock the blocks.  The blocks are locked in ascending block id
   * order to avoid deadlocks.
   *
   * @param isWrite if true, the block should be locked for writing
   */
  public void lock()
    throws SQLException
  {
    if (_isLocked)
      throw new IllegalStateException(L.l("blocks are already locked"));
    _isLocked = true;
    
    int len = _blockLocks.length;

    for (int i = 0; i < len; i++) {
      Block bestBlock = null;
      long bestId = Long.MAX_VALUE;

      loop:
      for (int j = 0; j < len; j++) {
	TableIterator iter = _tableIterators[j];

	if (iter == null)
	  continue;
	
	Block block = iter.getBlock();

	if (block == null)
	  continue;

	long id = block.getBlockId();
	if (bestId <= id)
	  continue;
	
	for (int k = 0; k < i; k++) {
	  if (_blockLocks[k] == block)
	    continue loop;
	}

	bestId = id;
	bestBlock = block;
      }

      _blockLocks[i] = bestBlock;

      if (bestBlock == null) {
      }
      else if (_isWrite) {
	_xa.lockReadAndWrite(bestBlock.getLock());
      }
      else {
	_xa.lockRead(bestBlock.getLock());
      }
    }
  }

  /**
   * Unlock the blocks.  The blocks are unlocked in descending order.
   *
   * @param isWrite if true, the block should be unlocked for writing
   */
  public void unlock()
    throws SQLException
  {
    if (! _isLocked)
      return;
    
    _isLocked = false;
    
    int len = _blockLocks.length;

    // need to unlock first since the writeData/commit will wait for
    // write locks to clear before committing
    for (int i = len - 1; i >= 0; i--) {
      Block block = _blockLocks[i];

      if (block == null) {
      }
      else if (_isWrite) {
	_xa.unlockReadAndWrite(block.getLock());
      }
      else {
	_xa.unlockRead(block.getLock());
      }
    }

    try {
      _xa.writeData();
    } finally {
      for (int i = len - 1; i >= 0; i--) {
	Block block = _blockLocks[i];
	_blockLocks[i] = null;

	if (block == null) {
	}
	else if (_isWrite) {
	  try {
	    block.commit();
	  } catch (IOException e) {
	    log.log(Level.FINE, e.toString(), e);
	  }
	}
      }
    }
  }

  /**
   * Returns a new query context.
   */
  public static void free(QueryContext queryContext)
  {
    queryContext._groupMap = null;

    _freeList.free(queryContext);
  }
}
