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
 * @author Charles Reich
 */

package com.caucho.quercus.lib.db;

import com.caucho.quercus.UnimplementedException;
import com.caucho.quercus.annotation.Optional;
import com.caucho.quercus.annotation.ReturnNullAsFalse;
import com.caucho.quercus.env.BooleanValue;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.LongValue;
import com.caucho.quercus.env.StringValue;
import com.caucho.quercus.env.UnicodeValueImpl;
import com.caucho.quercus.env.Value;
import com.caucho.util.L10N;

import java.sql.Connection;
import java.sql.DataTruncation;
import java.sql.DatabaseMetaData;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * mysqli object oriented API facade
 */
public class Mysqli extends JdbcConnectionResource {
  private static final Logger log = Logger.getLogger(Mysqli.class.getName());
  private static final L10N L = new L10N(Mysqli.class);

  /**
   * mysqli_multi_query populates _resultValues
   * NB: any updates (ie: INSERT, UPDATE, DELETE) will
   * have the update counts ignored.
   *
   * Has been stored tells moreResults whether the
   * _nextResultValue has been stored already.
   * If so, more results will return true only if
   * there is another result.
   *
   * _hasBeenStored is set to true by default.
   * if _hasBeenUsed == false, then
   * _resultValues.get(_nextResultValue)
   * is ready to be used by the next call to
   * mysqli_store_result or mysqli_use_result.
   */
  private ArrayList<JdbcResultResource> _resultValues
    = new ArrayList<JdbcResultResource>();
  private int _nextResultValue = 0;
  private boolean _hasBeenUsed = true;

  private static String _checkedDriverVersion = null;
  private static Object _checkDriverLock = new Object();

  private boolean _isPersistent = false;

  private boolean _lastSQLWasUpdate = false;

  /**
    * This is the constructor for the mysqli class.
    * It can be invoked by PHP or and by Java code.
    */

  public Mysqli(Env env,
                @Optional("localhost") StringValue host,
                @Optional StringValue user,
                @Optional StringValue password,
                @Optional StringValue db,
                @Optional("3306") int port,
                @Optional StringValue socket)
  {
    super(env);

    String hostStr;

    if (host.length() == 0)
      hostStr = "localhost";
    else
      hostStr = host.toString();

    connectInternal(env, hostStr, user.toString(), password.toString(),
                    db.toString(), port, socket.toString(),
                    0, null, null);
  }

  /**
   * This constructor can only be invoked by other method
   * implementations in the mysql and mysqli modules. It
   * accepts String arguments and supports additional
   * arguments not available in the mysqli constructor.
   */

  Mysqli(Env env,
         String host,
         String user,
         String password,
         String db,
         int port,
         String socket,
         int flags,
         String driver,
         String url)
  {
    super(env);

    if (host == null || host.length() == 0)
      host = "localhost";

    connectInternal(env, host, user, password, db, port, socket,
		    flags, driver, url);
  }

  protected Mysqli(Env env)
  {
    super(env);
  }
  
  /**
   * Verify that the ConnectorJ driver version is 3.1.14 or newer.
   * Older versions of this driver return incorrect type information
   * and suffer from encoding related bugs.
   */
  protected static void checkDriverVersion(Env env, Connection conn)
    throws SQLException
  {
    if (_checkedDriverVersion != null)
      return;
    
    synchronized(_checkDriverLock) {
      // required if statement to prevent multiple checks
      if (_checkedDriverVersion == null) {
        _checkedDriverVersion = checkDriverVersionImpl(env, conn);

        if (_checkedDriverVersion.length() != 0)
          return;
        
        String message = "Unable to detect MySQL Connector/J JDBC driver " +
                         "version.  The recommended JDBC version is 3.1.14.";

        log.log(Level.WARNING, message);
        env.warning(message);
      }
    }
  }


