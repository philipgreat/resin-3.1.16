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

package com.caucho.quercus.lib;

import com.caucho.quercus.Quercus;
import com.caucho.quercus.QuercusException;
import com.caucho.quercus.QuercusModuleException;
import com.caucho.quercus.annotation.NotNull;
import com.caucho.quercus.annotation.Optional;
import com.caucho.quercus.annotation.Reference;
import com.caucho.quercus.annotation.ReturnNullAsFalse;
import com.caucho.quercus.annotation.UsesSymbolTable;
import com.caucho.quercus.env.*;
import com.caucho.quercus.lib.file.BinaryStream;
import com.caucho.quercus.lib.file.FileInput;
import com.caucho.quercus.lib.file.FileModule;
import com.caucho.quercus.lib.file.FileOutput;
import com.caucho.quercus.module.AbstractQuercusModule;
import com.caucho.quercus.program.QuercusProgram;
import com.caucho.util.L10N;
import com.caucho.util.RandomUtil;
import com.caucho.vfs.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * PHP mysql routines.
 */
public class MiscModule extends AbstractQuercusModule {
  private static final L10N L = new L10N(MiscModule.class);
  private static final Logger log
    = Logger.getLogger(MiscModule.class.getName());

  /**
   * Escapes characters in a string.
   */
  public static String escapeshellcmd(String command)
  {
    StringBuilder sb = new StringBuilder();
    int len = command.length();
    
    boolean hasApos = false;
    boolean hasQuot = false;

    for (int i = 0; i < len; i++) {
      char ch = command.charAt(i);

      switch (ch) {
      case '#': case '&': case ';': case '`': case '|':
      case '*': case '?': case '~': case '<': case '>':
      case '^': case '(': case ')': case '[': case ']':
      case '{': case '}': case '$': case '\\': case ',':
      case 0x0a: case 0xff:
	sb.append('\\');
	sb.append(ch);
	break;
      case '\'':
	hasApos = ! hasApos;
	sb.append(ch);
	break;
      case '\"':
	hasQuot = ! hasQuot;
	sb.append(ch);
	break;
      default:
	sb.append(ch);
      }
    }

    String result = sb.toString();

    if (hasApos) {
      int p = result.lastIndexOf('\'');
      result = result.substring(0, p) + "\\" + result.substring(p);
    }

    if (hasQuot) {
      int p = result.lastIndexOf('\"');
      result = result.substring(0, p) + "\\" + result.substring(p);
    }

    return result;
  }

  /**
   * Escapes characters in a string.
   */
  public static String escapeshellarg(String arg)
  {
    StringBuilder sb = new StringBuilder();

    sb.append('\'');
    
    int len = arg.length();

    for (int i = 0; i < len; i++) {
      char ch = arg.charAt(i);

      if (ch == '\'')
	sb.append("\\\'");
      else
	sb.append(ch);
    }

    sb.append('\'');

    return sb.toString();
  }

  /**
   * Comples and evaluates an expression.
   */
  @UsesSymbolTable
  public Value eval(Env env, String code)
  {
    try {
      if (log.isLoggable(Level.FINER))
	log.finer("quercus eval: [[" + code + "]]");
      
      Quercus quercus = env.getQuercus();
      
      QuercusProgram program = quercus.parseCode(code);
      
      Value value = program.execute(env);
      
      return value;
    } catch (IOException e) {
      throw new QuercusException(e);
    }
  }

  /**
   * packs the format into a binary.
   */
  public Value pack(Env env, String format, Value []args)
  {
    try {
      ArrayList<PackSegment> segments = parsePackFormat(env, format);

      StringValue bb = env.createBinaryBuilder();

      int i = 0;
      for (PackSegment segment : segments) {
	i = segment.pack(env, bb, i, args);
      }

      return bb;
    } catch (IOException e) {
      throw new QuercusModuleException(e);
    }
  }

  /**
   * packs the format into a binary.
   */
  public Value unpack(Env env, String format, InputStream is)
  {
    try {
      ArrayList<PackSegment> segments = parseUnpackFormat(env, format);

      ArrayValue array = new ArrayValueImpl();

      for (PackSegment segment : segments) {
        segment.unpack(env, array, is);
      }

      return array;
    } catch (IOException e) {
      throw new QuercusModuleException(e);
    }
  }

  /**
   * Logs the expression.
   */
  public Value resin_debug(String code)
  {
    log.info(code);

    return NullValue.NULL;
  }

  /**
   * Dumps the Java stack to standard out.
   */
  public Value resin_thread_dump()
  {
    Thread.dumpStack();

    return NullValue.NULL;
  }

