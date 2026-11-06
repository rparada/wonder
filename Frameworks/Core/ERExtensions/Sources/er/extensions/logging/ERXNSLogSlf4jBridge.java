package er.extensions.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.webobjects.foundation.NSLog;

import er.extensions.foundation.ERXProperties;

/**
 * Sends {@link NSLog} output to the SLF4J logger named {@code NSLog}.
 * Log configuration stays with the application; this class only forwards
 * {@code NSLog.out}, {@code NSLog.err} and {@code NSLog.debug}.
 *
 * @property er.extensions.ERXNSLogSlf4jBridge.ignoreNSLogSettings if true, NSLog's enabled flag and debug level are not consulted when deciding whether to forward a message. {@code er.extensions.ERXNSLogLog4jBridge.ignoreNSLogSettings} is honored as well.
 */
public class ERXNSLogSlf4jBridge extends NSLog.PrintStreamLogger {
	public static final Logger log = LoggerFactory.getLogger("NSLog");
	public static final int OUT = 1;
	public static final int ERR = 2;
	public static final int DEBUG = 3;
	private final int type;

	/**
	 * Installs the bridge on {@link NSLog} if it is not already installed, and
	 * reapplies the current NSLog debug level so the SLF4J logger stays in step.
	 */
	public static void install() {
		int allowedLevel = NSLog.debug.allowedDebugLevel();

		if (!(NSLog.debug instanceof ERXNSLogSlf4jBridge)) {
			NSLog.setOut(new ERXNSLogSlf4jBridge(OUT));
			NSLog.setErr(new ERXNSLogSlf4jBridge(ERR));
			NSLog.setDebug(new ERXNSLogSlf4jBridge(DEBUG));
		}

		NSLog.debug.setAllowedDebugLevel(allowedLevel);
	}

	public ERXNSLogSlf4jBridge(int type) {
		super();

		this.type = type;
	}

	@Override
	public void appendln(Object obj) {
		if (obj == null) {
			return;
		}

		if (isEnabled()) {
			switch (type) {
			case OUT:
				log.info(obj.toString());
				break;
			case ERR:
				log.error(obj.toString());
				break;
			case DEBUG:
				log.debug(obj.toString());
				break;
			}
		}
		else {
			if (type == ERR)
				log.error(obj.toString());
		}
	}

	@Override
	public void appendln() {
		appendln(""); // Assuming people will always put "%n" at the end of the layout pattern.
	}

	@Override
	public void flush() {
	}

	private static boolean ignoreNSLogSettings() {
		return ERXProperties.booleanForKeyWithDefault("er.extensions.ERXNSLogSlf4jBridge.ignoreNSLogSettings", false)
				|| ERXProperties.booleanForKeyWithDefault("er.extensions.ERXNSLogLog4jBridge.ignoreNSLogSettings", false);
	}
}