  private static String checkDriverVersionImpl(Env env, Connection conn)
    throws SQLException
  {
    DatabaseMetaData databaseMetaData = null;

    try {
      databaseMetaData = conn.getMetaData();
    } catch (SQLException e) {
      log.log(Level.FINEST, e.toString(), e);
    }

    // If getMetaData() returns null or raises a SQLException,
    // then we can't verify the driver version.

    if (databaseMetaData == null)
      return "";

    String fullVersion = null;

    try {
      fullVersion = databaseMetaData.getDriverVersion();
    } catch (SQLException e) {
      log.log(Level.FINEST, e.toString(), e);
    }

    // If getDriverVersion() returns null or raises a SQLException,
    // then we can't verify the driver version.

    if (fullVersion == null) {
      return "";
    }

    String version = fullVersion;

    // Extract full version number.

    int start;
    int end = version.indexOf(' ');

    String checkedDriverVersion = "";
    
    if (end != -1) {
      version = version.substring(0, end);

      start = version.lastIndexOf('-');

      if (start != -1) {
        version = version.substring(start + 1);

        // version string should look like "3.1.14"
        int major;
        int minor;
        int release;

        start = version.indexOf('.');
        end = version.lastIndexOf('.');

        major = Integer.valueOf(version.substring(0, start));
        minor = Integer.valueOf(version.substring(start+1, end));
        release = Integer.valueOf(version.substring(end+1));

        checkedDriverVersion = major + "." + minor + "." + release;
        
        if (major == 3 && (minor > 1 || minor == 1 && release >= 14)) {
        }
        else if (major > 3) {
          String message = L.l("Your MySQL Connector/J JDBC {0} driver may " +
                               "have issues with column/table aliases and " +
                               "DESCRIBE statements.  The recommended " +
                               "JDBC version is 3.1.14.", version);
          
          log.log(Level.WARNING, message);
          env.warning(message);
        }
        else {
          String message = L.l("Your MySQL Connector/J JDBC {0} driver may " +
                               "have issues with character encoding.  The " +
                               "recommended JDBC version is 3.1.14.", version);

         log.log(Level.WARNING, message);
         env.warning(message);
        }
      }
    }
    
    return checkedDriverVersion;
  }

  public String getResourceType()
  {
    return "mysql link";
  }

  /**
   * Connects to the underlying database.
   */
  protected Connection connectImpl(Env env,
                                   String host,
                                   String userName,
                                   String password,
                                   String dbname,
                                   int port,
                                   String socket,
                                   int flags,
                                   String driver,
                                   String url)
  {
    if (isConnected()) {
      env.warning(L.l("Connection is already opened to '{0}'", this));
      return null;
    }

    if (port <= 0) {
      port = 3306;
    }

    try {
      if (host == null || host.equals("")) {
        host = "localhost";
      }

      if (driver == null || driver.equals("")) {
        driver = "com.mysql.jdbc.jdbc2.optional.MysqlConnectionPoolDataSource";
      }

      if (url == null || url.equals("")) {
        StringBuilder urlBuilder = new StringBuilder();
  
        urlBuilder.append("jdbc:mysql://");
        urlBuilder.append(host);
        urlBuilder.append(":");
        urlBuilder.append(port);
        urlBuilder.append("/");
        urlBuilder.append(dbname);

        url = "jdbc:mysql://" + host + ":" + port + "/" + dbname;

        // Ignore MYSQL_CLIENT_LOCAL_FILES and MYSQL_CLIENT_IGNORE_SPACE flags.

        if ((flags & MysqliModule.MYSQL_CLIENT_INTERACTIVE) != 0) {
          char sep = (urlBuilder.indexOf("?") < 0) ? '?' : '&';

          urlBuilder.append(sep);
          urlBuilder.append("interactiveClient=true");
        }

        if ((flags & MysqliModule.MYSQL_CLIENT_COMPRESS) != 0) {
          char sep = (urlBuilder.indexOf("?") < 0) ? '?' : '&';

          urlBuilder.append(sep);
          urlBuilder.append("useCompression=true");
        }

        if ((flags & MysqliModule.MYSQL_CLIENT_SSL) != 0) {
          char sep = (urlBuilder.indexOf("?") < 0) ? '?' : '&';

          urlBuilder.append(sep);
          urlBuilder.append("useSSL=true");
        }

	    // Explicitly indicate that iso-8859-1 encoding should
        // be used as the default driver encoding. We don't want the
        // driver to use its version of Cp1252 because that encoding
        // does not support byte values in the range 0x80 to 0x9f.
        //
        // php/144b
        char sep = urlBuilder.indexOf("?") < 0 ? '?' : '&';
        urlBuilder.append(sep + "characterEncoding=ISO8859_1");
        //urlBuilder.append("&useInformationSchema=true");
        
        // required to get the result table name alias,
        // doesn't work in mysql JDBC 5.1.6, but set it anyways in case
        // the mysql guys fix it
        //
        // php/141p
        urlBuilder.append("&useOldAliasMetadataBehavior=true");
	
        url = urlBuilder.toString();
      }
      Connection jConn = env.getConnection(driver, url, userName, password);

      checkDriverVersion(env, jConn);

      return jConn;
    } catch (SQLException e) {
      env.warning(L.l("A link to the server could not be established.\n  url={0}\n  driver={1}\n  {2}", url, driver, e.toString()), e);

      env.setSpecialValue("mysqli.connectErrno",new LongValue(e.getErrorCode()));
      env.setSpecialValue("mysqli.connectError", new UnicodeValueImpl(e.getMessage()));

      return null;
    } catch (Exception e) {
      env.warning(L.l("A link to the server could not be established.\n  url={0}\n  driver={1}\n  {2}", url, driver, e.toString()));
      env.setSpecialValue("mysqli.connectError", new UnicodeValueImpl(e.toString()));

      return null;
    }
  }

