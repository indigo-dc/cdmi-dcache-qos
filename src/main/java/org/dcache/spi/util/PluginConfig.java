package org.dcache.spi.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class PluginConfig {

  private static final Logger LOG = LoggerFactory.getLogger(PluginConfig.class);

  private Properties configurationProperties = new Properties();

  public PluginConfig() {
    String dcacheProperties = "dcache.properties";

    // From class Path
    InputStream configInputStream =
        getClass().getClassLoader().getResourceAsStream(dcacheProperties);
    if (configInputStream == null) {
      throw new RuntimeException("Failed to find config file");
    }

    try {
      configurationProperties.load(configInputStream);
    } catch (IOException e) {
      LOG.error("Failed to load configuration from {}: {}", dcacheProperties, e.getMessage());
      throw new RuntimeException(
          String.format("Failed to load configuration: %s", dcacheProperties), e);
    } finally {
      try {
        configInputStream.close();
      } catch (IOException e) {
        LOG.error("Failed to close resource input stream {}: {}", dcacheProperties, e);
        throw new RuntimeException(e);
      }
    }

    // From config directory
    InputStream configFis = null;
    String configFile = "config/" + dcacheProperties;
    try {
      configFis = new FileInputStream(configFile);
      configurationProperties.load(configFis);
    } catch (FileNotFoundException e) {
      LOG.warn("No configuration file {} in current working directory", configFile);
    } catch (IOException e) {
      LOG.error("Failure parsing local config file {}", configFile);
      throw new RuntimeException("Failed to parse local config file " + configFile, e);
    } finally {
      try {
        if (configFis != null) {
          configFis.close();
        }
      } catch (IOException e) {
        // do nothing
      }
    }
  }

  public String get(String parameter) {
    return configurationProperties.getProperty(parameter);
  }
}
