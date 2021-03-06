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

package com.caucho.server.cluster;

import com.caucho.config.types.Period;
import com.caucho.lifecycle.Lifecycle;
import com.caucho.loader.ClassLoaderListener;
import com.caucho.loader.DynamicClassLoader;
import com.caucho.loader.Environment;
import com.caucho.loader.EnvironmentClassLoader;
import com.caucho.loader.EnvironmentListener;
import com.caucho.management.server.PersistentStoreMXBean;
import com.caucho.util.Alarm;
import com.caucho.util.AlarmListener;
import com.caucho.util.L10N;
import com.caucho.util.LruCache;
import com.caucho.vfs.TempStream;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Base class for distributed stores.
 */
abstract public class StoreManager
  implements AlarmListener, EnvironmentListener, ClassLoaderListener {
  static protected final Logger log
    = Logger.getLogger(StoreManager.class.getName());
  static final L10N L = new L10N(StoreManager.class);
  
  private static int DECODE[];

  private Cluster _cluster;
  private String _serverId;
  
  protected int _selfIndex;
  private ServerConnector []_serverList;
  
  private Alarm _alarm;
  
  // protected long _maxIdleTime = 24 * 3600 * 1000L;
  protected long _maxIdleTime = 3 * 3600 * 1000L;
  protected long _idleCheckInterval = 5 * 60 * 1000L;

  protected boolean _isAlwaysLoad;
  protected boolean _isAlwaysSave;
  
  protected HashMap<String,Store> _storeMap;
  protected LruCache<String,ClusterObject> _clusterObjects;

  private final Lifecycle _lifecycle = new Lifecycle(log, toString());

  //
  // statistics
  //
  
  protected volatile long _loadCount;
  protected volatile long _loadFailCount;

  protected volatile long _saveCount;
  protected volatile long _saveFailCount;

  protected StoreManager()
  {
    _clusterObjects = new LruCache<String,ClusterObject>(4096);
    _clusterObjects.setEnableListeners(false);
    
    _storeMap = new HashMap<String,Store>();

    _alarm = new Alarm(this);

    Environment.addClassLoaderListener(this);
  }

  /**
   * Sets the cluster.
   */
  public void setCluster(Cluster cluster)
  {
    _cluster = cluster;
  }

  /**
   * Gets the cluster.
   */
  public Cluster getCluster()
  {
    return _cluster;
  }

  /**
   * Returns the admin.
   */
  public PersistentStoreMXBean getAdmin()
  {
    return null;
  }

  /**
   * Set true if the store should always try to load the object.
   */
  public void setAlwaysLoad(boolean alwaysLoad)
  {
    _isAlwaysLoad = alwaysLoad;
  }

  /**
   * Set true if the store should always try to load the object.
   */
  public boolean isAlwaysLoad()
  {
    return _isAlwaysLoad;
  }

  /**
   * Set true if the store should always try to store the object.
   */
  public void setAlwaysSave(boolean alwaysSave)
  {
    _isAlwaysSave = alwaysSave;
  }

  /**
   * Set true if the store should always try to store the object.
   */
  public boolean isAlwaysSave()
  {
    return _isAlwaysSave;
  }

  /**
   * Returns the length of time an idle object can remain in the store before
   * being cleaned.
   */
  public long getMaxIdleTime()
  {
    return _maxIdleTime;
  }

  /**
   * Sets the length of time an idle object can remain in the store before
   * being cleaned.
   */
  public void setMaxIdleTime(Period maxIdleTime)
  {
    _maxIdleTime = maxIdleTime.getPeriod();
  }

  /**
   * Sets the idle check interval for the alarm.
   */
  public void updateIdleCheckInterval(long idleCheckInterval)
  {
    if (_idleCheckInterval > 0 && idleCheckInterval > 0
	&& idleCheckInterval < _idleCheckInterval) {
      _idleCheckInterval = idleCheckInterval;
      _alarm.queue(idleCheckInterval);
    }

    if (_idleCheckInterval >= 0 && _idleCheckInterval < 1000)
      _idleCheckInterval = 1000;
  }

  /**
   * Sets the idle check interval for the alarm.
   */
  public long getIdleCheckTime()
  {
    if (_idleCheckInterval > 0)
      return _idleCheckInterval;
    else
      return _maxIdleTime;
  }

  /**
   * Returns the length of time an idle object can remain in the store before
   * being cleaned.
   */
  public long getAccessWindowTime()
  {
    long window = _maxIdleTime / 4;

    if (window < 60000L)
      return 60000L;
    else
      return window;
  }

  //
  // statistics
  //

  /**
   * Returns the objects in the store
   */
  public long getObjectCount()
  {
    return -1;
  }

  /**
   * Returns the total objects loaded.
   */
  public long getLoadCount()
  {
    return _loadCount;
  }

  /**
   * Returns the objects which failed to load.
   */
  public long getLoadFailCount()
  {
    return _loadFailCount;
  }

  /**
   * Returns the total objects saved.
   */
  public long getSaveCount()
  {
    return _saveCount;
  }

  /**
   * Returns the objects which failed to save.
   */
  public long getSaveFailCount()
  {
    return _saveFailCount;
  }

  /**
   * Creates a Store.  The Store manages
   * persistent objects for a particular domain, like the sesions
   * for the /foo URL.
   *
   * @param storeId the persistent domain.
   */
  public Store createStore(String storeId, ObjectManager objectManager)
  {
    Store store = getStore(storeId);

    store.setObjectManager(objectManager);

    return store;
  }

  /**
   * Removes a Store.  The Store manages
   * persistent objects for a particular domain, like the sesions
   * for the /foo URL.
   *
   * @param storeId the persistent domain.
   */
  public Store removeStore(String storeId)
  {
    Store store = getStore(storeId);

    store.setObjectManager(null);

    return store;
  }

  /**
   * Creates a ClusterObjectManager.  The ClusterObjectManager manages
   * persistent objects for a particular domain, like the sesions
   * for the /foo URL.
   *
   * @param storeId the persistent domain.
   */
  public Store getStore(String storeId)
  {
    synchronized (_storeMap) {
      Store store = _storeMap.get(storeId);
      if (store == null) {
	store = new Store(storeId, this);
	_storeMap.put(storeId, store);
      }

      return store;
    }
  }

  /**
   * Called after any factory settings.
   */
  @PostConstruct
  public boolean init()
    throws Exception
  {
    if (! _lifecycle.toInit())
      return false;

    _lifecycle.setName(toString());

    if (_cluster == null)
      _cluster = Cluster.getLocal();
    
    if (_cluster != null) {
      _serverId = Cluster.getServerId();
      ClusterServer selfServer = _cluster.getSelfServer();

      if (selfServer != null)
	_selfIndex = selfServer.getIndex();
      else if (_cluster.getServerList().length > 1) {
	// XXX: error?
	log.warning(L.l("cluster-store for '{0}' needs an <srun> configuration for it.",
			_serverId));
      }

      ClusterServer []serverList = _cluster.getServerList();
      
      _serverList = new ServerConnector[serverList.length];

      for (int i = 0; i < serverList.length; i++) {
	_serverList[i] = serverList[i].getServerConnector();
      }
    }


    Environment.addEnvironmentListener(this);

    return true;
  }

  /**
   * Called to start the store.
   */
  public boolean start()
    throws Exception
  {
    if (! _lifecycle.toActive())
      return false;
    
    // notify the siblings that we're awake
    if (_serverList != null) {
      ServerConnector []serverList = _serverList;

      for (int i = 0; i < serverList.length; i++) {
	ServerConnector server = serverList[i];

	if (server == null)
	  continue;

	try {
	  ClusterStream s = server.open();
	  s.close();
	} catch (Throwable e) {
	}
      }
      
    }

    handleAlarm(_alarm);

    return true;
  }

  /**
   * Cleans old objects.  Living objects corresponding to the old
   * objects are not cleared, since their timeout should be less than
   * the store timeout.
   */
  public void clearOldObjects()
    throws Exception
  {
  }

  /**
   * Returns true if this server is a primary for the given object id.
   */
  protected boolean isPrimary(String id)
  {
    return getPrimaryIndex(id, 0) == getSelfIndex();
  }

  /**
   * Returns the owning index.
   */
  public int getPrimaryIndex(String id, int offset)
  {
    return 0;
  }

  /**
   * Returns the backup index.
   */
  public int getSecondaryIndex(String id, int offset)
  {
    return 0;
  }

  /**
   * Returns the backup index.
   */
  public int getTertiaryIndex(String id, int offset)
  {
    return 0;
  }

  /**
   * Loads object access time.
   *
   * @param obj the object to update.
   */
  abstract protected boolean load(ClusterObject clusterObject, Object obj)
    throws Exception;

  /**
   * Updates the object's access time.
   *
   * @param storeId the identifier of the storage group
   * @param obj the object to update.
   */
  public void access(String uniqueId)
    throws Exception
  {
    ClusterObject obj = getClusterObject(uniqueId);

    if (obj != null)
      obj.access();
    else
      accessImpl(obj.getObjectId(), uniqueId);
  }

  /**
   * Updates the object's access time.
   *
   * @param storeId the identifier of the storage group
   * @param obj the object to update.
   */
  public void access(Store store, String id)
    throws Exception
  {
    getClusterObject(store, id).access();
  }
  
  /**
   * Updates the object's access time in the persistent store.
   *
   * @param uniqueId the identifier of the object.
   */
  abstract public void accessImpl(String objectId, String uniqueId)
    throws Exception;
  
  /**
   * Sets the timef for the expires interval.
   *
   * @param uniqueId the identifier of the object.
   * @param long the time in ms for the expire
   */
  public void setExpireInterval(String uniqueId, long expires)
    throws Exception
  {
  }

  /**
   * When the object is no longer valid, remove it from the backing store.
   *
   * @param storeId the identifier of the storeage group
   * @param objectId the identifier of the object to remove
   */
  public void update(String storeId, String objectId)
    throws Exception
  {
    ClusterObject obj = getClusterObject(storeId, objectId);

    if (obj != null)
      obj.update();
  }

  /**
   * Updates the owner object.
   *
   * @param uniqueId the identifier of the storage group
   */
  public void updateOwner(String objectId, String uniqueId)
    throws Exception
  {
    
  }
  
  /**
   * Saves the object to the cluster.
   *
   * @param storeId the identifier of the storage group
   * @param obj the object to store.
   */
  public void store(Store store, String id, Object obj)
    throws IOException
  {
    ClusterObject clusterObj = getClusterObject(store, id);

    if (clusterObj != null) {
    }
    else if (store.getObjectManager().isEmpty(obj))
      return;
    else
      clusterObj = createClusterObject(store, id);
    
    clusterObj.store(obj);
  }

  /**
   * Returns the cluster object.
   *
   * @param storeId the identifier of the storage group
   * @param obj the object to store.
   */
  ClusterObject createClusterObject(Store store, String id)
  {
    try {
      String uniqueId = store.getId() + ';' + id;

      synchronized (this) {
	ClusterObject clusterObj = _clusterObjects.get(uniqueId);
	if (clusterObj == null) {
	  clusterObj = create(store, id);
	  _clusterObjects.put(clusterObj.getUniqueId(), clusterObj);
	}

	return clusterObj;
      }
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);

      return null;
    }
  }

  /**
   * Returns the cluster object.
   *
   * @param storeId the identifier of the storage group
   * @param obj the object to store.
   */
  ClusterObject getClusterObject(Store store, String id)
  {
    return getClusterObject(makeUniqueId(store, id));
  }

  /**
   * Returns the cluster object.
   *
   * @param storeId the identifier of the storage group
   * @param obj the object to store.
   */
  ClusterObject getClusterObject(String storeId, String id)
  {
    return getClusterObject(makeUniqueId(storeId, id));
  }

  /**
   * Returns the cluster object.
   *
   * @param storeId the identifier of the storage group
   * @param obj the object to store.
   */
  ClusterObject getClusterObject(String uniqueId)
  {
    return _clusterObjects.get(uniqueId);
  }

  /**
   * Returns the cluster object.
   *
   * @param storeId the identifier of the storage group
   * @param obj the object to store.
   */
  ClusterObject removeClusterObject(String storeId, String id)
  {
    synchronized (this) {
      return _clusterObjects.remove(makeUniqueId(storeId, id));
    }
  }
  
  /**
   * Creates the cluster object.
   */
  ClusterObject create(Store store, String id)
  {
    return new ClusterObject(this, store, id);
  }
  
  /**
   * Save the object to the store.
   *
   * @param storeId the identifier of the storage group
   * @param obj the object to store.
   */
  abstract protected void store(ClusterObject clusterObject,
				TempStream tempStream,
				long crc)
    throws Exception;
  
  /**
   * Handles a callback from an alarm, scheduling the timeout.
   */
  public void handleAlarm(Alarm alarm)
  {
    if (! _lifecycle.isActive())
      return;

    try {
      clearOldObjects();
    } catch (Throwable e) {
      log.log(Level.WARNING, e.toString(), e);
    } finally {
      _alarm.queue(getIdleCheckTime());
    }
  }

  /**
   * When the object is no longer valid, remove it from the backing store.
   *
   * @param obj the object to remove
   */
  public void remove(ClusterObject obj)
    throws Exception
  {
    
  }

  /**
   * When the object is no longer valid, remove it from the backing store.
   *
   * @param store the identifier of the storeage group
   * @param objectId the identifier of the object to remove
   */
  public void remove(Store store, String objectId)
    throws Exception
  {
  }

  /**
   * Returns the unique id.
   */
  private String makeUniqueId(Store store, String objectId)
  {
    return store.getId() + ';' + objectId;
  }

  /**
   * Returns the unique id.
   */
  private String makeUniqueId(String storeId, String objectId)
  {
    return storeId + ';' + objectId;
  }

  /**
   * Returns the self servers.
   */
  protected int getSelfIndex()
  {
    return _selfIndex;
  }

  /**
   * Returns the list of cluster servers.
   */
  protected ServerConnector []getServerList()
  {
    return _serverList;
  }

  /**
   * Returns the cluster server which owns the object
   */
  protected ServerConnector getOwningServer(String objectId)
  {
    if (_cluster == null)
      return null;
    
    char ch = objectId.charAt(0);
    
    ServerConnector []serverList = _serverList;

    if (serverList.length > 0) {
      int srunIndex = decode(ch) % serverList.length;

      return serverList[srunIndex];
    }
    else
      return null;
  }
  
  /**
   * Handles the case where the environment is activated.
   */
  public void environmentBind(EnvironmentClassLoader loader)
  {
  }
  
  /**
   * Handles the case where the environment is activated.
   */
  public void environmentStart(EnvironmentClassLoader loader)
  {
    try {
      start();
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);
    }
  }
  
  /**
   * Handles the case where the environment loader is stops
   */
  public void environmentStop(EnvironmentClassLoader loader)
  {
  }
  
  /**
   * Handles the case where the environment is activated.
   */
  public void classLoaderInit(DynamicClassLoader loader)
  {
  }
  
  /**
   * Handles the case where the environment loader is dropped.
   */
  public void classLoaderDestroy(DynamicClassLoader loader)
  {
    destroy();
  }
  
  /**
   * Called at end of life.
   */
  public void destroy()
  {
    if (! _lifecycle.toDestroy())
      return;
    
    _alarm.dequeue();
  }

  public String toString()
  {
    return getClass().getSimpleName() + "[" + _serverId + "]";
  }

  static class ObjectKey {
    private String _storeId;
    private String _objectId;

    ObjectKey()
    {
    }
    
    ObjectKey(String storeId, String objectId)
    {
      init(storeId, objectId);
    }

    void init(String storeId, String objectId)
    {
      _storeId = storeId;
      _objectId = objectId;
    }

    @Override
    public int hashCode()
    {
      return _storeId.hashCode() * 65521 + _objectId.hashCode();
    }

    @Override
    public boolean equals(Object o)
    {
      if (this == o)
	return true;
      else if (! (o instanceof ObjectKey))
	return false;

      ObjectKey key = (ObjectKey) o;

      return _objectId.equals(key._objectId) && _storeId.equals(key._storeId);
    }
  }

  static int decode(int code)
  {
    return DECODE[code & 0x7f];
  }

  /**
   * Converts an integer to a printable character
   */
  private static char convert(long code)
  {
    code = code & 0x3f;
    
    if (code < 26)
      return (char) ('a' + code);
    else if (code < 52)
      return (char) ('A' + code - 26);
    else if (code < 62)
      return (char) ('0' + code - 52);
    else if (code == 62)
      return '_';
    else
      return '-';
  }

  static {
    DECODE = new int[128];
    for (int i = 0; i < 64; i++)
      DECODE[(int) convert(i)] = i;
  }
}