  /**
   * Quercus function to get the field 'affected_rows'.
   */

  public int getaffected_rows()
  {
    return affected_rows();
  }
  
  /**
   * returns the number of affected rows.
   */
  public int affected_rows()
  {
    return validateConnection().getAffectedRows();
  }

  /**
   * sets the autocommit mode
   */
  public boolean autocommit(boolean isAutoCommit)
  {
    return validateConnection().setAutoCommit(isAutoCommit);
  }

  /**
   * Changes the user and database
   *
   * @param user the new user
   * @param password the new password
   * @param db the new database
   */
  public boolean change_user(StringValue user, StringValue password, StringValue db)
  {
    // XXX: Docs for mysqli_change_user indicate that if new user authorization fails,
    // then the existing user perms are retained.

    close(getEnv());
    
    String userStr;
    String passwordStr;
    String dbStr;

    if (user.length() == 0) {
      userStr = getUserName();
    } else {
      userStr = user.toString();
    }

    if (password.length() == 0)
      passwordStr = getPassword();
    else
      passwordStr = password.toString();

    if (db.length() == 0)
      dbStr = getCatalog().toString();
    else
      dbStr = db.toString();

    return connectInternal(getEnv(), _host, userStr, passwordStr,
                           dbStr, _port, _socket, _flags, _driver, _url);
  }

  /**
   * Returns the client encoding.
   *
   * XXX: stubbed out. has to be revised once we
   * figure out what to do with character encoding
   */
  public StringValue character_set_name(Env env)
  {
    return env.createString(getCharacterSetName());
  }

  /**
   * Alias for character_set_name
   */
  public StringValue client_encoding(Env env)
  {
    return character_set_name(env);
  }

  /**
   * Quercus function to get the field 'errno'.
   */
  public int geterrno()
  {
    return errno();
  }
  
  /**
   * Returns the error code for the most recent function call
   */
  public int errno()
  {
    if (isConnected())
      return getErrorCode();
    else
      return 0;
  }

  /**
   * Quercus function to get the field 'error'.
   */
  public StringValue geterror(Env env)
  {
    return error(env);
  }

  /**
   * Escapes the string
   */
  public StringValue escape_string(StringValue str)
  {
    return real_escape_string(str);
  }

  /**
   * Quercus function to get the field 'client_info'.
   */
  public StringValue getclient_info(Env env)
  {
    return getClientInfo(env);
  }

  /**
   * Returns the client information.
   */

  static StringValue getClientInfo(Env env)
  {
    String version = env.getQuercus().getMysqlVersion();

    if (version != null) {
      // php/1f2h

      // Initialized to a specific version via:
      // <init mysql-version="X.X.X">
    } else {
      // php/142h

      if (_checkedDriverVersion != null && _checkedDriverVersion != "") {
        // A connection has already been made and the driver
        // version has been validated.

        version = _checkedDriverVersion;
      } else {
        // A connection has not been made or a valid driver
        // version was not found. The JDBC API provides no
        // way to get the release number without a connection,
        // so just grab the major and minor number and use
        // zero for the release number.

        try {
          Driver driver = DriverManager.getDriver("jdbc:mysql://localhost/");

          version = driver.getMajorVersion() + "." +
                    driver.getMinorVersion() + ".00";
        }
        catch (SQLException e) {
          version = "0.00.00";
        }
      }
    }

    return env.createString(version);
  }

  /**
   * Quercus function to get the field 'client_version'.
   */
  public int getclient_version(Env env)
  {
    return MysqliModule.mysqli_get_client_version(env);
  }
  
  /**
   * Returns the database name.
   */
  public Value get_dbname(Env env)
  {
    return getCatalog();
  }
  
