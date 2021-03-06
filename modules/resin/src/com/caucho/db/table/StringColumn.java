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

package com.caucho.db.table;

import com.caucho.db.index.BTree;
import com.caucho.db.index.KeyCompare;
import com.caucho.db.index.StringKeyCompare;
import com.caucho.db.sql.Expr;
import com.caucho.db.sql.QueryContext;
import com.caucho.db.sql.SelectResult;
import com.caucho.db.store.Transaction;
import com.caucho.sql.SQLExceptionWrapper;
import com.caucho.util.L10N;

import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

class StringColumn extends Column {
  private static final Logger log
    = Logger.getLogger(StringColumn.class.getName());
  private static final L10N L = new L10N(StringColumn.class);
  
  private final int _maxLength;

  /**
   * Creates a string column.
   *
   * @param columnOffset the offset within the row
   * @param maxLength the maximum length of the string
   */
  StringColumn(Row row, String name, int maxLength)
  {
    super(row, name);

    if (maxLength < 0)
      throw new IllegalArgumentException("length must be non-negative");
    else if (255 < maxLength)
      throw new IllegalArgumentException("length too big");
    
    _maxLength = maxLength;
  }

  /**
   * Returns the type code for the column.
   */
  public int getTypeCode()
  {
    return VARCHAR;
  }

  /**
   * Returns the java type.
   */
  public Class getJavaType()
  {
    return String.class;
  }

  /**
   * Returns the declaration size
   */
  public int getDeclarationSize()
  {
    return _maxLength;
  }

  /**
   * Returns the column's size.
   */
  public int getLength()
  {
    return 2 * _maxLength + 1;
  }

  /**
   * Returns the key compare for the column.
   */
  public KeyCompare getIndexKeyCompare()
  {
    return new StringKeyCompare();
  }

  /**
   * Sets the string value.
   *
   * @param block the buffer to store the row
   * @param rowOffset the offset into the row
   * @param str the string value
   */
  void setString(Transaction xa, byte []block, int rowOffset, String str)
  {
    int offset = rowOffset + _columnOffset;
    
    if (str == null) {
      setNull(block, rowOffset);
      return;
    }

    int len = str.length();
    int maxOffset = offset + 2 * _maxLength + 1;

    block[offset++] = (byte) (len);
    for (int i = 0; i < len && offset < maxOffset; i++) {
      int ch = str.charAt(i);

      block[offset++] = (byte) (ch >> 8);
      block[offset++] = (byte) (ch);
    }

    setNonNull(block, rowOffset);
  }
  
  public String getString(byte []block, int rowOffset)
  {
    if (isNull(block, rowOffset))
      return null;
    
    int startOffset = rowOffset + _columnOffset;
    int len = block[startOffset] & 0xff;

    char []cBuf = new char[len];

    int offset = startOffset + 1;
    int endOffset = offset + 2 * len;
    int i = 0;
    while (offset < endOffset) {
      int ch1 = block[offset++] & 0xff;
      int ch2 = block[offset++] & 0xff;

      cBuf[i++] = (char) ((ch1 << 8) + ch2);
    }

    return new String(cBuf, 0, cBuf.length);
  }
  
  /**
   * Sets the column based on an expression.
   *
   * @param block the block's buffer
   * @param rowOffset the offset of the row in the block
   * @param expr the expression to store
   */
  void setExpr(Transaction xa,
	       byte []block, int rowOffset,
	       Expr expr, QueryContext context)
    throws SQLException
  {
    if (expr.isNull(null))
      setNull(block, rowOffset);
    else
      setString(xa, block, rowOffset, expr.evalString(context));
  }

  /**
   * Returns true if the items in the given rows match.
   */
  public boolean isEqual(byte []block1, int rowOffset1,
			 byte []block2, int rowOffset2)
  {
    if (isNull(block1, rowOffset1) != isNull(block2, rowOffset2))
      return false;

    int startOffset1 = rowOffset1 + _columnOffset;
    int len1 = block1[startOffset1] & 0xff;

    int startOffset2 = rowOffset2 + _columnOffset;
    int len2 = block2[startOffset2] & 0xff;

    if (len1 != len2)
      return false;

    for (int i = 2 * len1; i > 0; i--) {
      if (block1[startOffset1 + i] != block2[startOffset2 + i])
	return false;
    }

    return true;
  }

