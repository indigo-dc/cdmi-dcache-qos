/*
 * Copyright 2016 Deutsches Elektronen-Synchrotron (DESY)
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package org.dcache.spi;

import java.util.Map;
import org.indigo.cdmi.SubjectBasedStorageBackend;
import org.indigo.cdmi.spi.StorageBackend;
import org.indigo.cdmi.spi.StorageBackendFactory;

public class dCacheStorageBackendFactory implements StorageBackendFactory {

  private final String type = "dCache";
  private final String description = "CDMI Spi plugin for dCache Storage System";

  @Override
  public StorageBackend createStorageBackend(Map<String, String> map)
      throws IllegalArgumentException {
    return new SubjectBasedStorageBackend(new dCacheStorageBackend());
  }

  @Override
  public String getDescription() {
    return this.description;
  }

  @Override
  public String getType() {
    return this.type;
  }
}
