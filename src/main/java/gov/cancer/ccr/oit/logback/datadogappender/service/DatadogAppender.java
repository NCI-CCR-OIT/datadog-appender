package gov.cancer.ccr.oit.logback.datadogappender.service;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.datadog.api.client.ApiClient;
import com.datadog.api.client.ApiException;
import com.datadog.api.client.v2.api.LogsApi;
import com.datadog.api.client.v2.model.HTTPLogItem;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;

public class DatadogAppender extends AppenderBase<ILoggingEvent> {

	private LogsApi apiInstance;
	private String datadogSite = "ddog-gov.com";
	private String apiKey;
	private String source = "java";
	private String service = "<YOUR APP NAME>";
	private String hostname;
	private String tags;
	private boolean debugMode = false;

	public LogsApi getApiInstance() {
		return apiInstance;
	}

	public String getApiKey() {
		return apiKey;
	}

	public String getDatadogSite() {
		return datadogSite;
	}

	public String getHostname() {
		if (hostname != null) {
			return hostname;
		} else {
			try {
				return InetAddress.getLocalHost().getHostName();
			} catch (UnknownHostException e) {
				return "unknown-hostname";
			}
		}
	}

	public String getService() {
		return service;
	}

	public String getSource() {
		return source;
	}

	public String getTags() {
		return tags;
	}

	public boolean isDebugMode() {
		return debugMode;
	}

	public void setApiKey(String apiKey) {
		this.apiKey = apiKey;
	}

	public void setDatadogSite(String datadogSite) {
		this.datadogSite = datadogSite;
	}

	public void setDebugMode(boolean debugMode) {
		this.debugMode = debugMode;
	}

	public void setHostname(String hostname) {
		this.hostname = hostname;
	}

	public void setService(String service) {
		this.service = service;
	}

	public void setSource(String source) {
		this.source = source;
	}

	public void setTags(String tags) {
		this.tags = tags;
	}

	@Override
	public void start() {
		if (this.getApiKey() == null) {
			addError("No Datadog API key set; unable to start Datadog log appender.");
			return;
		}

		ApiClient defaultClient = ApiClient.getDefaultApiClient();

		defaultClient.enableRetry(true);
		if (this.isDebugMode()) {
			defaultClient.setDebugging(true);
		}

		HashMap<String, String> serverVariables = new HashMap<>();
		serverVariables.put("site", this.getDatadogSite());
		defaultClient.setServerVariables(serverVariables);

		HashMap<String, String> secrets = new HashMap<>();
		secrets.put("apiKeyAuth", this.apiKey);
		defaultClient.configureApiKeys(secrets);

		this.apiInstance = new LogsApi(defaultClient);

		addInfo("Starting Datadog logback appender, with Datadog site " + this.getDatadogSite());
		super.start();
	}

	@Override
	protected void append(ILoggingEvent eventObject) {

		String message = eventObject.getFormattedMessage();
		String level = eventObject.getLevel().toString();
		String timestamp = eventObject.getInstant().toString();

		HTTPLogItem item = new HTTPLogItem().ddsource(this.getSource())
				.hostname(this.getHostname()).message(message).service(this.getService())
				.putAdditionalProperty("timestamp", timestamp).putAdditionalProperty("status", level)
				.putAdditionalProperty("label", "logback")
				.putAdditionalProperty("sourcepath", getBacktraceData(eventObject));
		if (this.getTags() != null) {
			item.ddtags(this.getTags());
		}

		ConcurrentMap<String, Object> data = null;
		if (eventObject.getMDCPropertyMap() != null
				&& eventObject.getMDCPropertyMap().size() > 0) {
			data = new ConcurrentHashMap<>();
			Map<String, String> mdcPropertyMap = eventObject.getMDCPropertyMap();
			for (Map.Entry<String, String> entry : mdcPropertyMap.entrySet()) {
				String k = entry.getKey();
				String v = entry.getValue();
				storeInDataMap(k, v, data);
			}
		}
		if (data != null) {
			item.putAdditionalProperty("data", data);
		}

		List<HTTPLogItem> body = Collections.singletonList(item);

		try {
			this.apiInstance.submitLog(body);
		} catch (ApiException e) {
			addError(e.getMessage());
		}
	}

	@SuppressWarnings("unchecked")
	public static void storeInDataMap(String k, Object v, Map<String, Object> data) {
		String[] kParts = k.split("\\.");
		Map<String, Object> dest = data;
		for (int i = 0; i < (kParts.length - 1); i++) {
			String kPart = kParts[i];
			if (!dest.containsKey(kPart)) {
				Map<String, Object> subData = new HashMap<>();
				dest.put(kPart, subData);
				dest = subData;
			} else {
				if (dest.get(kPart) instanceof HashMap) {
					dest = (HashMap<String, Object>) dest.get(kPart);
				} else {
					Map<String, Object> subData = new HashMap<>();
					subData.put("_root", dest.get(kPart).toString());
					dest.put(kPart, subData);
					dest = subData;
				}
			}
		}
		dest.put(kParts[kParts.length - 1], v);
	}

	public static String getBacktraceData(ILoggingEvent event) {
		String backtraceData = null;
		if (event.hasCallerData()) {
			StackTraceElement[] stes = event.getCallerData();
			StackTraceElement elem = stes[0];
			backtraceData = elem.getClassName() + "#" + elem.getMethodName() + ":"
					+ elem.getLineNumber();
		}
		return backtraceData;
	}
}