  /**
   * Dumps the stack.
   */
  public static Value dump_stack(Env env)
  {
    try {
      Exception e = new Exception("Stack trace");
      e.fillInStackTrace();

      WriteStream out = env.getPwd().lookup("stderr:").openWrite();
      try {
	e.printStackTrace(out.getPrintWriter());
	//ScriptStackTrace.printStackTrace(e, out.getPrintWriter());
      } finally {
	out.close();
      }

      return NullValue.NULL;
    } catch (IOException e) {
      throw new QuercusModuleException(e);
    }
  }

  /**
   * Execute a system command.
   */
  public static String exec(Env env, String command,
			    @Optional Value output,
			    @Optional @Reference Value result)
  {
    try {
      String []args = new String[3];

      if (Path.isWindows()) {
        args[0] = "cmd";
        args[1] = "/c";
      }
      else {
        args[0] = "sh";
        args[1] = "-c";
      }

      args[2] = command;
      
      Process process = Runtime.getRuntime().exec(args);

      InputStream is = process.getInputStream();
      InputStream es = process.getErrorStream();
      OutputStream os = process.getOutputStream();
      os.close();

      StringBuilder sb = new StringBuilder();
      String line = "";

      int ch;
      boolean hasCr = false;
      while ((ch = is.read()) >= 0) {
        if (ch == '\n') {
          if (! hasCr) {
            line = sb.toString();
            sb.setLength(0);
            if (output != null)
              output.put(env.createString(line));
          }
          hasCr = false;
        }
        else if (ch == '\r') {
          line = sb.toString();
          sb.setLength(0);
          output.put(env.createString(line));
          hasCr = true;
        }
        else
          sb.append((char) ch);
      }

      if (sb.length() > 0) {
        line = sb.toString();
        sb.setLength(0);
        output.put(env.createString(line));
      }

      is.close();
      
      env.getOut().writeStream(es);
      es.close();

      int status = process.waitFor();

      result.set(new LongValue(status));

      return line;
    } catch (Exception e) {
      log.log(Level.FINE, e.getMessage(), e);
      env.warning(e.getMessage(), e);

      return null;
    }
  }

  /**
   * Execute a system command.
   */
  public static Value shell_exec(Env env, String command)
  {
    String []args = new String[3];

    try {
      if (Path.isWindows()) {
        args[0] = "cmd";
        args[1] = "/c";
      }
      else {
        args[0] = "sh";
        args[1] = "-c";
      }

      args[2] = command;
      
      Process process = Runtime.getRuntime().exec(args);

      InputStream is = process.getInputStream();
      InputStream es = process.getErrorStream();
      OutputStream os = process.getOutputStream();
      os.close();

      StringValue sb = env.createUnicodeBuilder();

      int ch;
      while ((ch = is.read()) >= 0) {
        sb.append((char) ch);
      }

      is.close();

      if ((ch = es.read()) >= 0) {
        env.print((char)ch);
        
        while ((ch = es.read()) >= 0) {
          env.print((char)ch);
        }
        
        return NullValue.NULL;
      }
      
      es.close();

      int status = process.waitFor();

      return sb;
    } catch (Exception e) {
      log.log(Level.FINE, e.getMessage(), e);
      env.warning(e.getMessage(), e);

      return NullValue.NULL;
    }
  }

  /**
   * Execute a system command.
   */
  public static void passthru(Env env, String command,
			       @Optional @Reference Value result)
  {

    try {
      String []args = new String[3];
      if (Path.isWindows()) {
        args[0] = "cmd";
        args[1] = "/c";
      }
      else {
        args[0] = "sh";
        args[1] = "-c";
      }

      args[2] = command;

      ProcessBuilder processBuilder = new ProcessBuilder(args);
      processBuilder.redirectErrorStream(true);
      final Process process = processBuilder.start();

      try {
        InputStream is = process.getInputStream();
        OutputStream os = process.getOutputStream();
        os.close();

        env.getOut().writeStream(is);
        is.close();

        int status = process.waitFor();
      }
      finally {
        process.destroy();
      }
    } catch (Exception e) {
      env.warning(e.getMessage(), e);
    }
  }

