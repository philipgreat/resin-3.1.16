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

import com.caucho.db.Database;
import com.caucho.db.store.Transaction;
import com.caucho.db.table.Column;
import com.caucho.db.table.Table;
import com.caucho.db.table.TableIterator;
import com.caucho.log.Log;
import com.caucho.sql.SQLExceptionWrapper;
import com.caucho.util.CharBuffer;
import com.caucho.util.L10N;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

abstract public class Query {
  private static final Logger log = Log.open(Query.class);
  private static final L10N L = new L10N(Query.class);

  private Database _db;

  private String _sql;

  private FromItem []_fromItems;
  private ParamExpr []_params;

  private boolean _isGroup;
  private int _dataFieldCount;

  private Query _parent;
  private SubSelectExpr _parentSubSelect;

  private Expr []_whereExprs;
  protected Expr _whereExpr;

  private RowIterateExpr []_indexExprs;

  private ArrayList<SubSelectParamExpr> _paramExprs
    = new ArrayList<SubSelectParamExpr>();

  protected Query(Database db, String sql)
  {
    _db = db;
    _sql = sql;
  }

  protected Query(Database db, String sql, FromItem []fromItems)
  {
    _db = db;
    _sql = sql;
    _fromItems = fromItems;
  }

  /**
   * Returns the owning database.
   */
  public Database getDatabase()
  {
    return _db;
  }

  /**
   * Sets the parent query
   */
  public void setParent(Query query)
  {
    _parent = query;
  }

  /**
   * Gets the parent query
   */
  public Query getParent()
  {
    return _parent;
  }

  /**
   * Sets the parent sub-select.
   */
  public void setSubSelect(SubSelectExpr subSelect)
  {
    _parentSubSelect = subSelect;
  }

  /**
   * Gets the parent sub-select.
   */
  public SubSelectExpr getSubSelect()
  {
    return _parentSubSelect;
  }

  /**
   * Returns the number of temporary data fields.
   */
  public int getDataFields()
  {
    return _dataFieldCount;
  }

  /**
   * Sets the number of temporary data fields.
   */
  public void setDataFields(int fieldCount)
  {
    _dataFieldCount = fieldCount;
  }

  /**
   * Sets the maximum entires
   */
  public void setLimit(int limit)
  {
  }
  

  /**
   * Returns any from items.
   */
  public FromItem []getFromItems()
  {
    return _fromItems;
  }

  /**
   * Sets from items.
   */
  protected void setFromItems(FromItem []fromItems)
  {
    _fromItems = fromItems;
  }

  /**
   * Sets from items.
   */
  protected void setFromItems(ArrayList<FromItem> fromItems)
  {
    _fromItems = new FromItem [fromItems.size()];
    fromItems.toArray(_fromItems);
  }

  /**
   * Sets the where expr.
   */
  public void setWhereExpr(Expr expr)
  {
    _whereExpr = expr;
  }

  /**
   * Returns the where exprs
   */
  public Expr []getWhereExprs()
  {
    return _whereExprs;
  }

  /**
   * Sets the where exprs.
   */
  protected void setWhereExprs(Expr []whereExprs)
  {
    _whereExprs = whereExprs;
  }

  /**
   * Sets the params.
   */
  public void setParams(ParamExpr []params)
  {
    _params = params;
  }

  /**
   * Returns the param exprs.
   */
  public ArrayList<SubSelectParamExpr> getParamExprs()
  {
    return _paramExprs;
  }

  /**
   * Returns the SQL.
   */
  String getSQL()
  {
    return _sql;
  }

  /**
   * Returns true for select queries.
   */
  public boolean isSelect()
  {
    return false;
  }

  public boolean isReadOnly()
  {
    return true;
  }

  /**
   * Sets the current number of group fields.
   */
  public void setGroup(boolean isGroup)
  {
    _isGroup = isGroup;
  }

  /**
   * Sets true for group operations
   */
  public boolean isGroup()
  {
    return _isGroup;
  }