  /**
   * Quercus function to get the field 'host_info'.
   */
  public StringValue gethost_info(Env env)
  {
    return get_host_info(env);
  }
  
  /**
   * Returns the host information.
   */
  public StringValue get_host_info(Env env)
  {
    return env.createString(getHost() + " via TCP socket");
  }

  /**
   * Returns the host name.
   */
  public StringValue get_host_name(Env env)
  {
    return env.createString(getHost());
  }

  /**
   * Quercus function to get the field 'info'.
   */
  public Value getinfo(Env env)
  {
    return info(env);
  }

  /**
   * Return info string about the most recently executed
   * query. Documentation for mysql_info() indicates that
   * only some kinds of INSERT, UPDATE, LOAD, and ALTER
   * statements return results. A SELECT statement always
   * returns FALSE. The ConnectorJ module should provide a
   * way to get this result string since it is read from
   * the server, but that is not supported. This function
   * errors on the side of returning more results than
   * it should since it is an acceptable compromise.
   */

  Value info(Env env) {
    if (getResultResource() != null) {
      // Last SQL statement was a SELECT

      return BooleanValue.FALSE;
    }

    // INSERT result:      "Records: 23 Duplicates: 0 Warnings: 0"
    // LOAD result:        "Records: 42 Deleted: 0 Skipped: 0 Warnings: 0"
    // ALTER TABLE result: "Records: 60 Duplicates: 0 Warnings: 0"
    // UPDATE result:      "Rows matched: 1  Changed: 1  Warnings: 0"

    StringBuilder buff = new StringBuilder();

    int matched = affected_rows();
    int changed = matched;
    int duplicates = 0;
    int warnings = 0;

    SQLWarning warning = getWarnings();
    while (warning != null) {
      warning = warning.getNextWarning();
      warnings++;
    }

    if (_lastSQLWasUpdate)
      buff.append("Rows matched: ");
    else
      buff.append("Records: ");

    buff.append(matched);

    if (_lastSQLWasUpdate) {
      buff.append("  Changed: "); // PHP adds 2 spaces before Changed:
      buff.append(changed);
    } else {
      buff.append(" Duplicates: ");
      buff.append(duplicates);
    }

    if (_lastSQLWasUpdate)
      buff.append("  Warnings: "); // Only update has 2 spaces here
    else
      buff.append(" Warnings: ");

    buff.append(warnings);

    return env.createString(buff.toString());
  }

  /**
   * Returns the port number.
   */
  public int get_port_number()
  {
    return getPort();
  }

  /**
   * Quercus function to get the field 'protocol_version'.
   */
  public int getprotocol_version()
  {
    return get_proto_info();
  }
  
  /**
   * Returns the protocol information.
   */
  public int get_proto_info()
  {
    return 10;
  }

  /**
   * Quercus function to get the field 'server_info'.
   */
  public StringValue getserver_info(Env env)
  {
    return get_server_info(env);
  }
  
  /**
   * Returns the server information.
   */
  public StringValue get_server_info(Env env)
  {
    try {
      return env.createString(validateConnection().getServerInfo());
    } catch (SQLException e) {
      return env.createEmptyString();
    }
  }

  /**
   * Quercus function to get the field 'server_version'.
   */
  public int getserver_version()
  {
    return get_server_version();
  }

  /**
   * Returns the server information.
   */
  public int get_server_version()
  {
    try {
      String info = validateConnection().getServerInfo();

      return infoToVersion(info);
    } catch (SQLException e) {
      return 0;
    }
  }

  /**
   * Quercus function to get the field 'field_count'.
   */
  public int getfield_count()
  {
    return field_count();
  }
  
  /**
   * Returns the number of columns in the last query.
   */
  public int field_count()
  {
    return validateConnection().getFieldCount();
  }

  /**
   * Quercus function to get the field 'insert_id'.
   */
  public Value getinsert_id(Env env)
  {
    return insert_id(env);
  }
  
  /**
   * returns ID generated for an AUTO_INCREMENT column by the previous
   * INSERT query on success, 0 if the previous query does not generate
   * an AUTO_INCREMENT value, or FALSE if no MySQL connection was established
   *
   */
  public Value insert_id(Env env)
  {
    try {
      JdbcConnectionResource connV = validateConnection();
      Connection conn = connV.getConnection(env);

      if (conn == null)
        return BooleanValue.FALSE;
      
      Statement stmt = null;

      try {
        stmt = conn.createStatement();

        ResultSet rs = stmt.executeQuery("SELECT @@identity");

        if (rs.next())
          return LongValue.create(rs.getLong(1));
        else
          return BooleanValue.FALSE;
      } finally {
        if (stmt != null)
          stmt.close();
      }
    } catch (SQLException e) {
      log.log(Level.FINE, e.toString(), e);
      return BooleanValue.FALSE;
    }
  }

