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

package com.caucho.amber.manager;

import com.caucho.amber.AmberRuntimeException;
import com.caucho.amber.cfg.*;
import com.caucho.amber.gen.AmberEnhancer;
import com.caucho.amber.gen.AmberGenerator;
import com.caucho.amber.type.*;
import com.caucho.bytecode.JClass;
import com.caucho.bytecode.JClassLoader;
import com.caucho.config.*;
import com.caucho.loader.*;
import com.caucho.loader.enhancer.EnhancerManager;
import com.caucho.loader.enhancer.ScanListener;
import com.caucho.util.*;
import com.caucho.vfs.*;
import com.caucho.webbeans.manager.*;

import javax.sql.DataSource;
import javax.persistence.*;
import javax.persistence.spi.*;
import java.io.InputStream;
import java.net.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Environment-based container.
 */
public class AmberContainer implements ScanListener, EnvironmentListener {
  private static final L10N L = new L10N(AmberContainer.class);
  private static final Logger log
    = Logger.getLogger(AmberContainer.class.getName());

  private static final EnvironmentLocal<AmberContainer> _localContainer
    = new EnvironmentLocal<AmberContainer>();

  private EnvironmentClassLoader _parentLoader;
  private ClassLoader _tempLoader;
  // private EnhancingClassLoader _enhancedLoader;
  private AmberContainer _parentAmberContainer;

  private JClassLoader _jClassLoader;

  private AmberEnhancer _enhancer;

  private DataSource _dataSource;
  private DataSource _readDataSource;
  private DataSource _xaDataSource;

  private boolean _createDatabaseTables;

  private ArrayList<PersistenceUnitConfig> _unitConfigList
    = new ArrayList<PersistenceUnitConfig>();

  private HashMap<String,AmberPersistenceUnit> _unitMap
    = new HashMap<String,AmberPersistenceUnit>();

  private HashMap<String,EntityManagerFactory> _factoryMap
    = new HashMap<String,EntityManagerFactory>();

  private HashMap<String,EntityManager> _persistenceContextMap
    = new HashMap<String,EntityManager>();

  private HashMap<String,EmbeddableType> _embeddableMap
    = new HashMap<String,EmbeddableType>();

  private HashMap<String,EntityType> _entityMap
    = new HashMap<String,EntityType>();

  private HashMap<String,MappedSuperclassType> _mappedSuperclassMap
    = new HashMap<String,MappedSuperclassType>();

  private HashMap<String,ListenerType> _defaultListenerMap
    = new HashMap<String,ListenerType>();

  private HashMap<String, ArrayList<ListenerType>>
    _entityListenerMap = new HashMap<String, ArrayList<ListenerType>>();

  private Throwable _exception;

  private HashMap<String,Throwable> _embeddableExceptionMap
    = new HashMap<String,Throwable>();

  private HashMap<String,Throwable> _entityExceptionMap
    = new HashMap<String,Throwable>();

  private HashMap<String,Throwable> _listenerExceptionMap
    = new HashMap<String,Throwable>();

  private HashMap<Path,RootContext> _persistenceRootMap
    = new HashMap<Path,RootContext>();
  
  private ArrayList<RootContext> _pendingRootList
    = new ArrayList<RootContext>();
  
  private ArrayList<AmberPersistenceUnit> _pendingUnitList
    = new ArrayList<AmberPersistenceUnit>();

  private ArrayList<LazyEntityManagerFactory> _pendingFactoryList
    = new ArrayList<LazyEntityManagerFactory>();
  
  private HashSet<URL> _persistenceURLSet = new HashSet<URL>();

  private ArrayList<String> _pendingClasses = new ArrayList<String>();