  /**
   * Binds the query.
   */
  protected void bind()
    throws SQLException
  {
    if (_whereExpr != null) {
      generateWhere(_whereExpr);

      for (int i = 0; i < _whereExprs.length; i++) {
	Expr expr = _whereExprs[i];

	if (expr != null)
	  _whereExprs[i] = expr.bind(this);
      }
    }
    else if (_fromItems != null) {
      _whereExprs = new Expr[_fromItems.length + 1];
      _indexExprs = new RowIterateExpr[_fromItems.length];
    }
    else {
      _whereExprs = new Expr[2];
      _indexExprs = new RowIterateExpr[1];
    }

    for (int i = 0; i < _indexExprs.length; i++) {
      Expr expr = _indexExprs[i];

      if (expr == null)
	_indexExprs[i] = new RowIterateExpr();
      else
	_indexExprs[i] = (RowIterateExpr) _indexExprs[i].bind(this);
    }

    for (int i = 0; i < _paramExprs.size(); i++) {
      SubSelectParamExpr expr = _paramExprs.get(i);

      expr = (SubSelectParamExpr) expr.bind(_parent);
      _paramExprs.set(i, expr);
    }
  }

  /**
   * Optimize the where and order the from items.
   */
  protected void generateWhere(Expr whereExpr)
    throws SQLException
  {
    ArrayList<Expr> andProduct = new ArrayList<Expr>();

    whereExpr.splitAnd(andProduct);

    FromItem []fromItems = getFromItems();

    Expr []whereExprs = new Expr[fromItems.length + 1];
    RowIterateExpr []indexExprs = new RowIterateExpr[fromItems.length];

    _whereExprs = whereExprs;
    _indexExprs = indexExprs;

    ArrayList<FromItem> costItems = new ArrayList<FromItem>();
    orderFromItems(costItems, andProduct);

    costItems.clear();
    for (int i = fromItems.length; i >= 0; i--) {
      if (i < fromItems.length)
	costItems.add(fromItems[i]);

      AndExpr subWhereExpr = null;
      boolean hasExpr = false;

      int bestIndex = -1;
      long bestCost;

      do {
	bestCost = Long.MAX_VALUE;

	for (int j = andProduct.size() - 1; j >= 0; j--) {
	  Expr subExpr = andProduct.get(j);

	  long cost = subExpr.cost(costItems);

	  if (Integer.MAX_VALUE <= cost && i != 0) {
	  }
	  else if (cost < bestCost) {
	    bestCost = cost;
	    bestIndex = j;
	  }
	}

	if (bestCost < Long.MAX_VALUE) {
	  Expr expr = andProduct.remove(bestIndex);
	  RowIterateExpr indexExpr = null;

	  if (i < fromItems.length)
	    indexExpr = expr.getIndexExpr(fromItems[i]);

	  if (indexExpr != null && indexExprs[i] == null) {
	    indexExprs[i] = indexExpr;
	  }
	  else {
	    // XXX: check if really need to add
	    if (subWhereExpr == null)
	      subWhereExpr = new AndExpr();

	    subWhereExpr.add(expr);
	  }
	}
      } while (bestCost < Long.MAX_VALUE);

      if (subWhereExpr != null)
	whereExprs[i] = subWhereExpr.getSingleExpr();
    }

    for (int i = 0; i < whereExprs.length; i++) {
      Expr expr = whereExprs[i];
      /*
      if (expr != null)
	expr = expr.bind(this);
      */

      whereExprs[i] = expr;
    }

    _whereExprs = whereExprs;

    if (log.isLoggable(Level.FINE)) {
      log.fine("where-" + (whereExprs.length - 1) +  ": static " + whereExprs[whereExprs.length - 1]);

      for (int i = whereExprs.length - 2; i >= 0; i--) {
	if (_indexExprs[i] != null)
	  log.fine("index-" + i + ": " + _fromItems[i] + " " + _indexExprs[i]);

	log.fine("where-" + i + ": " + _fromItems[i] + " " + whereExprs[i]);
      }
    }
  }

