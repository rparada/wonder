package er.extensions.foundation;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.net.URL;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import junit.framework.TestCase;

import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;
import org.w3c.dom.Document;

import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSData;
import com.webobjects.foundation.NSMutableDictionary;

import er.extensions.foundation.ERXPropertyListSerialization.PListFormat;

/**
 * Compares the JDK XML written for a binary property list with the Xerces
 * serializer previously used by ERXPropertyListSerialization.
 */
public class ERXPropertyListSerializationXmlTest extends TestCase {

	private static final Pattern NUMERIC_CHARACTER_REFERENCE = Pattern.compile("&#(?:x([0-9a-fA-F]+)|([0-9]+));");

	public void testBinaryPropertyListXmlMatchesXerces() throws Exception {
		NSMutableDictionary<String, Object> nested = new NSMutableDictionary<>();
		nested.setObjectForKey("inner", "child");

		NSMutableDictionary<String, Object> plist = new NSMutableDictionary<>();
		plist.setObjectForKey("français <tag> & \"quote\" 'apos' > end", "name");
		plist.setObjectForKey("", "empty");
		StringBuilder longText = new StringBuilder();
		for (int i = 0; i < 40; i++) {
			longText.append("word ");
		}
		plist.setObjectForKey(longText.toString(), "long");
		plist.setObjectForKey("line\nbreak", "lines");
		plist.setObjectForKey(Integer.valueOf(42), "count");
		plist.setObjectForKey(Double.valueOf(1.25), "real");
		plist.setObjectForKey(Boolean.TRUE, "yes");
		plist.setObjectForKey(Boolean.FALSE, "no");
		plist.setObjectForKey(new NSData(new byte[] { 0, 1, 2, (byte) 255 }), "data");
		plist.setObjectForKey(new Date(0), "when");
		plist.setObjectForKey(new NSArray<>(new Object[] { "a&b", "" }), "items");
		plist.setObjectForKey(nested, "nested");

		String[] xml = xmlFromBinaryPlist(plist);
		assertEquals(xml[0], xml[1]);
	}

	public void testCharacterReferencesMatchXercesExceptForSpelling() throws Exception {
		NSMutableDictionary<String, Object> plist = new NSMutableDictionary<>();
		plist.setObjectForKey("a\rb", "cr");
		plist.setObjectForKey("smile \uD83D\uDE00", "emoji");

		String[] xml = xmlFromBinaryPlist(plist);
		assertEquals(canonicalNumericCharacterReferences(xml[0]), canonicalNumericCharacterReferences(xml[1]));
	}

	/** @return Xerces XML at index 0 and JDK XML at index 1, from one binary plist */
	private static String[] xmlFromBinaryPlist(NSMutableDictionary<String, Object> plist) throws IOException {
		File file = File.createTempFile("ERXPropertyListSerialization", ".plist");
		try {
			try (OutputStream out = new FileOutputStream(file)) {
				ERXPropertyListSerialization.writePropertyListToStream(plist, out, PListFormat.NSPropertyListBinaryFormat_v1_0, "UTF-8");
			}
			URL url = file.toURI().toURL();
			Document document = ERXPropertyListSerialization.documentWithBinaryPropertyListURL(url);
			StringWriter stringOut = new StringWriter();
			OutputFormat format = new OutputFormat(document);
			XMLSerializer serial = new XMLSerializer(stringOut, format);
			serial.asDOMSerializer();
			serial.serialize(document.getDocumentElement());
			String jdkXml = ERXPropertyListSerialization.xmlStringWithBinaryPropertyListURL(url);
			return new String[] { stringOut.toString(), jdkXml };
		} finally {
			file.delete();
		}
	}

	/**
	 * Xerces writes hexadecimal character references ({@code &#xd;}, {@code &#x1f600;}).
	 * The JDK transformer writes decimal ones ({@code &#13;}, {@code &#128512;}).
	 */
	private static String canonicalNumericCharacterReferences(String xml) {
		Matcher matcher = NUMERIC_CHARACTER_REFERENCE.matcher(xml);
		StringBuffer buffer = new StringBuffer();
		while (matcher.find()) {
			int codePoint = matcher.group(1) != null
					? Integer.parseInt(matcher.group(1), 16)
					: Integer.parseInt(matcher.group(2));
			matcher.appendReplacement(buffer, Matcher.quoteReplacement(String.format("&#x%x;", codePoint)));
		}
		matcher.appendTail(buffer);
		return buffer.toString();
	}
}
