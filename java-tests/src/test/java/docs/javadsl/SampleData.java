/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * license agreements; and to You under the Apache License, version 2.0:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * This file is part of the Apache Pekko project, derived from Akka.
 */

/*
 * Copyright (C) 2014 - 2016 Softwaremill <https://softwaremill.com>
 * Copyright (C) 2016 - 2020 Lightbend Inc. <https://www.lightbend.com>
 */

package docs.javadsl;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

public class SampleData {

  public final String name;
  public final int value;

  @JsonCreator
  public SampleData(@JsonProperty("name") String name, @JsonProperty("value") int value) {
    this.name = name;
    this.value = value;
  }

  @Override
  public String toString() {
    return "SampleData(name=" + name + ",value=" + value + ")";
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    SampleData that = (SampleData) o;
    return value == that.value && Objects.equals(name, that.name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, value);
  }
}
