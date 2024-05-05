package com.nokia.nsw.uiv.adapter.util;

import org.apache.commons.lang3.StringUtils;
import org.springframework.context.support.GenericXmlApplicationContext;
import com.nokia.nsw.uiv.rda.configuration.RDAConfiguration;
import com.nokia.nsw.uiv.rda.configuration.RefreshableRDAConfig;

import com.nokia.nsw.uiv.adapter.util.UivRdaAdapterMock;

public class NFMPL2VPNAdapterApp {

	public static final String TEST_PROFILE = "AdapterSaTest";

	static final String[] ACTIVE_PROFILE = { "CORE", "RDA", "FloeAdapters", TEST_PROFILE };

	public static void main(String[] args) throws Exception {

		GenericXmlApplicationContext context = new GenericXmlApplicationContext();
		context.getEnvironment().setActiveProfiles(ACTIVE_PROFILE);
		context.load("NFMPL2VPNAppContext.xml");
                context.getBeanFactory().registerScope("refresh", new org.springframework.cloud.context.scope.refresh.RefreshScope());                           
                 context.registerBean(RefreshableRDAConfig.class);
                 context.registerBean(RDAConfiguration.class);
                 context.refresh();

		UivRdaAdapterMock uivRdaAdapterMock = context.getBean(UivRdaAdapterMock.class);
		uivRdaAdapterMock.doTest("NFMPL2VPN", "NFMPL2VPNAdapterConfig.yaml");
		
		context.close();
	}

}