  @ReturnNullAsFalse
  public JdbcResultResource list_dbs()
  {
    return validateConnection().getCatalogs();
  }

  /**
   * Check for more results in a multi-query
   */
  public boolean more_results()
  {
    return ((Mysqli) validateConnection()).moreResults();
  }

  /**
   * executes one or multiple queries which are
   * concatenated by a semicolon.
   */
  public boolean multi_query(Env env, StringValue query)
  {
    return ((Mysqli) validateConnection()).multiQuery(env, query);
  }

  /**
   * prepares next result set from a previous call to
   * mysqli_multi_query
   */
  public boolean next_result()
  {
    return ((Mysqli) validateConnection()).nextResult();
  }

  /**
   * Sets a mysqli option.
   */
  public boolean options(int option, Value value)
  {
    return false;
  }

  /**
   * Executes a query.
   *
   * @param env the PHP executing environment
   * @param sql the escaped query string (can contain escape sequences like `\n' and `\Z')
   * @param resultMode ignored
   *
   * @return a {@link JdbcResultResource}, or null for failure
   */
  public Value query(Env env,
                     StringValue sql,
                     @Optional("MYSQLI_STORE_RESULT") int resultMode)
  {
    return realQuery(env, sql.toString());
  }

  /**
   * Intercept Mysql specific query before sending to JDBC driver
   * to handle any special cases.
   */

  @Override
  protected Value realQuery(Env env, String sql)
  {
    clearErrors();

    _lastSQLWasUpdate = false;

    setResultResource(null);

    try {
      // Check for valid conneciton

      Connection conn = getConnection(env);
      
      if (conn == null)
        return BooleanValue.FALSE;

      SqlParseToken tok = parseSqlToken(sql, null);

      if (tok != null &&
          tok.matchesFirstChar('U', 'u') &&
          tok.matchesToken("USE")) {
        // Mysql "USE DBNAME" statement.
        //
        // The Mysql JDBC driver does not properly implement getCatalog()
        // when calls to setCatalog() are mixed with SQL USE statements.
        // Work around this problem by translating a SQL USE statement
        // into a JDBC setCatalog() invocation. The setCatalog() API in
        // the ConnectorJ implementation just creates a USE statement
        // anyway. This also makes sure the database pool logic knows
        // which database is currently selected. If a second call to
        // select the current database is found, it is a no-op.

        tok = parseSqlToken(sql, tok);

        if (tok != null) {
          String dbname = tok.toUnquotedString();

          setCatalog(dbname);

          return BooleanValue.TRUE;
        }
      }
      else if (tok != null &&
          tok.matchesFirstChar('U', 'u') &&
          tok.matchesToken("UPDATE")) {
        // SQL UPDATE statement

        _lastSQLWasUpdate = true;
      }

      return super.realQuery(env, sql);
    } catch (SQLException e) {
      saveErrors(e);

      log.log(Level.FINEST, e.toString(), e);
      return BooleanValue.FALSE;
    } catch (IllegalStateException e) {
      log.log(Level.FINEST, e.toString(), e);
      
      // #2184, some drivers return this on closed connection
      saveErrors(new SQLExceptionWrapper(e));

      return BooleanValue.FALSE;
    }
  }

  /**
   * Execute an single query against the database whose result can then be retrieved
   * or stored using the mysqli_store_result() or mysqli_use_result() functions.
   *
   * @param env the PHP executing environment
   * @param query the escaped query string (can contain escape sequences like `\n' and `\Z')
   */
  public boolean real_query(Env env,
                            StringValue query)
  {
    // Assume that the query argument contains just one query. Reuse the
    // result management logic in multiQuery(), so that a future call to
    // mysqli_store_result() will work as expected.

    return multiQuery(env, query);
  }

  /**
   * returns a prepared statement or null on error.
   */
  public MysqliStatement prepare(Env env, StringValue query)
  {
    MysqliStatement stmt = new MysqliStatement((Mysqli) validateConnection());

    boolean result = stmt.prepare(env, query);

    if (! result) {
      stmt.close();
      return null;
    }

    return stmt;
  }

