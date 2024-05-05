package com.nokia.nsw.uiv.adapter.headerprocessor;

import java.nio.charset.Charset;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.nokia.nsw.uiv.adapter.transformation.intf.FileHeaderProcessorHandler;

public class NFMPL2VPNFileHeaderProcessor implements FileHeaderProcessorHandler {

	static Pattern xmlnsPattern = Pattern.compile("xmlns(?!\\s*:)(\\s*=\\s*\"[^\"]*\")");

	@Override
	public boolean processHeader(byte[] bytes, Charset charSet) {
		String xmlStr = new String(bytes);
		Matcher m = xmlnsPattern.matcher(xmlStr);
		if (m.find()) {
			byte[] repBytes = m
					.replaceAll(new String(new char[m.group(1).length() + "xmlns".length()]).replace("\0", " "))
					.getBytes();
			System.arraycopy(repBytes, 0, bytes, 0, repBytes.length);
			return true;
		}

		return false;
	}

}