  /*
   * Basic implementation of proc_open.
   * XXX: options
   */
  @ReturnNullAsFalse 
  public static ProcOpenResource proc_open(Env env, 
                                           String command, 
                                           ArrayValue descriptorArray,
                                           @Reference Value pipes,
                                           @Optional Path pwd,
                                           @Optional ArrayValue envArray,
                                           @Optional ArrayValue options)
  {
    String []args = new String[3];

    try {
      if (Path.isWindows()) {
        args[0] = "cmd";
        args[1] = "/c";
      }
      else {
        args[0] = "sh";
        args[1] = "-c";
      }

      args[2] = command;

      String []envStrings = null;
      File pwdFile = null;
      
      if (envArray != null) {
        int size = envArray.getSize();
        
        envStrings = new String[size];
        
        int i = 0;
        for (Map.Entry<Value,Value> entry : envArray.entrySet()) {
          envStrings[i++] = entry.getKey() + "=" + entry.getValue();
        }
      }
      
      if (pwd != null) {
        pwdFile = new File(pwd.getFullPath());
      }

      Process process = Runtime.getRuntime().exec(args, envStrings, pwdFile);
      
      ProcOpenOutput in = null;
      ProcOpenInput out = null;
      ProcOpenInput es = null;

      ArrayValue array = pipes.toAutoArray().toArrayValue(env);
      pipes.set(array);
      array.clear();
      
      for (Map.Entry<Value,Value> entry : descriptorArray.entrySet()) {
        Value key = entry.getKey();
        Value val = entry.getValue();

        String type = val.get(LongValue.ZERO).toString();
        StringValue name = val.get(LongValue.ONE).toStringValue();
        String mode = val.get(LongValue.create(2)).toString();
        
        // input to the command
        if (key.equals(LongValue.ZERO)) {
          if (type.equals("pipe")) {
            in = new ProcOpenOutput(env, process.getOutputStream());

            array.put(LongValue.ZERO, env.wrapJava(in));
          }
          else if (type.equals("file")) {
            OutputStream processOut = process.getOutputStream();
            
            BinaryStream stream = FileModule.fopen(env, name, mode, false, null);
            
            if (stream instanceof FileInput) {
              FileInput file = (FileInput) stream;
              
              int ch;
              while ((ch = file.read()) >= 0) {
                processOut.write(ch);
              }
            }
            
            stream.close();
            processOut.close();
          }
        }
        // place to put output from the command
        else if (key.equals(LongValue.ONE)) {
          if (type.equals("pipe")) {
            out = new ProcOpenInput(env, process.getInputStream());

            array.put(LongValue.ONE, env.wrapJava(out));
          }
          else if (type.equals("file")) {
            BinaryStream stream = FileModule.fopen(env, name, mode, false, null);

            if (stream instanceof FileOutput) {
              FileOutput file = (FileOutput) stream;
              
              out = new ProcOpenInput(env, process.getInputStream(), file);
            }
            else if (stream != null)
              stream.close();
          }
        }
        // place to put error output from the command
        else if (key.equals(new LongValue(2))) {
          if (type.equals("pipe")) {
            es = new ProcOpenInput(env, process.getErrorStream());
            
            array.put(new LongValue(2), env.wrapJava(es));
          }
          else if (type.equals("file")) {
            BinaryStream stream = FileModule.fopen(env, name, mode, false, null);
            
            if (stream instanceof FileOutput) {
              FileOutput file = (FileOutput) stream;
              
              es = new ProcOpenInput(env, process.getErrorStream(), file);
            }
            else if (stream != null)
              stream.close();
          }
        }
      }
      
      return new ProcOpenResource(env, process, in, out, es);

    } catch (Exception e) {

      log.log(Level.FINE, e.getMessage(), e);
      env.warning(e);

      return null;
    }
  }
  
  /*
   * Closes the process opened by proc_open.
   */
  public static int proc_close(Env env,
                               @NotNull ProcOpenResource stream)
  {
    if (stream == null) {
      log.log(Level.FINE, "input to proc_close must not be null");
      env.warning("input to proc_close must not be null");
      
      return -1;
    }
    
   return stream.pclose();
  }
  
  /*
   * Forcibly terminates the process opened by proc_open.
   */
  public static boolean proc_terminate(Env env,
                                       @NotNull ProcOpenResource stream)
  {
    if (stream == null) {
      log.log(Level.FINE, "input to proc_close must not be null");
      env.warning("input to proc_close must not be null");
      
      return false;
    }
    
   return stream.terminate();
  }
  
  /**
   * Returns the disconnect ignore setting
   */
  public static int ignore_user_abort(@Optional boolean set)
  {
    return 0;
  }

  /**
   * Returns a unique id.
   */
  public String uniqid(@Optional String prefix, @Optional boolean moreEntropy)
  {
    StringBuilder sb = new StringBuilder();

    if (prefix != null)
      sb.append(prefix);

    addUnique(sb);

    if (moreEntropy)
      addUnique(sb);

    return sb.toString();
  }

  private void addUnique(StringBuilder sb)
  {
    long value = RandomUtil.getRandomLong();

    if (value < 0)
      value = -value;

    int limit = 13;

    for (; limit > 0; limit--) {
      long digit = value % 26;
      value = value / 26;

      sb.append((char) ('a' + digit));
    }
  }

  /**
   * Sleep for a number of microseconds.
   */
  public static Value usleep(long microseconds)
  {
    try {
      Thread.sleep(microseconds / 1000);
    } catch (Throwable e) {
    }

    return NullValue.NULL;
  }

  /**
   * Sleep for a number of seconds.
   */
  public static long sleep(long seconds)
  {
    try {
      Thread.sleep(seconds * 1000);
    } catch (Throwable e) {
    }

    return seconds;
  }