  private AmberContainer(ClassLoader loader)
  {
    _parentAmberContainer = _localContainer.get(loader);
    _parentLoader = Environment.getEnvironmentClassLoader(loader);
    _localContainer.set(this, _parentLoader);

    _tempLoader = _parentLoader.getNewTempClassLoader();

    // --- ok
    
    // XXX: change after the 3.1.4 release
    _jClassLoader = EnhancerManager.create(_parentLoader).getJavaClassLoader();
    /*
    _jClassLoader
      = JClassLoaderWrapper.create(_parentLoader.getNewTempClassLoader());
    */

    _enhancer = new AmberEnhancer(this);

    EnhancerManager.create(_parentLoader).addClassEnhancer(_enhancer);

    if (_parentAmberContainer != null)
      copyContainerDefaults(_parentAmberContainer);

    _parentLoader.addScanListener(this);

    Environment.addEnvironmentListener(this, _parentLoader);

    try {
      if (_parentLoader instanceof DynamicClassLoader)
        ((DynamicClassLoader) _parentLoader).make();
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Returns the local container.
   */
  public static AmberContainer create()
  {
    return create(Thread.currentThread().getContextClassLoader());
  }

  /**
   * Returns the local container.
   */
  public static AmberContainer create(ClassLoader loader)
  {
    synchronized (_localContainer) {
      AmberContainer container = _localContainer.getLevel(loader);

      if (container == null) {
        container = new AmberContainer(loader);

        _localContainer.set(container, loader);
      }

      return container;
    }
  }

  /**
   * Returns the local container.
   */
  public static AmberContainer getCurrent()
  {
    return getCurrent(Thread.currentThread().getContextClassLoader());
  }

  /**
   * Returns the current environment container.
   */
  public static AmberContainer getCurrent(ClassLoader loader)
  {
    synchronized (_localContainer) {
      return _localContainer.get(loader);
    }
  }

  /**
   * Sets the primary data source.
   */
  public void setDataSource(DataSource dataSource)
  {
    _dataSource = dataSource;
  }

  /**
   * Gets the primary data source.
   */
  public DataSource getDataSource()
  {
    return _dataSource;
  }

  /**
   * Sets the read data source.
   */
  public void setReadDataSource(DataSource dataSource)
  {
    _readDataSource = dataSource;
  }

  /**
   * Gets the read data source.
   */
  public DataSource getReadDataSource()
  {
    return _readDataSource;
  }

  /**
   * Sets the xa data source.
   */
  public void setXADataSource(DataSource dataSource)
  {
    _xaDataSource = dataSource;
  }

  /**
   * Gets the XA data source.
   */
  public DataSource getXADataSource()
  {
    return _xaDataSource;
  }

  /**
   * True if database tables should be created automatically.
   */
  public boolean getCreateDatabaseTables()
  {
    return _createDatabaseTables;
  }

  /**
   * True if database tables should be created automatically.
   */
  public void setCreateDatabaseTables(boolean isCreate)
  {
    _createDatabaseTables = isCreate;
  }

  /**
   * Returns the parent loader
   */
  public ClassLoader getParentClassLoader()
  {
    return _parentLoader;
  }

  /**
   * Returns the parent loader
   */
  public ClassLoader getEnhancedLoader()
  {
    return _parentLoader;
  }

  /**
   * Returns the enhancer.
   */
  public AmberGenerator getGenerator()
  {
    return _enhancer;
  }

  /**
   * Returns the persistence unit JNDI context.
   */
  public static String getPersistenceUnitJndiPrefix()
  {
    return "java:comp/env/persistence/_amber_PersistenceUnit/";
  }

  /**
   * Returns the persistence unit JNDI context.
   */
  public static String getPersistenceContextJndiPrefix()
  {
    //return "java:comp/env/persistence/PersistenceContext/";
    return "java:comp/env/persistence/";
  }

  /**
   * Returns the JClassLoader.
   */
  public JClassLoader getJClassLoader()
  {
    return _jClassLoader;
  }

  private void copyContainerDefaults(AmberContainer parent)
  {
    _dataSource = parent._dataSource;
    _xaDataSource = parent._xaDataSource;
    _readDataSource = parent._readDataSource;
    _createDatabaseTables = parent._createDatabaseTables;
  }

  public void init()
  {
  }

  /**
   * Returns the EmbeddableType for an introspected class.
   */
  public EmbeddableType getEmbeddable(String className)
  {
    Throwable e = _embeddableExceptionMap.get(className);

    if (e != null)
      throw new AmberRuntimeException(e);
    else if (_exception != null) {
      throw new AmberRuntimeException(_exception);
    }

    return _embeddableMap.get(className);
  }

  /**
   * Returns the EntityType for an introspected class.
   */
  public EntityType getEntity(String className)
  {
    Throwable e = _entityExceptionMap.get(className);

    if (e != null)
      throw new AmberRuntimeException(e);
    else if (_exception != null) {
      throw new AmberRuntimeException(_exception);
    }

    return _entityMap.get(className);
  }

  /**
   * Returns the MappedSuperclassType for an introspected class.
   */
  public MappedSuperclassType getMappedSuperclass(String className)
  {
    Throwable e = _entityExceptionMap.get(className);

    if (e != null)
      throw new AmberRuntimeException(e);
    else if (_exception != null) {
      throw new AmberRuntimeException(_exception);
    }

    MappedSuperclassType type = _mappedSuperclassMap.get(className);

    return type;
  }

  /**
   * Returns the default ListenerType for an introspected class.
   */
  public ListenerType getDefaultListener(String className)
  {
    if (true)
      return null;
    
    Throwable e = _listenerExceptionMap.get(className);

    if (e != null)
      throw new AmberRuntimeException(e);
    else if (_exception != null) {
      throw new AmberRuntimeException(_exception);
    }

    return _defaultListenerMap.get(className);
  }

  /**
   * Returns the entity ListenerType for an introspected class.
   */
  public ListenerType getEntityListener(String className)
  {
    if (true)
      return null;
    
    Throwable e = _listenerExceptionMap.get(className);

    if (e != null)
      throw new AmberRuntimeException(e);
    else if (_exception != null) {
      throw new AmberRuntimeException(_exception);
    }

    ArrayList<ListenerType> listenerList;

    for (Map.Entry<String, ArrayList<ListenerType>>
           entry : _entityListenerMap.entrySet()) {

      listenerList = entry.getValue();

      if (listenerList == null)
        continue;

      for (ListenerType listener : listenerList) {
        if (className.equals(listener.getBeanClass().getName()))
          return listener;
      }
    }

    return null;
  }

  /**
   * Returns the listener for an introspected class.
   */
  public ListenerType getListener(String className)
  {
    if (true)
      return null;
    
    ListenerType listener = getDefaultListener(className);

    if (listener == null)
      listener = getEntityListener(className);

    return listener;
  }

  /**
   * Returns the entity listeners for an entity.
   */
  public ArrayList<ListenerType>
  getEntityListeners(String entityClassName)
  {
    return null;
    
    // return _entityListenerMap.get(entityClassName);
  }

  /**
   * Adds an entity for an introspected class.
   */
  public void addEntityException(String className, Throwable e)
  {
    _entityExceptionMap.put(className, e);
  }

  /**
   * Adds an entity for an introspected class.
   */
  public void addException(Throwable e)
  {
    if (_exception == null) {
      _exception = e;

      Environment.setConfigException(e);
    }
  }

  public Throwable getConfigException()
  {
    return _exception;
  }

  /**
   * Adds an embeddable for an introspected class.
   */
  public void addEmbeddable(String className, EmbeddableType type)
  {
    _embeddableMap.put(className, type);
  }

  /**
   * Adds an entity for an introspected class.
   */
  public void addEntity(String className, EntityType type)
  {
    _entityMap.put(className, type);
  }

  /**
   * Adds a mapped superclass for an introspected class.
   */
  public void addMappedSuperclass(String className,
                                  MappedSuperclassType type)
  {
    _mappedSuperclassMap.put(className, type);
  }

  /**
   * Adds a default listener.
   */
  public void addDefaultListener(String className,
                                 ListenerType type)
  {
    _defaultListenerMap.put(className, type);
  }

  /**
   * Adds an entity listener.
   */
  public void addEntityListener(String entityClassName,
                                ListenerType listenerType)
  {
    ArrayList<ListenerType> listenerList
      = _entityListenerMap.get(entityClassName);

    if (listenerList == null) {
      listenerList = new ArrayList<ListenerType>();
      _entityListenerMap.put(entityClassName, listenerList);
    }

    listenerList.add(listenerType);
  }

  /**
   * Initialize the entity homes.
   */
  public void initEntityHomes()
  {
    throw new UnsupportedOperationException();
  }

  public AmberPersistenceUnit createPersistenceUnit(String name)
  {
    AmberPersistenceUnit unit = new AmberPersistenceUnit(this, name);

    _unitMap.put(unit.getName(), unit);

    return unit;
  }

  public void start()
  {
    configurePersistenceRoots();
    startPersistenceUnits();
  }

  public AmberPersistenceUnit getPersistenceUnit(String name)
  {
    if (_exception != null)
      throw new AmberRuntimeException(_exception);

    return _unitMap.get(name);
  }

  public EntityManagerFactory getEntityManagerFactory(String name)
  {
    if (_exception != null)
      throw new AmberRuntimeException(_exception);

    EntityManagerFactory factory = _factoryMap.get(name);
    if (factory != null)
      return factory;

    if (_pendingRootList.size() > 0)
      configurePersistenceRoots();

    factory = _factoryMap.get(name);
    if (factory != null)
      return factory;
    
    AmberPersistenceUnit amberUnit = _unitMap.get(name);
    if (amberUnit != null) {
      factory = new AmberEntityManagerFactory(amberUnit);
      _factoryMap.put(name, factory);
      return factory;
    }

    if ("".equals(name) && _factoryMap.size() == 1)
      return _factoryMap.values().iterator().next();

    if ("".equals(name) && _unitMap.size() == 1) {
      amberUnit = _unitMap.values().iterator().next();
      
      factory = new AmberEntityManagerFactory(amberUnit);
      _factoryMap.put(name, factory);
      return factory;
    }

    if (_parentAmberContainer != null)
      return _parentAmberContainer.getEntityManagerFactory(name);
    else
      return null;
  }
  
  public EntityManager getPersistenceContext(String name)
  {
    if (_exception != null)
      throw new AmberRuntimeException(_exception);

    if ("".equals(name) && _unitConfigList.size() > 0)
      name = _unitConfigList.get(0).getName();

    EntityManager context = _persistenceContextMap.get(name);
    if (context != null)
      return context;

    if (_pendingRootList.size() > 0)
      configurePersistenceRoots();

    if ("".equals(name) && _unitConfigList.size() > 0)
      name = _unitConfigList.get(0).getName();

    context = _persistenceContextMap.get(name);
    if (context != null)
      return context;
    
    AmberPersistenceUnit amberUnit = _unitMap.get(name);
    if (amberUnit != null) {
      context = new EntityManagerProxy(amberUnit);
      _persistenceContextMap.put(name, context);
      
      return context;
    }

    return null;
  }
  
  public EntityManager getExtendedPersistenceContext(String name)
  {
    if (_exception != null)
      throw new AmberRuntimeException(_exception);

    if ("".equals(name) && _unitConfigList.size() > 0)
      name = _unitConfigList.get(0).getName();

    if (_pendingRootList.size() > 0)
      configurePersistenceRoots();

    if ("".equals(name) && _unitConfigList.size() > 0)
      name = _unitConfigList.get(0).getName();
    
    AmberPersistenceUnit amberUnit = _unitMap.get(name);
    if (amberUnit != null) {
      return new EntityManagerExtendedProxy(amberUnit);
    }

    return null;
  }

  /**
   * Adds a persistence root.
   */
  public void addPersistenceUnit(Path root)
  {
    if (_persistenceRootMap.get(root) != null)
      return;

    RootContext context = new RootContext(root);
    _persistenceRootMap.put(root, context);
    _pendingRootList.add(context);
  }
  
  /**
   * Adds the URLs for the classpath.
   */
  public void configurePersistenceRoots()
  {
    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();

    try {
      // jpa/1630
      // thread.setContextClassLoader(_tempLoader);
      thread.setContextClassLoader(_parentLoader);

      ArrayList<RootContext> rootList
	= new ArrayList<RootContext>(_pendingRootList);
      _pendingRootList.clear();
      
      for (RootContext rootContext : rootList) {
	configureRoot(rootContext);
      }
    } finally {
      thread.setContextClassLoader(oldLoader);
    }
  }

  private void configureRoot(RootContext rootContext)
  {
    Path root = rootContext.getRoot();

    try {
      Path ormXml = root.lookup("META-INF/orm.xml");

      EntityMappingsConfig entityMappings
        = configureMappingFile(root, ormXml);

      ArrayList<PersistenceUnitConfig> unitList = parsePersistenceConfig(root);

      if (unitList == null)
	return;

      HashMap<String, JClass> classMap = new HashMap<String, JClass>();

      for (PersistenceUnitConfig unitConfig : unitList) {
	Class provider = unitConfig.getProvider();
	if (provider != null
	    && ! AmberPersistenceProvider.class.equals(provider)) {
	  addProviderUnit(unitConfig);
	  
	  continue;
	}
	
        try {
	  if (log.isLoggable(Level.CONFIG))
	    log.config("Amber PersistenceUnit[" + unitConfig.getName() + "] configuring " + rootContext.getRoot().getURL());
	  
          if (! unitConfig.isExcludeUnlistedClasses()) {
	    classMap.clear();
	    
	    for (String className : rootContext.getClassNameList())
	      lookupClass(className, classMap, entityMappings);
	    
	    unitConfig.addAllClasses(classMap);
          }

          ArrayList<EntityMappingsConfig> entityMappingsList
            = new ArrayList<EntityMappingsConfig>();

          if (entityMappings != null)
            entityMappingsList.add(entityMappings);

          // jpa/0s2n: <jar-file>
          for (String fileName : unitConfig.getJarFiles()) {
            JarPath jarFile;

            Path parent = root;

            if (root instanceof JarPath) {
              parent = ((JarPath) root).getContainer().getParent();
            }

            jarFile = JarPath.create(parent.lookup(fileName));

            classMap.clear();

            // lookupJarClasses(jarFile, classMap, entityMappings);

            unitConfig.addAllClasses(classMap);
          }

          // jpa/0s2l: custom mapping-file.
          for (String fileName : unitConfig.getMappingFiles()) {
            Path mappingFile = root.lookup(fileName);

            EntityMappingsConfig mappingFileConfig
              = configureMappingFile(root, mappingFile);

            if (mappingFileConfig != null) {
              entityMappingsList.add(mappingFileConfig);

              classMap.clear();
	      /*
	      lookupClasses(root.getPath().length(), root, classMap,
                            mappingFileConfig);
	      */
              unitConfig.addAllClasses(classMap);
            }
          }

          AmberPersistenceUnit unit
	    = unitConfig.init(this, entityMappingsList);

	  _pendingUnitList.add(unit);

          _unitMap.put(unit.getName(), unit);
        } catch (Exception e) {
          addException(e);

          log.log(Level.WARNING, e.toString(), e);
        }
      }
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw ConfigException.create(e);
    }
  }
  
  /**
   * Adds the URLs for the classpath.
   */
  public void startPersistenceUnits()
  {
    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();

    try {
      // jpa/1630
      // thread.setContextClassLoader(_tempLoader);
      thread.setContextClassLoader(_parentLoader);

      ArrayList<AmberPersistenceUnit> unitList
	= new ArrayList<AmberPersistenceUnit>(_pendingUnitList);
      _pendingUnitList.clear();

      ArrayList<LazyEntityManagerFactory> lazyEmfList
	= new ArrayList<LazyEntityManagerFactory>(_pendingFactoryList);
      _pendingFactoryList.clear();
      
      for (LazyEntityManagerFactory lazyEmf : lazyEmfList) {
	lazyEmf.init();
      }
      
      for (AmberPersistenceUnit unit : unitList) {
	unit.initEntityHomes();
      }
    } finally {
      thread.setContextClassLoader(oldLoader);
    }
  }

  private void addProviderUnit(PersistenceUnitConfig unit)
  {
    try {
      Class cl = unit.getProvider();
	  
      if (log.isLoggable(Level.CONFIG)) {
	log.config("JPA PersistenceUnit[" + unit.getName() + "] handled by "
		   + cl.getName());
      }

      PersistenceProvider provider = (PersistenceProvider) cl.newInstance();

      String unitName = unit.getName();
      Map props = null;
      
      synchronized (this) {
	LazyEntityManagerFactory lazyFactory
	  = new LazyEntityManagerFactory(unit, provider, props);
	
	_pendingFactoryList.add(lazyFactory);
      }
      
      EntityManagerTransactionProxy persistenceContext
	= new EntityManagerTransactionProxy(this, unitName, props);
      
      _persistenceContextMap.put(unitName, persistenceContext);

      WebBeansContainer webBeans = WebBeansContainer.create(_parentLoader);
      webBeans.addComponent(new EntityManagerFactoryComponent(this, provider, unit));
      webBeans.addComponent(new PersistenceContextComponent(unitName, persistenceContext));
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw ConfigException.create(e);
    }
  }

  /**
   * Adds a persistence root.
   */
  private ArrayList<PersistenceUnitConfig> parsePersistenceConfig(Path root)
  {
    Path persistenceXml = root.lookup("META-INF/persistence.xml");

    if (! persistenceXml.canRead())
      return null;

    persistenceXml.setUserPath(persistenceXml.getURL());

    if (log.isLoggable(Level.FINE))
      log.fine(this + " parsing " + persistenceXml.getURL());

    InputStream is = null;

    try {
      is = persistenceXml.openRead();

      PersistenceConfig persistence = new PersistenceConfig();
      persistence.setRoot(root);

      new Config().configure(persistence, is,
                             "com/caucho/amber/cfg/persistence-30.rnc");

      ArrayList<PersistenceUnitConfig> unitList = persistence.getUnitList();

      _unitConfigList.addAll(unitList);

      return unitList;
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw LineConfigException.create(e);
    } finally {
      try {
        if (is != null)
          is.close();
      } catch (Exception e) {
      }
    }
  }

  //
  // private

  //
  // Configures the default orm.xml or mapping files specified with
  // mapping-file tags within a persistence-unit.
  //
  private EntityMappingsConfig configureMappingFile(Path root,
                                                    Path xmlFile)
    throws Exception
  {
    EntityMappingsConfig entityMappings = null;

    if (xmlFile.exists()) {
      InputStream is = xmlFile.openRead();

      entityMappings = new EntityMappingsConfig();
      entityMappings.setRoot(root);

      new Config().configure(entityMappings, is,
                             "com/caucho/amber/cfg/mapping-30.rnc");
    }

    return entityMappings;
  }

  private void lookupClass(String className,
                           HashMap<String, JClass> classMap,
                           EntityMappingsConfig entityMappings)
    throws Exception
  {
    JClass type = _jClassLoader.forName(className);

    if (type != null) {
      boolean isEntity
        = type.getAnnotation(javax.persistence.Entity.class) != null;
      boolean isEmbeddable
        = type.getAnnotation(javax.persistence.Embeddable.class) != null;
      boolean isMappedSuperclass
        = type.getAnnotation(javax.persistence.MappedSuperclass.class) != null;

      MappedSuperclassConfig mappedSuperclassOrEntityConfig = null;

      if (entityMappings != null) {
        mappedSuperclassOrEntityConfig = entityMappings.getEntityConfig(className);

        if (mappedSuperclassOrEntityConfig == null)
          mappedSuperclassOrEntityConfig = entityMappings.getMappedSuperclass(className);
      }

      if (isEntity || isEmbeddable || isMappedSuperclass
	  || (mappedSuperclassOrEntityConfig != null)) {
        classMap.put(className, type);
      }
    }
  }

  //
  // ScanListener
  //

  /**
   * Returns true if the root is a valid scannable root.
   */
  public boolean isRootScannable(Path root)
  {
    if (! root.lookup("META-INF/persistence.xml").canRead())
      return false;

    RootContext context = _persistenceRootMap.get(root);

    if (context == null) {
      context = new RootContext(root);
      _pendingRootList.add(context);
      _persistenceRootMap.put(root, context);
    }

    if (context.isScanComplete())
      return false;
    else {
      if (log.isLoggable(Level.FINER))
	log.finer(this + " scanning " + root);
      
      context.setScanComplete(true);
      
      return true;
    }
  }

  public boolean isScanMatch(CharBuffer annotationName)
  {
    if (annotationName.matches("javax.persistence.Entity"))
      return true;
    else if (annotationName.matches("javax.persistence.Embeddable"))
      return true;
    else if (annotationName.matches("javax.persistence.MappedSuperclass"))
      return true;
    else
      return false;
  }
  
  /**
   * Callback to note the class matches
   */
  public void classMatchEvent(EnvironmentClassLoader loader,
			      Path root,
			      String className)
  {
    RootContext context = _persistenceRootMap.get(root);

    if (context == null) {
      context = new RootContext(root);
      _persistenceRootMap.put(root, context);
      _pendingRootList.add(context);
    }
      
    context.addClassName(className);
  }

  //
  // EnvironmentListener
  //
  
  /**
   * Handles the environment config phase
   */
  public void environmentBind(EnvironmentClassLoader loader)
  {
    configurePersistenceRoots();
  }
  
 /**
   * Handles the case where the environment is starting (after init).
   */
  public void environmentStart(EnvironmentClassLoader loader)
  {
    start();
  }

  /**
   * Handles the case where the environment is stopping
   */
  public void environmentStop(EnvironmentClassLoader loader)
  {
  }

  public String toString()
  {
    return "AmberContainer[" + _parentLoader.getId() + "]";
  }

  class LazyEntityManagerFactory {
    private final PersistenceUnitConfig _unit;
    private final PersistenceProvider _provider;
    private final Map _props;

    LazyEntityManagerFactory(PersistenceUnitConfig unit,
			     PersistenceProvider provider,
			     Map props)
    {
      _unit = unit;
      _provider = provider;
      _props = props;
    }

    void init()
    {
      synchronized (AmberContainer.this) {
	String unitName = _unit.getName();
	
	EntityManagerFactory factory = _factoryMap.get(unitName);

	if (factory == null) {
	  factory
	    = _provider.createContainerEntityManagerFactory(_unit, _props);

	  if (factory == null)
	    throw new ConfigException(L.l("'{0}' must return an EntityManagerFactory",
					  _provider.getClass().getName()));

	  if (log.isLoggable(Level.FINE)) {
	    log.fine(L.l("Amber creating persistence unit '{0}' created with provider '{1}'",
			 unitName, _provider.getClass().getName()));
	  }

	  _factoryMap.put(unitName, factory);
	}
      }
    }
  }
}
