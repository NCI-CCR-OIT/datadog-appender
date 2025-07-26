package gov.cancer.ccr.oit.logback.datadogappender;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Tester {

	public static Logger log = LoggerFactory.getLogger(Tester.class);

	public static void main(String[] args) {
		log.info("Hello");
		log.error("SOMETHING BAD HAPPENED!");
		log.warn("Be warned");
		log.debug("This is some inside info.");
	}

}