  /**
   * Connects to the underlying database.
   */
  public boolean real_connect(Env env,
                              @Optional("localhost") StringValue host,
                              @Optional StringValue userName,
                              @Optional StringValue password,
                              @Optional StringValue dbname,
                              @Optional("3306") int port,
                              @Optional StringValue socket,
                              @Optional int flags)
  {
    return connectInternal(env,
                           host.toString(),
                           userName.toString(),
                           password.toString(),
                           dbname.toString(),
                           port,
                           socket.toString(),
                           flags,
                           null,
                           null);
  }

  /**
   * Escapes the string
   */
  public StringValue real_escape_string(StringValue str)
  {
    return realEscapeString(str);
  }

  /**
   * Rolls the current transaction back.
   */
  public boolean rollback()
  {
    return super.rollback();
  }

  /**
   * Selects the underlying database/catalog to use.
   *
   * @param dbname the name of the database to select.
   */
  public boolean select_db(StringValue dbname)
  {
    try {
      if (isConnected()) {
        validateConnection().setCatalog(dbname.toString());

        return true;
      }
      else
        return false;
    } catch (SQLException e) {
      log.log(Level.FINE, e.toString(), e);
      getEnv().warning(e.getMessage());
      return false;
    }
  }

  /**
   * Sets the character set
   */
  public boolean set_charset(String charset)
  {
    return false;
  }

  /**
   * Sets a mysqli option
   */
  public boolean set_opt(int option, Value value)
  {
    return options(option, value);
  }

  /**
   * Quercus function to get the field 'sqlstate'.
   */
  public StringValue getsqlstate(Env env)
  {
    return sqlstate(env);
  }

  /**
   * Returns the SQLSTATE error
   */
  public StringValue sqlstate(Env env)
  {
    int code = validateConnection().getErrorCode();
    return env.createString(lookupSqlstate(code));
  }

  /**
   * Given an error number, returns a SQLSTATE error string.
   */
  static String lookupSqlstate(int errno)
  {
    if (errno == 0)
      return "00000";
    else
      return "HY" + errno;
  }

  /**
   * returns a string with the status of the connection
   * or FALSE if error
   */
  public Value stat(Env env)
  {
    try {
      JdbcConnectionResource connV = validateConnection();

      Connection conn = connV.getConnection(env);
      
      if (conn == null)
        return BooleanValue.FALSE;
      
      Statement stmt = null;

      StringBuilder str = new StringBuilder();

      try {
        stmt = conn.createStatement();
        stmt.execute("SHOW STATUS");

        ResultSet rs = stmt.getResultSet();

        while (rs.next()) {
          if (str.length() > 0)
            str.append(' ');
          str.append(rs.getString(1));
          str.append(": ");
          str.append(rs.getString(2));
        }

        return env.createString(str.toString());
      } finally {
        if (stmt != null)
          stmt.close();
      }
    } catch (SQLException e) {
      log.log(Level.FINE, e.toString(), e);
      return BooleanValue.FALSE;
    }
  }

  /**
   * returns a string with the status of the connection
   *
  public Value stat()
  {
    clearErrors();

    StringBuilder str = new StringBuilder();

    try {
      Statement stmt = _conn.createStatement();
      stmt.execute("SHOW STATUS");

      ResultSet rs = stmt.getResultSet();

      while (rs.next()) {
        if (str.length() > 0)
          str.append(' ');
        str.append(rs.getString(1));
        str.append(": ");
        str.append(rs.getString(2));
      }

      return new StringValueImpl(str.toString());
    } catch (SQLException e) {
      saveErrors(e);
      log.log(Level.WARNING, e.toString(), e);
      return BooleanValue.FALSE;
    }
  }
  */

  /**
   * returns a statement for use with
   * mysqli_stmt_prepare
   */
  public MysqliStatement stmt_init(Env env)
  {
    return new MysqliStatement((Mysqli) validateConnection());
  }

  /**
   * Transfers the result set from the last query on the
   * database connection represented by conn.
   *
   * Used in conjunction with mysqli_multi_query
   */
  @ReturnNullAsFalse
  public JdbcResultResource store_result(Env env)
  {
    return ((Mysqli) validateConnection()).storeResult();
  }

  /**
   * Quercus function to get the field 'thread_id'.
   */
  public Value getthread_id(Env env)
  {
    return thread_id(env);
  }

  /**
   * Query an identifier that corresponds to this specific
   * connection. Mysql calls this integer identifier a
   * thread id, but it is really a connection id.
   * Return an integer on success, FALSE on failure.
   */

