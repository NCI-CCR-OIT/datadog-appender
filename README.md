# datadog-appender

Custom [Logback](https://logback.qos.ch/index.html) appender that uses the [Datadog HTTP v2 API](https://docs.datadoghq.com/api/latest/logs/#send-logs) (via the [Datadog Java client](https://github.com/DataDog/datadog-api-client-java)) to send log entries to Datadog.

## Usage ##

### Maven ###

This library's Maven artifacts are hosted on Github Projects, so you will first need to make sure you have the Github Projects URL for the repository included in your Maven settings, which usually is done via a `profile` in your `~/.m2/settings.xml` file.

```
<profile>
	<id>github</id>
	<activation>
		<activeByDefault>true</activeByDefault>
	</activation>
	<repositories>
		<repository>
			<id>github-datadog-appender</id>
			<name>GitHub Packages (Datadog Appender)</name>
			<url>https://maven.pkg.github.com/NCI-CCR-OIT/datadog-appender</url>
		</repository>
	</repositories>
</profile>
```

Once that is configured, you can just include it as a dependency in your Maven project:

```
<dependency>
	<groupId>gov.cancer.ccr.oit</groupId>
	<artifactId>datadog-appender</artifactId>
	<version>1.0.1</version>
</dependency>
```

Finally, configure Logback in your project to include it as an appender, and include that appender in whatever loggers and/or root that you want to use it:

```
<appender name="DatadogAppender" class="gov.cancer.ccr.oit.logback.datadogappender.service.DatadogAppender">
	<apiKey>YOUR_API_KEY</apiKey>
	<service>Your App Name</service>
</appender>
<root level="INFO">
	<appender-ref ref="DatadogAppender">
</root>
```

### Configuration options

* `apiKey`: Datadog API key *(required)*
* `datadogSite`: the [Datadog site parameter](https://docs.datadoghq.com/getting_started/site/?site=gov) for your tenant *(default: `ddog-gov.com`)*
* `source`: the source for the log events, corresponding to Datadog's `source` [reserved attribute](https://docs.datadoghq.com/logs/log_configuration/attributes_naming_convention/#reserved-attributes) *(default: `java`)*
* `service`: the name of the application or service generating the log events, corresponding to Datadog's `service` [reserved attribute](https://docs.datadoghq.com/logs/log_configuration/attributes_naming_convention/#reserved-attributes) *(default `<YOUR APP NAME>`, as a reminder to set this for your application or service)*
* `hostname`: the hostname where the application or service is running *(default is the local hostname via `InetAddress.getLocalHost().getHostName()`)*
* `tags`: a comma-separated list of tag name:value pairs to include with all log entries (e.g., `env:prod,appVersion:1.1.0`; default is empty)
* `debugMode`: boolean to enable [Datadog's Java client request logging mode](https://github.com/DataDog/datadog-api-client-java?tab=readme-ov-file#enable-requests-logging) (default `false`)

## Log event content

The appender is [MDC](https://logback.qos.ch/manual/mdc.html)-aware, so any information you attach to the MDC before sending a log event will be added to the log event inside a `data` custom property object; this allows you to add metadata to log events, like trace IDs, usernames, or other useful information.

Each log event will also include a few additional properties:

* `label`: statically set to `logback`
* `sourcepath`: the full path of the class, method, and line number where the log event was generated
* `status`: the log level of the event (e.g., `info`, `debug`, etc.)
* `timestamp`: an ISO-8601 timestamp of the log event (in UTC)
