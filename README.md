This plugin changes getters for properties that are not optional singletons in an XSD file (*minOccurs = 0* and *maxOccurs = 1*) to return an `Optional` value.

Example use in Maven when building with the *maven-jaxb2-plugin*:

```xml 
<build>
  <plugins>
    <plugin>
      <groupId>org.jvnet.jaxb2.maven2</groupId>
      <artifactId>maven-jaxb2-plugin</artifactId>
      <version>LATEST</version>
      <executions>
        <execution>
          <id>xsd-generate</id>
          <phase>generate-sources</phase>
          <goals>
            <goal>generate</goal>
          </goals>
        </execution>
      </executions>
      <configuration>
        <extension>true</extension>
	<args>
	  <arg>-Xnullsafegetters</arg>
        </args>
        <plugins>
          <plugin>
            <groupId>codes.rafael.xjc</groupId>
            <artifactId>nullsafe</artifactId>
            <version>LATEST</version>
          </plugin>
        </plugins>
      </configuration>
    </plugin>
  </plugins>
</build>
```

Under the Apache 2.0 license.
