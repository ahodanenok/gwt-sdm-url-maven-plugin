# GWT SDM URL Maven Plugin

This maven plugin allows to change URL, which application uses to locate SDM CodeServer.

By default `host` is taken from `$wnd.location.hostname` which is ok if application could be launched locally.  
But if this is not possible and application needed to be deployed in some remote environment,
it adds some difficulties in using SDM locally, because `host` will no longer be `localhost`.  
Using this plugin `host` could be set to `localhost` or other as needed.

```
<plugin>
  <groupId>ahodanenok.maven.plugins</groupId>
  <artifactId>gwt-sdm-url-maven-plugin</artifactId>
  <version>1.0.0</version>
  <executions>
    <execution>
      <goals>
        <goal>setGwtSdmUrl</goal>
      </goals>
    </execution>
  </executions>
  <configuration>
    <module>testModule</module>
    <host>localhost</host>
    <port>9876</port>
    <webappDirectory>${project.build.directory}/${project.build.finalName}</webappDirectory>
  </configuration>
</plugin>
```

All configuration parameters are optional, except module.
Default values:
* host: `localhost`
* port: `9876`
* webappDirectory: `${project.build.directory}/${project.build.finalName}`

Supported versions:
* Maven: 3+
* GWT: 2.7+
