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

package com.caucho.boot;

import com.caucho.config.Config;
import com.caucho.config.ConfigException;
import com.caucho.loader.*;
import com.caucho.server.resin.ResinELContext;
import com.caucho.util.L10N;
import com.caucho.vfs.Path;
import com.caucho.vfs.Vfs;
import com.caucho.Version;

import com.caucho.webbeans.manager.WebBeansContainer;
import java.io.*;
import java.lang.management.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.*;

/**
 * ResinBoot is the main bootstrap class for Resin.  It parses the
 * resin.conf and looks for the &lt;server> block matching the -server
 * argument.
 *
 * <h3>Start Modes:</h3>
 *
 * The start modes are STATUS, DIRECT, START, STOP, KILL, RESTART, SHUTDOWN.
 *
 * <ul>
 * <li>DIRECT starts a <server> from the command line
 * <li>START starts a <server> with a Watchdog in the background
 * <li>STOP stop the <server> Resin in the background
 * </ul>
 */
public class ResinBoot {
  private static L10N _L;
  private static Logger _log;

  private WatchdogArgs _args;

  private WatchdogClient _client;

  ResinBoot(String []argv)
    throws Exception
  {
    _args = new WatchdogArgs(argv);
    
    Path resinHome = _args.getResinHome();

    ClassLoader loader = ProLoader.create(resinHome);
    if (loader != null) {
      System.setProperty("resin.home", resinHome.getNativePath());
      
      Thread.currentThread().setContextClassLoader(loader);

      Vfs.initJNI();

      resinHome = Vfs.lookup(resinHome.getFullPath());
      _args.setResinHome(resinHome);
    }
    
    Environment.init();
    
    // required for license check
    System.setProperty("resin.home", resinHome.getNativePath());

    // watchdog/0210
    // Vfs.setPwd(_rootDirectory);

    if (! _args.getResinConf().canRead()) {
      throw new ConfigException(L().l("Resin/{0} can't open configuration file '{1}'",
                                      Version.VERSION,
                                      _args.getResinConf().getNativePath()));
    }

    // XXX: set _isResinProfessional

    Config config = new Config();
    BootManager bootManager = new BootManager(_args);

    ResinELContext elContext = _args.getELContext();

    /**
     * XXX: the following setVar calls should not be necessary, but the
     * EL.setEnviornment() call above is not effective:
     */
    WebBeansContainer webBeans = WebBeansContainer.create();
    webBeans.addSingletonByName(elContext.getResinHome(), "resinHome");
    webBeans.addSingletonByName(elContext.getJavaVar(), "java");
    webBeans.addSingletonByName(elContext.getResinVar(), "resin");
    webBeans.addSingletonByName(elContext.getServerVar(), "server");

    config.configure(bootManager, _args.getResinConf(),
                     "com/caucho/server/resin/resin.rnc");

    _client = bootManager.findClient(_args.getServerId());

    if (_client == null)
      throw new ConfigException(L().l("Resin/{0}: -server '{1}' does not match any defined <server>\nin {2}.",
                                      Version.VERSION, _args.getServerId(), _args.getResinConf()));

    Path logDirectory = bootManager.getLogDirectory();
    if (! logDirectory.exists()) {
      logDirectory.mkdirs();

      if (_client.getUserName() != null)
	logDirectory.changeOwner(_client.getUserName());
      
      if (_client.getGroupName() != null)
	logDirectory.changeOwner(_client.getGroupName());
    }
  }