  /**
   * Returns an array detailing what the browser is capable of.
   * A general browscap.ini file can be used.
   *
   * @param env
   * @param user_agent
   * @param return_array
   */
  public static Value get_browser(
                       Env env,
                       @Optional() String user_agent,
                       @Optional() boolean return_array)
  {
    if (user_agent == null ||
        user_agent.length() == 0) 
      user_agent = env.getRequest().getHeader("User-Agent");

    if (user_agent == null) {
      env.warning(L.l("HTTP_USER_AGENT not set."));
      return BooleanValue.FALSE;
    }

    Value browscap = env.getConfigVar("browscap");
    if (browscap == null) {
      env.warning(L.l("Browscap path not set in PHP.ini."));
      return BooleanValue.FALSE;
    }

    Path path = env.lookup(browscap.toString());
    if (path == null) {
      env.warning(L.l("Browscap file not found."));
      return BooleanValue.FALSE;
    }

    Value ini = FileModule.parse_ini_file(env, path, true);
    if (ini == BooleanValue.FALSE)
      return BooleanValue.FALSE;

    return getBrowserReport(
        env, ini.toArrayValue(env), user_agent, return_array);
  }

  private static Value getBrowserReport(
                       Env env,
                       ArrayValue browsers,
                       String user_agent,
                       boolean return_array)
  {
    StringValue patternMatched = StringValue.EMPTY;
    String regExpMatched = null;

    for (Map.Entry<Value,Value> entry : browsers.entrySet()) {
      StringValue pattern = entry.getKey().toStringValue();
      
      if (pattern.toString().equals(user_agent)) {
        patternMatched = pattern;
        regExpMatched = null;
        break;
      }

      String regExp = formatBrowscapRegexp(pattern);
      Matcher m = Pattern.compile(regExp).matcher(user_agent);

      // Want the longest matching pattern.
      if (m.matches()) {
        if (pattern.length() > patternMatched.length()) {
          patternMatched = pattern;
          regExpMatched = regExp;
        }
      }
    }

    if (patternMatched.length() == 0)
      return BooleanValue.FALSE;

    return prepareBrowserReport(env, browsers, patternMatched, regExpMatched,
        user_agent, return_array);
  }

  private static Value prepareBrowserReport(
                       Env env,
                       ArrayValue browsers,
                       StringValue patternMatched, 
                       String regExpMatched,
                       String user_agent,
                       boolean return_array)
  {
    ArrayValue capabilities = browsers.get(patternMatched).toArrayValue(env);

    if (regExpMatched == null)
      capabilities.put(env.createString("browser_name_regex"),
		       patternMatched);
    else
      capabilities.put("browser_name_regex", regExpMatched);
    capabilities.put(env.createString("browser_name_pattern"), patternMatched);

    addBrowserCapabilities(env, browsers,
			   capabilities.get(env.createString("parent")),
			   capabilities);

    if (return_array) {
      ArrayValue array = new ArrayValueImpl();
      array.put(env.createString(user_agent), capabilities);
      return array;
    }

    ObjectValue object = env.createObject();
    for (Map.Entry<Value,Value> entry : capabilities.entrySet()) {
      object.putField(env, entry.getKey().toString(), entry.getValue());
    }
    
    return object;
  }
  
  private static void addBrowserCapabilities(
                       Env env,
                       ArrayValue browsers,
                       Value browser,
                       ArrayValue cap)
  {
    if (browser == UnsetValue.UNSET)
      return;

    Value field = null;
    if ((field = browsers.get(browser)) == UnsetValue.UNSET)
      return;

    ArrayValue browserCapabilities = field.toArrayValue(env);
    StringValue parentString = env.createString("parent");
    
    for (Map.Entry<Value,Value> entry : browserCapabilities.entrySet()) {
      Value key = entry.getKey();

      if (key.equals(parentString)) {
        addBrowserCapabilities(
            env, browsers, entry.getValue(), cap);
      }
      else if (cap.containsKey(key) == null)
        cap.put(key, entry.getValue());
    }
  }

  private static String formatBrowscapRegexp(StringValue key)
  {
    int length = key.length();
  
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < length; i++) {
      char ch = key.charAt(i);
      switch (ch) {
        case '*':
          sb.append('.');
          sb.append('*');
          break;
        case '?':
          sb.append('.');
          break;
        case '.':
          sb.append('\\');
          sb.append('.');
          break;
        case '+':
          sb.append('\\');
          sb.append('+');
          break;
        case '(':
          sb.append('\\');
          sb.append('(');
          break;
         case ')':
          sb.append('\\');
          sb.append(')');
          break;
        case '{':
          sb.append('\\');
          sb.append('{');
          break;
         case '}':
          sb.append('\\');
          sb.append('}');
          break;
        case ']':
          sb.append('\\');
          sb.append(']');
          break;
        case '[':
          sb.append('\\');
          sb.append('[');
          break;
        case '\\':
          sb.append('\\');
          sb.append('\\');
          break;
        case '^':
          sb.append('\\');
          sb.append('^');
          break;
        case '$':
          sb.append('\\');
          sb.append('$');
          break;
        case '&':
          sb.append('\\');
          sb.append('&');
          break;
        case '|':
          sb.append('\\');
          sb.append('|');
          break;
        default:
          sb.append(ch);
      }
    }
    