  Value thread_id(Env env)
  {
    try {
      JdbcConnectionResource connV = validateConnection();
      Connection conn = connV.getConnection(env);
      
      if (conn == null)
        return BooleanValue.FALSE;

      Statement stmt = null;

      try {
        stmt = conn.createStatement();

        ResultSet rs = stmt.executeQuery("SELECT CONNECTION_ID()");

        if (rs.next())
          return LongValue.create(rs.getLong(1));
        else
          return BooleanValue.FALSE;
      } finally {
        if (stmt != null)
          stmt.close();
      }
    } catch (SQLException e) {
      log.log(Level.FINE, e.toString(), e);
      return BooleanValue.FALSE;
    }
  }

  /**
   * Kills the given mysql thread id. Killing the connection
   * is not the same as simply closing the connection. For
   * example, table locks are released by a KILL.
   */
  public boolean kill(Env env, int threadId)
  {
    try {
      JdbcConnectionResource connV = validateConnection();
      Connection conn = connV.getConnection(env);

      if (conn == null)
        return false;
      
      Statement stmt = null;
      boolean result = false;

      try {
        stmt = conn.createStatement();

        env.getQuercus().markForPoolRemoval(conn);

        ResultSet rs = stmt.executeQuery("KILL CONNECTION " + threadId);
      } catch (SQLException e) {
        // Invoking the KILL command above will terminate the connection
        // and raise a SQLException in executeQuery().

        result = true;
      } finally {
        if (stmt != null)
          stmt.close();
      }

      return result;
    } catch (SQLException e) {
      log.log(Level.FINE, e.toString(), e);
      return false;
    }
  }

  /**
   * Returns true for thread_safe
   */
  public boolean thread_safe()
  {
    return true;
  }

  /**
   * Transfers the result set from the last query on the
   * database connection represented by conn.
   *
   * Used in conjunction with mysqli_multi_query
   */
  @ReturnNullAsFalse
  public JdbcResultResource use_result(Env env)
  {
    return ((Mysqli) validateConnection()).storeResult();
  }

  /**
   * Quercus function to get the field 'warning_count'.
   */
  public int getwarning_count(Env env)
  {
    return warning_count(env);
  }
  
  /**
   * returns the number of warnings from the last query
   * in the connection object.
   *
   * @return number of warnings
   */
  public int warning_count(Env env)
  {
    return ((Mysqli) validateConnection()).getWarningCount(env);
  }

  /**
   * Creates a database-specific result.
   */
  @Override
  protected JdbcResultResource createResult(Env env,
					    Statement stmt,
                                            ResultSet rs)
  {
    return new MysqliResult(env, stmt, rs, this);
  }

  /**
   * This functions queries the connection with "SHOW WARNING"
   *
   * @return # of warnings
   */
  private int getWarningCount(Env env)
  {
    if (getWarnings() != null) {
      JdbcResultResource warningResult;
      warningResult = metaQuery(env, "SHOW WARNINGS", getCatalog().toString());
      int warningCount = 0;

      if (warningResult != null) {
        warningCount =
          JdbcResultResource.getNumRows(warningResult.getResultSet());
      }

      if (warningCount >= 0)
        return warningCount;
      else
        return 0;
    } else
      return 0;
  }

  /**
   * Used by the
   * various mysqli functions to query the database
   * for metadata about the resultset which is
   * not in ResultSetMetaData.
   *
   * This function DOES NOT clear existing resultsets.
   */
  protected MysqliResult metaQuery(Env env,
                                   String sql,
                                   String catalog)
  {
    clearErrors();

    Value currentCatalog = getCatalog();

    try {
      Connection conn = getConnection(env);
      
      if (conn == null)
        return null;
      
      conn.setCatalog(catalog);

      // need to create statement after setting catalog or
      // else statement will have wrong catalog
      Statement stmt = conn.createStatement();
      stmt.setEscapeProcessing(false);

      if (stmt.execute(sql)) {
        MysqliResult result
	  = (MysqliResult) createResult(getEnv(), stmt, stmt.getResultSet());
        conn.setCatalog(currentCatalog.toString());
        return result;
      } else {
        conn.setCatalog(currentCatalog.toString());
        return null;
      }
    } catch (SQLException e) {
      saveErrors(e);
      log.log(Level.WARNING, e.toString(), e);
      return null;
    }
  }

