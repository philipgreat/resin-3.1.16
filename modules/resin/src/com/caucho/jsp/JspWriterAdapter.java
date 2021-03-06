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

package com.caucho.jsp;

import com.caucho.server.connection.AbstractResponseStream;
import com.caucho.util.L10N;

import javax.servlet.jsp.JspWriter;
import java.io.IOException;
import java.util.logging.*;

/**
 * A buffered JSP writer encapsulating a Writer.
 */
public class JspWriterAdapter extends AbstractBodyContent {
  private static final Logger log
    = Logger.getLogger(JspWriterAdapter.class.getName());
  private static final L10N L = new L10N(JspWriterAdapter.class);

  // the parent writer
  private JspWriter _parent;

  // the underlying writer
  private AbstractResponseStream _out;

  // the page context
  private PageContextImpl _pageContext;

  private boolean _isClosed;

  /**
   * Creates a new JspWriterAdapter
   */
  JspWriterAdapter()
  {
  }

  /**
   * Initialize the JSP writer
   *
   * @param os the underlying stream
   */
  void init(PageContextImpl pageContext)
  {
    _pageContext = pageContext;
    _out = null;
    _isClosed = false;
  }

  /**
   * Initialize the JSP writer
   *
   * @param os the underlying stream
   */
  void init(JspWriter parent, AbstractResponseStream out)
  {
    _parent = parent;
    _out = out;
    _isClosed = false;
  }

  /**
   * Writes a character array to the writer.
   *
   * @param buf the buffer to write.
   * @param off the offset into the buffer
   * @param len the number of characters to write
   */
  final public void write(char []buf, int offset, int length)
    throws IOException
  {
    if (_isClosed) {
      // jsp/15m7
      if (log.isLoggable(Level.FINE))
        log.fine(this + " write() forbidden after writer is closed");

      return;
    }

    _out.print(buf, offset, length);
  }

  /**
   * Writes a character to the output.
   *
   * @param buf the buffer to write.
   */
  final public void write(int ch) throws IOException
  {
    if (_isClosed) {
      if (Character.isWhitespace(ch))
        return;

      // jsp/15m7
      if (log.isLoggable(Level.FINE))
        log.fine(this + " write() forbidden after writer is closed");

      return;
    }

    _out.print(ch);
  }

  /**
   * Prints the newline.
   */
  final public void println() throws IOException
  {
    if (_isClosed) {
      // jsp/15m7
      if (log.isLoggable(Level.FINE))
        log.fine(this + " write() forbidden after writer is closed");

      return;
    }

    _out.print('\n');
  }

  /**
   * Writes a subsection of a string to the output.
   */
  final public void write(String s, int off, int len)
    throws IOException
  {
    if (_isClosed) {
      // jsp/15m7
      if (log.isLoggable(Level.FINE))
        log.fine(this + " write() forbidden after writer is closed");

      return;
    }

    char []writeBuffer = _out.getCharBuffer();
    int size = writeBuffer.length;
    int writeLength = _out.getCharOffset();
    int end = off + len;

    while (off < end) {
      int sublen = end - off;

      if (size - writeLength < sublen) {
        if (size == writeLength) {
          writeBuffer = _out.nextCharBuffer(writeLength);
          writeLength = _out.getCharOffset();

          sublen = Math.min(size - writeLength, sublen);
        }
        else
          sublen = size - writeLength;
      }

      int tail = off + sublen;
      s.getChars(off, tail, writeBuffer, writeLength);

      off = tail;
      writeLength += sublen;
    }

    _out.setCharOffset(writeLength);
  }

  /**
   * Returns the buffer size of the writer.
   */
  public int getBufferSize()
  {
    return _out.getBufferSize();
  }

  /**
   * Returns the remaining bytes in the buffer.
   */
  public int getRemaining()
  {
    return _out.getRemaining();
  }

  public void clear() throws IOException
  {
    if (_isClosed) {
      // jsp/15m7
      if (log.isLoggable(Level.FINE))
        log.fine(this + " write() forbidden after writer is closed");

      return;
    }

    /*
    else if (_out.isCommitted()) {
      // jsp/0502
      throw new IOException(L.l("clear() forbidden after data is committed"));
    }
    */

    _out.clear();
    // clearBuffer();
  }

  public void clearBuffer() throws IOException
  {
    if (_isClosed)
      return;

    _out.clearBuffer();
  }

  public void flushBuffer()
    throws IOException
  {
    if (_isClosed) {
      // jsp/15m7
      if (log.isLoggable(Level.FINE))
        log.fine(this + " write() forbidden after writer is closed");

      return;
    }

    _out.flushBuffer();
  }

  /**
   * Flushes the output stream.
   */
  public void flush() throws IOException
  {
    if (_isClosed) {
      // jsp/15m7
      if (log.isLoggable(Level.FINE))
        log.fine(this + " write() forbidden after writer is closed");

      return;
    }

    _out.flushChar();
  }

  /**
   * Pops the enclosing writer.
   */
  AbstractJspWriter popWriter()
  {
    try {
      close();
    } catch (Throwable e) {
      log.log(Level.FINER, e.toString(), e);
    }

    return super.popWriter();
  }

  final public void close() throws IOException
  {
    _isClosed = true;

    _out = null;
    _parent = null;
    _pageContext = null;
  }
}
