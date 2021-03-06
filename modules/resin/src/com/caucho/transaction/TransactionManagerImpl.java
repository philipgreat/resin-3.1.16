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

package com.caucho.transaction;

import com.caucho.config.types.Period;
import com.caucho.loader.ClassLoaderListener;
import com.caucho.loader.DynamicClassLoader;
import com.caucho.loader.Environment;
import com.caucho.log.Log;
import com.caucho.transaction.xalog.AbstractXALogManager;
import com.caucho.transaction.xalog.AbstractXALogStream;
import com.caucho.util.Crc64;
import com.caucho.util.L10N;
import com.caucho.util.RandomUtil;
import com.caucho.webbeans.component.*;

import javax.transaction.*;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;
import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implementation of the transaction manager.
 */
public class TransactionManagerImpl
  implements TransactionManager, Serializable,
             ClassLoaderListener
{
  private static L10N L = new L10N(TransactionManagerImpl.class);
  private static Logger log
    = Logger.getLogger(TransactionManagerImpl.class.getName());

  private static TransactionManagerImpl _tm = new TransactionManagerImpl();

  private long _serverId;

  private AbstractXALogManager _xaLogManager;

  // thread local is dependent on the transaction manager
  private ThreadLocal<TransactionImpl> _threadTransaction
    = new ThreadLocal<TransactionImpl>();

  private ArrayList<WeakReference<TransactionImpl>> _transactionList
    = new ArrayList<WeakReference<TransactionImpl>>();

  // listeners for transaction begin
  private ArrayList<TransactionListener> _transactionListeners
    = new ArrayList<TransactionListener>();

  private long _timeout = -1;

  public TransactionManagerImpl()
  {
  }

  /**
   * Returns the local transaction manager.
   */
  public static TransactionManagerImpl getInstance()
  {
    return _tm;
  }

  /**
   * Returns the local transaction manager.
   */
  public static TransactionManagerImpl getLocal()
  {
    return _tm;
  }

  /**
   * Sets the timeout.
   */
  public void setTimeout(Period timeout)
  {
    _timeout = timeout.getPeriod();
  }

  /**
   * Gets the timeout.
   */
  public long getTimeout()
  {
    return _timeout;
  }

  /**
   * Sets the XA log manager.
   */
  public void setXALogManager(AbstractXALogManager xaLogManager)
  {
    _xaLogManager = xaLogManager;
  }

  /**
   * Create a new transaction and associate it with the thread.
   */
  public void begin()
    throws NotSupportedException, SystemException
  {
    getCurrent().begin();
  }

  /**
   * Creates a new transaction id.
   */
  XidImpl createXID()
  {
    return new XidImpl(getServerId(),
                       RandomUtil.getRandomLong());
  }
  /**
   * Creates a new transaction id.
   */
  AbstractXALogStream getXALogStream()
  {
    if (_xaLogManager != null)
      return _xaLogManager.getStream();
    else
      return null;
  }

  /**
   * Returns the server id.
   */
  private long getServerId()
  {
    if (_serverId == 0) {
      String server = (String) Environment.getAttribute("caucho.server-id");

      if ("".equals(server))
        server = "default";

      if (server == null)
        _serverId = 1;
      else
        _serverId = Crc64.generate(server);
    }

    return _serverId;
  }

  /**
   * Returns the transaction for the current thread.
   */
  public Transaction getTransaction()
    throws SystemException
  {
    TransactionImpl trans = _threadTransaction.get();

    if (trans == null
        || trans.getStatus() == Status.STATUS_NO_TRANSACTION
        || trans.getStatus() == Status.STATUS_UNKNOWN
        || trans.isSuspended()) {
      return null;
    }
    else {
      return trans;
    }
  }

  /**
   * Suspend the transaction.
   */
  public Transaction suspend()
    throws SystemException
  {
    TransactionImpl trans = _threadTransaction.get();

    if (trans == null ||
        (! trans.hasResources() &&
         (trans.getStatus() == Status.STATUS_NO_TRANSACTION ||
          trans.getStatus() == Status.STATUS_UNKNOWN)))
      return null;

    _threadTransaction.set(null);
    trans.suspend();

    return trans;
  }

  /**
   * Resume the transaction.
   */
  public void resume(Transaction tobj)
    throws InvalidTransactionException, SystemException
  {
    Transaction old = _threadTransaction.get();

    if (old != null && old.getStatus() != Status.STATUS_NO_TRANSACTION)
      throw new SystemException(L.l("can't resume transaction with active transaction {0}", String.valueOf(old)));

    TransactionImpl impl = (TransactionImpl) tobj;

    impl.resume();

    _threadTransaction.set(impl);
  }

  /**
   * Force any completion to be a rollback.
   */
  public void setRollbackOnly()
    throws SystemException
  {
    getCurrent().setRollbackOnly();
  }

  /**
   * Force any completion to be a rollback.
   */
  public void setRollbackOnly(Exception e)
  {
    getCurrent().setRollbackOnly(e);
  }

  /**
   * Returns the transaction's status
   */
  public int getStatus()
    throws SystemException
  {
    return getCurrent().getStatus();
  }

  /**
   * sets the timeout for the transaction
   */
  public void setTransactionTimeout(int seconds)
    throws SystemException
  {
    getCurrent().setTransactionTimeout(seconds);
  }

  /**
   * Commit the transaction.
   */
  public void commit()
    throws RollbackException, HeuristicMixedException,
    HeuristicRollbackException, SystemException
  {
    getCurrent().commit();
  }

  /**
   * Rollback the transaction.
   */
  public void rollback()
  {
    getCurrent().rollback();
  }

  /**
   * Returns the corresponding user transaction.
   */
  /*
  public UserTransaction getUserTransaction()
  {
    return this;
  }
  */

  /**
   * Returns the current TransactionImpl, creating if necessary.
   *
   * <p/>The TransactionImpl is not an official externally
   * visible Transaction if the status == NO_TRANSACTION.
   */
  public TransactionImpl getCurrent()
  {
    TransactionImpl trans = _threadTransaction.get();

    if (trans == null || trans.isDead()) {
      trans = new TransactionImpl(this);
      _threadTransaction.set(trans);

      addTransaction(trans);
    }

    return trans;
  }

  private void addTransaction(TransactionImpl trans)
  {
    synchronized (_transactionList) {
      for (int i = _transactionList.size() - 1; i >= 0; i--) {
        WeakReference<TransactionImpl> ref = _transactionList.get(i);

        if (ref.get() == null)
          _transactionList.remove(i);
      }

      _transactionList.add(new WeakReference<TransactionImpl>(trans));
    }
  }

  /**
   * Returns the corresponding user transaction.
   */
  public void recover(XAResource xaRes)
    throws XAException
  {
    Xid []xids;

    xids = xaRes.recover(XAResource.TMSTARTRSCAN|XAResource.TMENDRSCAN);

    if (xids == null)
      return;

    for (int i = 0; i < xids.length; i++) {
      byte []global = xids[i].getGlobalTransactionId();

      if (global.length != XidImpl.GLOBAL_LENGTH)
        continue;

      XidImpl xidImpl = new XidImpl(xids[i].getGlobalTransactionId());

      if (! xidImpl.isSameServer(getServerId()))
        continue;

      if (_xaLogManager != null
          && _xaLogManager.hasCommittedXid(xidImpl)) {
        log.fine(L.l("XAResource {0} commit xid {1}", xaRes, xidImpl));

        try {
          xaRes.commit(xids[i], false);
        } catch (Throwable e) {
          log.log(Level.WARNING, e.toString(), e);
        }
      }
      else {
        // XXX: need to check if the transaction belongs to this TM
        // the ownership is encoded in the xid

        log.fine(L.l("XAResource {0} forget xid {1}", xaRes, xidImpl));

        boolean isValid = false;

        try {
          xaRes.rollback(xids[i]);
          isValid = true;
          // #4748
          // xaRes.forget(xidImpl);
        } catch (Throwable e) {
          log.log(Level.FINER, e.toString(), e);
        }

        if (! isValid) {
          try {
            // #4748
            xaRes.forget(xids[i]);
            isValid = true;
          } catch (Throwable e) {
            log.log(Level.FINER, e.toString(), e);
          }
        }
      }
    }
  }

  /**
   * Flushes the log stream (primarily for QA).
   */
  public void flush()
  {
    if (_xaLogManager != null)
      _xaLogManager.flush();
  }

  /**
   * Handles the case where a class loader has completed initialization
   */
  public void classLoaderInit(DynamicClassLoader loader)
  {
  }

  /**
   * Handles the case where a class loader is dropped.
   */
  public void classLoaderDestroy(DynamicClassLoader loader)
  {
    AbstractXALogManager xaLogManager = _xaLogManager;
    _xaLogManager = null;

    if (xaLogManager != null)
      xaLogManager.close();

    _serverId = 0;

    synchronized (_transactionList) {
      for (int i = _transactionList.size() - 1; i >= 0; i--) {
        WeakReference<TransactionImpl> ref = _transactionList.get(i);
        TransactionImpl xa = ref.get();

        try {
          if (xa != null) {
            xa.rollback();
          }
        } catch (Throwable e) {
          log.log(Level.WARNING, e.toString(), e);
        }
      }
    }
  }

  /**
   * Clearing for test purposes.
   */
  public void testClear()
  {
    _serverId = 0;
    _timeout = -1;
    AbstractXALogManager logManager = _xaLogManager;
    _xaLogManager = null;

    if (logManager != null)
      logManager.close();
  }

  /**
   * Serialize to a handle
   */
  private Object writeReplace()
  {
    return new WebBeansHandle(TransactionManager.class);
  }

  public String toString()
  {
    return "TransactionManagerImpl[]";
  }
}