  boolean start()
    throws Exception
  {
    if (_args.isStatus()) {
      try {
	String status = _client.statusWatchdog();
	
	System.out.println(L().l("Resin/{0} status for watchdog at {1}:{2}",
				 Version.VERSION,
				 _client.getWatchdogAddress(),
				 _client.getWatchdogPort()));
        System.out.println(status);
      } catch (Exception e) {
	System.out.println(L().l("Resin/{0} can't start -server '{1}' for watchdog at {2}:{3}.\n{4}",
				 Version.VERSION, _client.getId(),
				 _client.getWatchdogAddress(),
				 _client.getWatchdogPort(),
				 e.toString()));

	log().log(Level.FINE, e.toString(), e);

	System.exit(1);
      }
      
      return false;
    }
    else if (_args.isStart()) {
      try {
	_client.startWatchdog(_args.getArgv());
	
	System.out.println(L().l("Resin Modified By DC INC/{0} started for watchdog at {2}:{3}",
				 Version.VERSION, _client.getId(),
				 _client.getWatchdogAddress(),
				 _client.getWatchdogPort()));
      } catch (Exception e) {
	System.out.println(L().l("Resin Modified By DC INC/{0} can't start -server '{1}' for watchdog at {2}:{3}.\n{4}",
				 Version.VERSION, _client.getId(),
				 _client.getWatchdogAddress(),
				 _client.getWatchdogPort(),
				 e.toString()));

	log().log(Level.FINE, e.toString(), e);

	System.exit(1);
      }
      
      return false;
    }
    else if (_args.isStop()) {
      try {
	_client.stopWatchdog();
	
	System.out.println(L().l("Resin Modified By DC INC/{0} stopped -server '{1}' for watchdog at {2}:{3}",
				 Version.VERSION, _client.getId(),
				 _client.getWatchdogAddress(),
				 _client.getWatchdogPort()));
      } catch (Exception e) {
	System.out.println(L().l("Resin Modified By DC INC/{0} can't stop -server '{1}' for watchdog at {2}:{3}.\n{4}",
				 Version.VERSION, _client.getId(),
				 _client.getWatchdogAddress(),
				 _client.getWatchdogPort(),
				 e.toString()));

	log().log(Level.FINE, e.toString(), e);

	System.exit(1);
      }
      
      return false;
    }
    else if (_args.isKill()) {
      try {
	_client.killWatchdog();
	
	System.out.println(L().l("Resin Modified By DC INC/{0} killed -server '{1}' for watchdog at {2}:{3}",
				 Version.VERSION, _client.getId(),
				 _client.getWatchdogAddress(),
				 _client.getWatchdogPort()));
      } catch (Exception e) {
	System.out.println(L().l("Resin Modified By DC INC/{0} can't kill -server '{1}' for watchdog at {2}:{3}.\n{4}",
				 Version.VERSION, _client.getId(),
				 _client.getWatchdogAddress(),
				 _client.getWatchdogPort(),
				 e.toString()));

	log().log(Level.FINE, e.toString(), e);

	System.exit(1);
      }
      
      return false;
    }
    else if (_args.isRestart()) {
      try {
	_client.restartWatchdog(_args.getArgv());
	
	System.out.println(L().l("Resin Modified By DC INC/{0} stopped -server '{1}' for watchdog at {2}:{3}",
				 Version.VERSION, _client.getId(),
				 _client.getWatchdogAddress(),
				 _client.getWatchdogPort()));
      } catch (Exception e) {
	System.out.println(L().l("Resin Modified By DC INC/{0} can't restart -server '{1}'.\n{2}",
				 Version.VERSION, _client.getId(),
				 e.toString()));

	log().log(Level.FINE, e.toString(), e);

	System.exit(1);
      }
      
      return false;
    }
    else if (_args.isShutdown()) {
      try {
	_client.shutdown();

	System.err.println(L().l("Resin Modified By DC INC/{0} shutdown ResinWatchdogManager",
				 Version.VERSION));
      } catch (Exception e) {
	System.err.println(L().l("Resin Modified By DC INC/{0} can't shutdown ResinWatchdogManager.\n{1}",
				 Version.VERSION, e.toString()));

	log().log(Level.FINE, e.toString(), e);

	System.exit(1);
      }

      return false;
    }
    else if (_args.isSingle()) {
      return _client.startSingle() != 0;
    }
    else {
      throw new IllegalStateException(L().l("Unknown start mode"));
    }
  }

  /**
   * The main start of the web server.
   *
   * <pre>
   * -conf resin.conf  : alternate configuration file
   * -server web-a     : &lt;server> to start
   * <pre>
   */

  public static void main(String []argv)
  {
    try {
      ResinBoot boot = new ResinBoot(argv);

      while (boot.start()) {
	try {
	  synchronized (boot) {
	    boot.wait(5000);
	  }
	} catch (Exception e) {
	}
      }
    } catch (Exception e) {
      if (e instanceof ConfigException) {
	System.out.println(e.getMessage());

	System.exit(2);
      }
      else {
	e.printStackTrace();
      
	System.exit(3);
      }
    }
  }

  private static L10N L()
  {
    if (_L == null)
      _L = new L10N(ResinBoot.class);

    return _L;
  }

  private static Logger log()
  {
    if (_log == null)
      _log = Logger.getLogger(ResinBoot.class.getName());

    return _log;
  }
}