  /**
   * indicates if one or more result sets are
   * available from a multi query
   *
   * _hasBeenStored tells moreResults whether the
   * _nextResultValue has been stored already.
   * If so, more results will return true only if
   * there is another result.
   */
  private boolean moreResults()
  {
    return !_hasBeenUsed || _nextResultValue < _resultValues.size() - 1;
  }

  /**
   * Used for multiple queries. the
   * JdbcConnectionResource now stores the
   * result sets so that mysqli_store_result
   * and mysqli_use_result can return result values.
   *
   * XXX: this may not function correctly in the
   * context of a transaction.  Unclear wether
   * mysqli_multi_query was designed with transactions
   * in mind.
   *
   * XXX: multiQuery sets fieldCount to true or false
   * depending on the last query entered.  Not sure what
   * actual PHP intention is.
   */
  private boolean multiQuery(Env env, StringValue sql)
  {
    clearErrors();

    // Empty _resultValues on new call to query
    // But DO NOT close the individual result sets.
    // They may still be in use.
    _resultValues.clear();

    ArrayList<String> splitQuery = splitMultiQuery(sql);

    Statement stmt = null;

    try {
      setResultResource(null);

      for (String s : splitQuery) {
        Connection conn = getConnection(env);
        
        if (conn == null)
          return false;
        
        stmt = conn.createStatement();
        stmt.setEscapeProcessing(false);
        if (stmt.execute(s)) {
          setAffectedRows(0);
          setResultResource(createResult(getEnv(), stmt, stmt.getResultSet()));
          _resultValues.add(getResultResource());
          setWarnings(stmt.getWarnings());
        } else {
          setAffectedRows(stmt.getUpdateCount());
          setWarnings(stmt.getWarnings());
        }
      }
    } catch (DataTruncation truncationError) {
      try {
        setAffectedRows(stmt.getUpdateCount());
        setWarnings(stmt.getWarnings());
      } catch (SQLException e) {
        saveErrors(e);
        log.log(Level.WARNING, e.toString(), e);
        return false;
      }
    } catch (SQLException e) {
      saveErrors(e);
      log.log(Level.WARNING, e.toString(), e);
      return false;
    }

    if (_resultValues.size() > 0) {
      _nextResultValue = 0;
      _hasBeenUsed = false;
    }

    return true;
  }

  /**
   * prepares the next resultset from
   * a multi_query
   */
  private boolean nextResult()
  {
    if (_nextResultValue + 1 < _resultValues.size()) {
      _hasBeenUsed = false;
      _nextResultValue++;
      return true;
    } else
      return false;
  }

  /**
   * splits a string of multiple queries separated
   * by ";" into an arraylist of strings
   */
  private ArrayList<String> splitMultiQuery(StringValue sqlStr)
  {
    ArrayList<String> result = new ArrayList<String>();
    StringBuilder queryBuffer = new StringBuilder(64);
    final String sql = sqlStr.toString();
    final int length = sql.length();
    boolean inQuotes = false;
    char c;

    for (int i = 0; i < length; i++) {
      c = sql.charAt(i);

      if (c == '\\') {
        queryBuffer.append(c);
        if (i < length - 1) {
          queryBuffer.append(sql.charAt(i+1));
          i++;
        }
        continue;
      }

      if (inQuotes) {
        queryBuffer.append(c);
        if (c == '\'') {
          inQuotes = false;
        }
        continue;
      }

      if (c == '\'') {
        queryBuffer.append(c);
        inQuotes = true;
        continue;
      }

      if (c == ';') {
        result.add(queryBuffer.toString().trim());
        queryBuffer = new StringBuilder(64);
      } else
        queryBuffer.append(c);
    }

    if (queryBuffer.length() > 0)
      result.add(queryBuffer.toString().trim());

    return result;
  }

  /**
   * returns the next jdbcResultValue
   */
  private JdbcResultResource storeResult()
  {
    if (!_hasBeenUsed) {
      _hasBeenUsed = true;

      return _resultValues.get(_nextResultValue);
    } else
      return null;
  }

  // Indicate that this connection is a "persistent" connections,
  // meaning it can't be closed.

  public void setPersistent()
  {
    _isPersistent = true;
  }

  // Invoked to close a connection.

  public boolean close(Env env)
  {
    return super.close(env);
  }

  public String toString()
  {
    StringBuilder desc = new StringBuilder();
    desc.append("Mysqli[");

    if (isConnected())
      desc.append( getHost() );

    desc.append("]");

    if (_isPersistent)
      desc.append(" persistent");

    return desc.toString();
  }
}
