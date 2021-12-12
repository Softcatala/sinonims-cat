package org.softcatala.sinonims;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.Properties;

public class ThesaurusConfig {
  private static String DEFAULT_PORT = "8000";
  private static String DEFAULT_SERVERTIME = "Europe/Madrid";
  
  private static String DEFAULT_URLPATH = "/sinonims-api/";
  
  Integer serverPort;
  File srcFile = null;
  File auxFile = null;
  String srcFilePath;
  String serverTimezone;
  String logging;
  String production;
  String urlPath;
  String auxFilePath;
  
  public  String toString() {
    return String.format("serverPort=%d, srcFile=%s, serverTimezone=%s, logging=%s, production=%s, urlPath=%s",
            serverPort, srcFilePath, serverTimezone, logging, production, urlPath, auxFilePath
    );
  }

  public ThesaurusConfig(String[] args) {
    File file = new File (args[1]);
    try {
      Properties props = new Properties();
      try (FileInputStream fis = new FileInputStream(file)) {
        InputStreamReader isr = new InputStreamReader(fis, Charset.forName("UTF-8"));
        props.load(isr);
        serverPort = Integer.parseInt(getOptionalProperty(props,"serverPort", DEFAULT_PORT));
        serverTimezone = getOptionalProperty(props,"serverTimezone", DEFAULT_SERVERTIME);
        urlPath = getOptionalProperty(props,"urlPath", DEFAULT_URLPATH);
        logging = getOptionalProperty(props, "logging", "on");
        production = getOptionalProperty(props, "production", "yes");
        srcFilePath = getOptionalProperty(props, "srcFile", null);
        if (srcFilePath != null) {
          srcFile = new File(srcFilePath);
          if (!srcFile.exists() || !srcFile.isFile()) {
            throw new RuntimeException("Source file can not be found: " + srcFilePath);
          }
        }
        auxFilePath = getOptionalProperty(props, "auxFile", null);
        if (auxFilePath != null) {
          auxFile = new File(auxFilePath);
//          if (!auxFile.exists() || !auxFile.isFile()) {
//            throw new RuntimeException("Auxiliary file can not be found: " + auxFilePath);
//          }
        }
      } 
    } catch (IOException e) {
      throw new RuntimeException("Could not load properties from '" + file + "'", e);
    } 
  }
  
  protected String getOptionalProperty(Properties props, String propertyName, String defaultValue) {
    String propertyValue = (String)props.get(propertyName);
    if (propertyValue == null) {
      return defaultValue;
    }
    return propertyValue;
  }
}