  private void orderFromItems(ArrayList<FromItem> costItems,
			      ArrayList<Expr> topAndProduct)
  {
    FromItem []fromItems = getFromItems();

    ArrayList<Expr> andProduct = new ArrayList<Expr>(topAndProduct);

    for (int i = fromItems.length - 1; i >= 0; i--) {
      costItems.clear();

      for (int j = i + 1; j < fromItems.length; j++)
	costItems.add(fromItems[j]);

      int bestIndex = i;
      long bestCost = Expr.COST_INVALID;

      loop:
      for (int j = 0; j <= i; j++) {
	FromItem item = fromItems[j];

	costItems.add(item);

	for (int k = 0; k < fromItems.length; k++) {
	  if (! fromItems[k].isValid(costItems)) {
	    costItems.remove(costItems.size() - 1);
	    continue loop;
	  }
	}

	long cost = Long.MAX_VALUE;
	for (int k = 0; k < andProduct.size(); k++) {
	  Expr expr = andProduct.get(k);

	  long subCost = expr.cost(costItems);

	  if (Expr.COST_INVALID <= subCost) {
	    costItems.remove(costItems.size() - 1);
	    continue loop;
	  }

	  if (subCost < cost)
	    cost = subCost;
	}

	costItems.remove(costItems.size() - 1);

	if (cost < bestCost) {
	  bestCost = cost;
	  bestIndex = j;
	}
      }

      FromItem tempItem = fromItems[i];
      fromItems[i] = fromItems[bestIndex];
      fromItems[bestIndex] = tempItem;

      costItems.add(fromItems[i]);
      for (int k = andProduct.size() - 1; k >= 0; k--) {
	Expr expr = andProduct.get(k);

	long subCost = expr.cost(costItems);

	if (subCost < Expr.COST_NO_TABLE) {
	  andProduct.remove(k);
	}
      }
    }
  }

  private String logWhere()
  {
    CharBuffer cb = CharBuffer.allocate();

    cb.append("[");
    for (int i = 0; i < _whereExprs.length; i++) {
      if (i != 0)
	cb.append(", ");

      if (_whereExprs[i] != null)
	cb.append(_whereExprs[i]);
    }

    cb.append("]");

    return cb.close();
  }

  /**
   * Returns a bound expression for the specified table.column.
   */
  protected Expr bind(String tableName, String columnName)
    throws SQLException
  {
    FromItem []fromItems = getFromItems();

    if (tableName == null) {
      if ("resin_oid".equals(columnName))
	return new OidExpr(fromItems[0].getTable(), 0);

      for (int i = 0; i < fromItems.length; i++) {
	Table table = fromItems[i].getTable();

	int columnIndex = table.getColumnIndex(columnName);

	if (columnIndex >= 0) {
	  Column column = table.getColumn(columnName);

	  return new IdExpr(fromItems[i], column);
	}
      }

      Expr expr = bindParent(tableName, columnName);
      if (expr != null) {
	return expr;
      }

      throw new SQLException(L.l("`{0}' is an unknown column.", columnName));
    }
    else {
      for (int i = 0; i < fromItems.length; i++) {
	if (tableName.equals(fromItems[i].getName())) {
	  Table table = fromItems[i].getTable();

	  if ("resin_oid".equals(columnName))
	    return new OidExpr(table, i);

	  int columnIndex = table.getColumnIndex(columnName);

	  if (columnIndex < 0) {
	    Expr expr = bindParent(tableName, columnName);
	    if (expr != null)
	      return expr;

	    throw new SQLException(L.l("`{0}' is an unknown column in \n  {1}.",
				       columnName, _sql));
	  }

	  Column column = table.getColumn(columnName);

	  return new IdExpr(fromItems[i], column);
	}
      }

      Expr expr = bindParent(tableName, columnName);
      if (expr != null)
	return expr;


      throw new SQLException(L.l("`{0}' is an unknown table.\n{1}",
				 tableName, getSQL()));
    }
  }

  /**
   * Binds as a subselect.
   */
  private Expr bindParent(String tableName, String columnName)
    throws SQLException
  {
    if (_parent != null) {
      Expr expr = _parent.bind(tableName, columnName);

      if (expr != null) {
	SubSelectParamExpr paramExpr;

	paramExpr = new SubSelectParamExpr(this, expr, _paramExprs.size());
	_paramExprs.add(paramExpr);

	return paramExpr;
      }
    }

    return null;
  }

  /**
   * Clears the paramters.
   */
  public void clearParameters()
  {
    for (int i = 0; i < _params.length; i++)
      _params[i].clear();
  }

  /**
   * Sets the indexed parameter as a boolean.
   */
  public void setBoolean(int index, boolean value)
  {
    _params[index - 1].setBoolean(value);
  }

  /**
   * Sets the indexed parameter as a string.
   */
  public void setString(int index, String value)
  {
    _params[index - 1].setString(value);
  }

  /**
   * Sets the indexed parameter as a long.
   */
  public void setLong(int index, long value)
  {
    _params[index - 1].setLong(value);
  }

  /**
   * Sets the indexed parameter as a double.
   */
  public void setDouble(int index, double value)
  {
    _params[index - 1].setDouble(value);
  }

  /**
   * Sets the indexed parameter as a date value.
   */
  public void setDate(int index, long value)
  {
    _params[index - 1].setDate(value);
  }