    return sb.toString();
  }

  /**
   * Execute a system command.
   */
  public static String system(Env env, String command,
			      @Optional @Reference Value result)
  {
    return exec(env, command, null, result);
  }

  private static ArrayList<PackSegment> parsePackFormat(Env env, String format)
  {
    ArrayList<PackSegment> segments = new ArrayList<PackSegment>();

    int length = format.length();
    for (int i = 0; i < length; i++) {
      char ch = format.charAt(i);
      
      int count = 0;
      char ch1 = ' ';
      for (i++;
	   i < length && '0' <= (ch1 = format.charAt(i)) && ch1 <= '9';
	   i++) {
	count = 10 * count + ch1 - '0';
      }

      if (ch1 == '*' && count == 0) {
	i++;
	count = Integer.MAX_VALUE;
      }
      else if (count == 0)
	count = 1;

      if (i < length)
	i--;

      switch (ch) {
      case 'a':
	segments.add(new SpacePackSegment(env, count, (byte) 0));
	break;
      case 'A':
	segments.add(new SpacePackSegment(env, count, (byte) 0x20));
	break;
      case 'h':
	segments.add(new RevHexPackSegment(count));
	break;
      case 'H':
	segments.add(new HexPackSegment(env, count));
	break;
      case 'c':
      case 'C':
	segments.add(new BigEndianPackSegment(count, 1));
	break;
      case 's':
      case 'n':
      case 'S':
	segments.add(new BigEndianPackSegment(count, 2));
	break;
      case 'v':
	segments.add(new LittleEndianPackSegment(count, 2));
	break;
      case 'l':
      case 'L':
      case 'N':
	segments.add(new BigEndianPackSegment(count, 4));
	break;
      case 'V':
	segments.add(new LittleEndianPackSegment(count, 4));
	break;
      case 'i':
      case 'I':
	segments.add(new BigEndianPackSegment(count, 8));
	break;
      case 'd':
	segments.add(new DoublePackSegment(count));
	break;
      case 'f':
	segments.add(new FloatPackSegment(count));
	break;
      case 'x':
	segments.add(new NullPackSegment(count));
	break;
      case '@':
	segments.add(new PositionPackSegment(count));
	break;
      }
    }

    return segments;
  }

  private static ArrayList<PackSegment> parseUnpackFormat(Env env,
							  String format)
  {
    ArrayList<PackSegment> segments = new ArrayList<PackSegment>();

    int length = format.length();
    for (int i = 0; i < length; i++) {
      char ch = format.charAt(i);
      
      int count = 0;
      char ch1 = ' ';
      for (i++;
	   i < length && '0' <= (ch1 = format.charAt(i)) && ch1 <= '9';
	   i++) {
	count = 10 * count + ch1 - '0';
      }
      
      if (count == 0)
	count = 1;

      if (i < length)
	i--;

      StringBuilder sb = new StringBuilder();
      
      for (i++; i < length && (ch1 = format.charAt(i)) != '/'; i++) {
	sb.append(ch1);
      }

      String name = sb.toString();

      switch (ch) {
      case 'a':
	segments.add(new SpacePackSegment(env, name, count, (byte) 0));
	break;
      case 'A':
	segments.add(new SpacePackSegment(env, name, count, (byte) 0x20));
	break;
      case 'h':
	segments.add(new RevHexPackSegment(name, count));
	break;
      case 'H':
	segments.add(new HexPackSegment(env, name, count));
	break;
      case 'c':
	segments.add(new BigEndianPackSegment(name, count, 1, true));
	break;
      case 'C':
	segments.add(new BigEndianPackSegment(name, count, 1, false));
	break;
      case 's':
	segments.add(new BigEndianPackSegment(name, count, 2, true));
	break;
      case 'n':
      case 'S':
	segments.add(new BigEndianPackSegment(name, count, 2, false));
	break;
      case 'v':
	segments.add(new LittleEndianPackSegment(name, count, 2));
	break;
      case 'l':
	segments.add(new BigEndianPackSegment(name, count, 4, true));
	break;
      case 'L':
      case 'N':
	segments.add(new BigEndianPackSegment(name, count, 4, false));
	break;
      case 'V':
	segments.add(new LittleEndianPackSegment(name, count, 4));
	break;
      case 'i':
      case 'I':
	segments.add(new BigEndianPackSegment(name, count, 8, false));
	break;
      case 'd':
	segments.add(new DoublePackSegment(name, count));
	break;
      case 'f':
	segments.add(new FloatPackSegment(name, count));
	break;
      case 'x':
	segments.add(new NullPackSegment(name, count));
	break;
      case '@':
	segments.add(new PositionPackSegment(name, count));
	break;
      }
    }

    return segments;
  }

  abstract static class PackSegment {
    abstract public int pack(Env env, StringValue bb,
			      int i, Value []args)
      throws IOException;
    
    abstract public void unpack(Env env, ArrayValue array, InputStream is)
      throws IOException;
  }

  static class SpacePackSegment extends PackSegment {
    private final StringValue _name;
    private final int _length;
    private final byte _pad;

    SpacePackSegment(Env env, int length, byte pad)
    {
      this(env, "", length, pad);
    }

    SpacePackSegment(Env env, String name, int length, byte pad)
    {
      _name = env.createString(name);
      _length = length;
      _pad = pad;
    }
    
    @Override
    public int pack(Env env, StringValue bb, int i, Value []args)
      throws IOException
    {
      Value arg;

      if (i < args.length) {
	arg = args[i];
	i++;
      }
      else {
	env.warning("a: not enough arguments");

	return i;
      }

      InputStream is = arg.toInputStream();

      int length = _length;

      for (int j = 0; j < length; j++) {
	int ch = is.read();

	if (ch >= 0)
	  bb.appendByte(ch);
	else if (length == Integer.MAX_VALUE)
	  return i;
	else
	  bb.appendByte(_pad);
      }

      return i;
    }
    
    @Override
    public void unpack(Env env, ArrayValue result, InputStream is)
      throws IOException
    {
      StringValue bb = env.createBinaryBuilder();
      
      for (int i = 0; i < _length; i++) {
	int ch = is.read();

	if (ch == _pad) {
	}
	else if (ch >= 0)
	  bb.appendByte(ch);
	else
	  break;
      }

      result.put(_name, bb);
    }
  }

  static class HexPackSegment extends PackSegment {
    private final StringValue _name;
    private final int _length;

    HexPackSegment(Env env, int length)
    {
      this(env, "", length);
    }

    HexPackSegment(Env env, String name, int length)
    {
      _name = env.createString(name);
      _length = length;
    }
    
    @Override
    public int pack(Env env, StringValue bb, int i, Value []args)
      throws IOException
    {
      Value arg;

      if (i < args.length) {
	arg = args[i];
	i++;
      }
      else {
	env.warning("a: not enough arguments");

	return i;
      }

      StringValue s = arg.toStringValue();

      int strlen = s.length();

      if (_length == Integer.MAX_VALUE) {
      }
      else if (strlen < _length) {
	env.warning("not enough characters in hex string");

	return i;
      }
      else if (_length < strlen)
	strlen = _length;
      
      int tail = strlen / 2;
      for (int j = 0; j < tail; j++) {
	int d = 0;
	
	char ch = s.charAt(2 * j);

	d += 16 * hexToDigit(env, ch);
	
	ch = s.charAt(2 * j + 1);

	d += hexToDigit(env, ch);

	bb.appendByte(d);
      }
      
      if ((strlen & 1) == 1) {
	int d = 16 * hexToDigit(env, s.charAt(strlen - 1));

	bb.appendByte(d);
      }

      return i;
    }
    
    @Override
    public void unpack(Env env, ArrayValue result, InputStream is)
      throws IOException
    {
      StringValue sb = new StringBuilderValue();
      for (int i = _length / 2 - 1; i >= 0; i--) {
	int ch = is.read();

	sb.append(digitToHex(ch >> 4));
	sb.append(digitToHex(ch));
      }

      result.put(_name, sb);
    }
  }

  static class RevHexPackSegment extends PackSegment {
    private final StringValue _name;
    private final int _length;

    RevHexPackSegment(int length)
    {
      this("", length);
    }

    RevHexPackSegment(String name, int length)
    {
      _name = new StringBuilderValue(name);
      _length = length;
    }
    
    @Override
    public int pack(Env env, StringValue bb, int i, Value []args)
      throws IOException
    {
      Value arg;

      if (i < args.length) {
	arg = args[i];
	i++;
      }
      else {
	env.warning("a: not enough arguments");

	return i;
      }

      StringValue s = arg.toStringValue();

      int strlen = s.length();

      if (_length == Integer.MAX_VALUE) {
      }
      else if (strlen < _length) {
	env.warning("not enough characters in hex string");

	return i;
      }
      else if (_length < strlen)
	strlen = _length;
      
      int tail = strlen / 2;
      for (int j = 0; j < tail; j++) {
	int d = 0;
	
	char ch = s.charAt(2 * j);

	d += hexToDigit(env, ch);
	
	ch = s.charAt(2 * j + 1);

	d += 16 * hexToDigit(env, ch);

	bb.appendByte(d);
      }
      
      if ((strlen & 1) == 1) {
	int d = hexToDigit(env, s.charAt(strlen - 1));

	bb.appendByte(d);
      }

      return i;
    }
    
    @Override
    public void unpack(Env env, ArrayValue result, InputStream is)
      throws IOException
    {
      StringValue sb = new StringBuilderValue();
      for (int i = _length / 2 - 1; i >= 0; i--) {
	int ch = is.read();

	sb.append(digitToHex(ch));
	sb.append(digitToHex(ch >> 4));
      }

      result.put(_name, sb);
    }
  }

  static class BigEndianPackSegment extends PackSegment {
    private final String _name;
    private final int _length;
    private final int _bytes;
    private final boolean _isSigned;

    BigEndianPackSegment(int length, int bytes)
    {
      _name = "";
      _length = length;
      _bytes = bytes;
      _isSigned = false;
    }

    BigEndianPackSegment(String name, int length, int bytes, boolean isSigned)
    {
      _name = name;
      _length = length;
      _bytes = bytes;
      _isSigned = isSigned;
    }
    
    @Override
    public int pack(Env env, StringValue bb, int i, Value []args)
      throws IOException
    {
      for (int j = 0; j < _length; j++) {
	Value arg;

	if (i < args.length) {
	  arg = args[i];
	  i++;
	}
	else if (_length == Integer.MAX_VALUE)
	  return i;
	else {
	  env.warning("a: not enough arguments");

	  return i;
	}
 
	long v = arg.toLong();

	for (int k = _bytes - 1; k >= 0; k--) {
	  bb.appendByte((int) (v >> (8 * k)));
	}
      }

      return i;
    }
    
    @Override
    public void unpack(Env env, ArrayValue result, InputStream is)
      throws IOException
    {
      for (int j = 0; j < _length; j++) {
	Value key;

	// XXX: check key type with unicode semantics

	if (_name.length() == 0)
	  key = LongValue.create(j + 1);
	else if (_length == 1)
	  key = new StringBuilderValue(_name);
	else {
	  StringValue sb = new StringBuilderValue();
	  sb.append(_name);
	  sb.append(j);

	  key = sb;
	}
	
	long v = 0;

	for (int k = 0; k < _bytes; k++) {
	  long d = is.read() & 0xff;

	  v = 256 * v + d;
	}

	if (_isSigned) {
	  switch (_bytes) {
	  case 1:
	    v = (byte) v;
	    break;
	  case 2:
	    v = (short) v;
	    break;
	  case 4:
	    v = (int) v;
	    break;
	  }
	}

	result.put(key, LongValue.create(v));
      }
    }
  }

  static class LittleEndianPackSegment extends PackSegment {
    private final String _name;
    private final int _length;
    private final int _bytes;

    LittleEndianPackSegment(int length, int bytes)
    {
      _name = "";
      _length = length;
      _bytes = bytes;
    }

    LittleEndianPackSegment(String name, int length, int bytes)
    {
      _name = name;
      _length = length;
      _bytes = bytes;
    }
    
    @Override
    public int pack(Env env, StringValue bb, int i, Value []args)
      throws IOException
    {
      for (int j = 0; j < _length; j++) {
	Value arg;

	if (i < args.length) {
	  arg = args[i];
	  i++;
	}
	else if (_length == Integer.MAX_VALUE)
	  return i;
	else {
	  env.warning("a: not enough arguments");

	  return i;
	}
 
	long v = arg.toLong();

	for (int k = 0; k < _bytes; k++) {
	  bb.appendByte((int) (v >> (8 * k)));
	}
      }

      return i;
    }
    
    @Override
    public void unpack(Env env, ArrayValue result, InputStream is)
      throws IOException
    {
      for (int j = 0; j < _length; j++) {
	Value key;

	if (_name == "")
	  key = LongValue.create(j);
	else if (_length == 1)
	  key = new StringBuilderValue(_name);
	else {
	  StringBuilderValue sb = new StringBuilderValue();
	  sb.append(_name);
	  sb.append(j);

	  key = sb;
	}
	
	long v = 0;

	for (int k = 0; k < _bytes; k++) {
	  long d = is.read() & 0xff;

	  v |= d << 8 * k;
	}

	result.put(key, LongValue.create(v));
      }
    }
  }

  static class DoublePackSegment extends PackSegment {
    private final String _name;
    private final int _length;

    DoublePackSegment(int length)
    {
      this("", length);
    }

    DoublePackSegment(String name, int length)
    {
      _name = name;
      _length = length;
    }
    
    @Override
    public int pack(Env env, StringValue bb, int i, Value []args)
      throws IOException
    {
      for (int j = 0; j < _length; j++) {
	Value arg;

	if (i < args.length) {
	  arg = args[i];
	  i++;
	}
	else if (_length == Integer.MAX_VALUE)
	  return i;
	else {
	  env.warning("a: not enough arguments");

	  return i;
	}
 
	double d = arg.toDouble();
	long v = Double.doubleToLongBits(d);

	for (int k = 7; k >= 0; k--) {
	  bb.appendByte((int) (v >> (8 * k)));
	}
      }

      return i;
    }
    
    @Override
    public void unpack(Env env, ArrayValue result, InputStream is)
      throws IOException
    {
      for (int j = 0; j < _length; j++) {
	Value key;

	if (_name == "")
	  key = LongValue.create(j);
	else if (_length == 1)
	  key = env.createString(_name);
	else {
	  StringValue sb = env.createBinaryBuilder();
	  sb.append(_name);
	  sb.append(j);

	  key = sb;
	}
	
	long v = 0;

	for (int k = 0; k < 8; k++) {
	  long d = is.read() & 0xff;

	  v = 256 * v + d;
	}

	result.put(key, new DoubleValue(Double.longBitsToDouble(v)));
      }
    }
  }

  static class FloatPackSegment extends PackSegment {
    private final String _name;
    private final int _length;

    FloatPackSegment(int length)
    {
      this("", length);
    }

    FloatPackSegment(String name, int length)
    {
      _name = name;
      _length = length;
    }

    @Override
    public int pack(Env env, StringValue bb, int i, Value []args)
      throws IOException
    {
      for (int j = 0; j < _length; j++) {
	Value arg;

	if (i < args.length) {
	  arg = args[i];
	  i++;
	}
	else if (_length == Integer.MAX_VALUE)
	  return i;
	else {
	  env.warning("a: not enough arguments");

	  return i;
	}
 
	double d = arg.toDouble();
	int v = Float.floatToIntBits((float) d);

	for (int k = 3; k >= 0; k--) {
	  bb.appendByte((int) (v >> (8 * k)));
	}
      }

      return i;
    }
    
    @Override
    public void unpack(Env env, ArrayValue result, InputStream is)
      throws IOException
    {
      for (int j = 0; j < _length; j++) {
	Value key;

	if (_name == "")
	  key = LongValue.create(j);
	else if (_length == 1)
	  key = env.createString(_name);
	else {
	  StringValue sb = env.createBinaryBuilder();
	  sb.append(_name);
	  sb.append(j);

	  key = sb;
	}
	
	int v = 0;

	for (int k = 0; k < 4; k++) {
	  int d = is.read() & 0xff;

	  v = 256 * v + d;
	}

	result.put(key, new DoubleValue(Float.intBitsToFloat(v)));
      }
    }
  }

  static class NullPackSegment extends PackSegment {
    private final String _name;
    private final int _length;

    NullPackSegment(int length)
    {
      this("", length);
    }

    NullPackSegment(String name, int length)
    {
      _name = name;
      
      if (length == Integer.MAX_VALUE)
	length = 0;
      
      _length = length;
    }
    
    @Override
    public int pack(Env env, StringValue bb, int i, Value []args)
      throws IOException
    {
      for (int j = 0; j < _length; j++) {
	bb.appendByte(0);
      }

      return i;
    }
    
    @Override
    public void unpack(Env env, ArrayValue result, InputStream is)
      throws IOException
    {
      for (int i = 0; i < _length; i++)
	is.read();
    }
  }

  static class PositionPackSegment extends PackSegment {
    private final int _length;

    PositionPackSegment(int length)
    {
      this("", length);
    }

    PositionPackSegment(String name, int length)
    {
      if (length == Integer.MAX_VALUE)
	length = 0;
      
      _length = length;
    }
    
    @Override
    public int pack(Env env, StringValue bb, int i, Value []args)
      throws IOException
    {
      while (bb.length() < _length) {
	bb.appendByte(0);
      }

      return i;
    }
    
    @Override
    public void unpack(Env env, ArrayValue result, InputStream is)
      throws IOException
    {
      throw new UnsupportedOperationException("'@' skip to position");
    }
  }

  static int hexToDigit(Env env, char ch)
  {
    if ('0' <= ch && ch <= '9')
      return (ch - '0');
    else if ('a' <= ch && ch <= 'f')
      return (ch - 'a' + 10);
    else if ('A' <= ch && ch <= 'F')
      return (ch - 'A' + 10);
    else {
      env.warning("pack: non hex digit: " + (char) ch);

      return 0;
    }
  }

  static char digitToHex(int d)
  {
    d &= 0xf;
    
    if (d < 10)
      return (char) ('0' + d);
    else
      return (char) ('a' + d - 10);
  }
}