  /**
   * Returns true if the bytes match.
   */
  public boolean isEqual(byte []block, int rowOffset,
			 byte []buffer, int offset, int length)
  {
    if (isNull(block, rowOffset))
      return false;

    int startOffset = rowOffset + _columnOffset;
    int len = block[startOffset] & 0xff;

    if (len != length)
      return false;

    int blockOffset = startOffset + 1;
    int endOffset = blockOffset + 2 * len;
    while (blockOffset < endOffset) {
      if (block[blockOffset++] != buffer[offset++])
	return false;
    }

    return true;
  }
  
  public boolean isEqual(byte []block, int rowOffset, String value)
  {
    if (value == null)
      return isNull(block, rowOffset);
    else if (isNull(block, rowOffset))
      return false;
    
    int startOffset = rowOffset + _columnOffset;
    int len = block[startOffset] & 0xff;

    int strLength = value.length();
    int strOffset = 0;

    int offset = startOffset + 1;
    int endOffset = offset + 2 * len;
    while (offset < endOffset && strOffset < strLength) {
      int ch1 = ((block[offset++] & 0xff) << 8) + (block[offset++] & 0xff);
      char ch = value.charAt(strOffset++);

      if (ch1 != ch)
	return false;
    }

    return offset == endOffset && strOffset == strLength;
  }

  /**
   * Evaluates the column to a stream.
   */
  public void evalToResult(byte []block, int rowOffset, SelectResult result)
  {
    if (isNull(block, rowOffset)) {
      result.writeNull();
      return;
    }

    int startOffset = rowOffset + _columnOffset;
    int len = block[startOffset] & 0xff;

    result.writeString(block, startOffset + 1, len);
    /*
    result.write(Column.VARCHAR);
    result.write(0);
    result.write(0);
    result.write(0);
    result.write(len);
    result.write(block, startOffset + 1, len);
    */
  }
  
  /**
   * Evaluate to a buffer.
   *
   * @param block the block's buffer
   * @param rowOffset the offset of the row in the block
   * @param buffer the result buffer
   * @param buffer the result buffer offset
   *
   * @return the length of the value
   */
  int evalToBuffer(byte []block, int rowOffset,
		   byte []buffer, int bufferOffset)
    throws SQLException
  {
    if (isNull(block, rowOffset))
      return 0;

    int startOffset = rowOffset + _columnOffset;
    // static length for now
    int len = getLength();

    System.arraycopy(block, startOffset, buffer, bufferOffset, len);

    return len;
  }
  
  /**
   * Sets any index for the column.
   *
   * @param block the block's buffer
   * @param rowOffset the offset of the row in the block
   * @param rowAddr the address of the row
   */
  void setIndex(Transaction xa,
		byte []block, int rowOffset,
		long rowAddr, QueryContext context)
    throws SQLException
  {
    BTree index = getIndex();

    if (index != null) {
      try {
	index.insert(block,
		     rowOffset + _columnOffset, getLength(),
		     rowAddr,
		     xa,
		     false);
      } catch (SQLException e) {
	log.log(Level.FINER, e.toString(), e);
	
	throw new SQLExceptionWrapper(L.l("StringColumn '{0}.{1}' unique index set failed for {2}.\n{3}",
					  getTable().getName(),
					  getName(),
					  getString(block, rowOffset),
					  e.toString()),
				      e);
      }
    }
  }
  
  /**
   * Deleting the row, based on the column.
   *
   * @param block the block's buffer
   * @param rowOffset the offset of the row in the block
   * @param expr the expression to store
   */
  void delete(Transaction xa, byte []block, int rowOffset)
    throws SQLException
  {
    BTree index = getIndex();

    if (index != null)
      index.remove(block, rowOffset + _columnOffset, getLength(), xa);
  }

  public String toString()
  {
    if (getIndex() != null)
      return "StringColumn[" + getName() + ",index]";
    else
      return "StringColumn[" + getName() + "]";
  }
}