  /**
   * Sets the indexed parameter as a binary stream
   */
  public void setBinaryStream(int index, InputStream is, int length)
  {
    _params[index - 1].setBinaryStream(is, length);
  }

  /**
   * Executes the query.
   */
  abstract public void execute(QueryContext queryCtx, Transaction xa)
    throws SQLException;

  /**
   * Starts the query.
   */
  protected boolean start(TableIterator []rows, int rowLength,
			  QueryContext queryContext, Transaction xa)
    throws SQLException
  {
    try {
      Expr []whereExprs = _whereExprs;

      // Test the constant expression
      if (whereExprs == null || whereExprs[rowLength] == null) {
      }
      else if (! whereExprs[rowLength].isSelect(queryContext)) {
	return false;
      }

      if (rowLength == 0)
	return true;

      for (int i = rowLength - 1; i >= 0; i--) {
	TableIterator row = rows[i];
	RowIterateExpr iterExpr = _indexExprs[i];

	if (! iterExpr.init(queryContext, row)) {
	  return false;
	}

	// XXX: check to make sure others actually lock this properly
	//if (! xa.isAutoCommit())
	//  xa.lockRead(row.getTable().getLock());
      }

      return (initBlockRow(rowLength - 1, rows, queryContext)
	      || nextBlock(rowLength - 1, rows, rowLength, queryContext));
    } catch (IOException e) {
      throw new SQLExceptionWrapper(e);
    }
  }

  /**
   * Returns the next tuple from the query.
   */
  protected boolean nextTuple(TableIterator []rows, int rowLength,
			      QueryContext queryContext, Transaction xa)
    throws SQLException
  {
    try {
      if (rowLength == 0)
	return false;

      RowIterateExpr []indexExprs = _indexExprs;
      Expr []whereExprs = _whereExprs;

      for (int i = 0; i < rowLength; i++) {
	TableIterator table = rows[i];
	RowIterateExpr indexExpr = indexExprs[i];

	Expr whereExpr = whereExprs == null ? null : whereExprs[i];

	while (indexExpr.nextRow(queryContext, table)) {
	  if (whereExpr == null || whereExpr.isSelect(queryContext)) {
	    if (i == 0 || initBlockRow(i - 1, rows, queryContext)) {
	      return true;
	    }
	  }
	}
      }

      return nextBlock(rowLength - 1, rows, rowLength, queryContext);
    } catch (IOException e) {
      throw new SQLExceptionWrapper(e);
    }
  }

  /**
   * Initialize this row and all previous rows within this block group.
   */
  private boolean nextBlock(int i,
			    TableIterator []rows,
			    int rowLength,
			    QueryContext queryContext)
    throws IOException, SQLException
  {
    TableIterator rowIter = rows[i];
    RowIterateExpr iterExpr = _indexExprs[i];

    while (true) {
      if (i > 0 && nextBlock(i - 1, rows, rowLength, queryContext)) {
	return true;
      }

      if (! iterExpr.nextBlock(queryContext, rowIter)) {
	return false;
      }

      if (! iterExpr.allowChildRowShift(queryContext, rows[i]))
	return false;

      for (int j = i - 1; j >= 0; j--) {
	if (! iterExpr.init(queryContext, rows[j]))
	  return false;
      }

      if (initBlockRow(rowLength - 1, rows, queryContext))
	return true;
    }
  }

  /**
   * Initialize this row and all previous rows within this block group.
   */
  private boolean initBlockRow(int i,
			       TableIterator []rows,
			       QueryContext queryContext)
    throws IOException, SQLException
  {
    RowIterateExpr iterExpr = _indexExprs[i];

    Expr []whereExprs = _whereExprs;
    Expr expr = whereExprs == null ? null : whereExprs[i];

    TableIterator rowIter = rows[i];

    if (! iterExpr.initRow(queryContext, rowIter)) {
      return false;
    }

    while (expr != null && ! expr.isSelect(queryContext)
	   || i > 0 && ! initBlockRow(i - 1, rows, queryContext)) {
      if (! iterExpr.nextRow(queryContext, rowIter)) {
	return false;
      }
    }

    return true;
  }

  /**
   * Frees any blocks for the rows.
   */
  protected void freeRows(TableIterator []rows, int rowLength)
  {
    for (rowLength--; rowLength >= 0; rowLength--) {
      if (rows[rowLength] != null)
	rows[rowLength].free();
    }
  }
}
