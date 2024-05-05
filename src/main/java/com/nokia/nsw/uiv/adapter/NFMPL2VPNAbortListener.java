package com.nokia.nsw.uiv.adapter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.nokia.nsw.uiv.exceptions.BatchAbortException;
import com.nokia.nsw.uiv.uivevents.AdapterEvent;
import com.nokia.nsw.uiv.uivevents.EventType;

import lombok.extern.slf4j.Slf4j;

import com.nokia.nsw.uiv.adapter.NFMPL2VPN;
import com.nokia.nsw.uiv.adapter.NFMPL2VPNSeedParam;

@Slf4j
@Component
@Profile("FloeAdapters")
public class NFMPL2VPNAbortListener implements ApplicationListener<AdapterEvent>{
	
	private boolean abortTriggered = false;
	private String operationId = null;
	 private static final Logger log = LoggerFactory.getLogger(NFMPL2VPNAbortListener.class);
	public void init(NFMPL2VPNSeedParam adapterSeedParam) {
		this.abortTriggered = false;
		this.operationId = adapterSeedParam.getOperationId();
	}

	@Override
	public void onApplicationEvent(AdapterEvent event) {
		if (event != null && event.getEvent() == EventType.ABORT && event.getOperationId().equals(operationId)
				&& event.getTarget().equals(NFMPL2VPN.ADAPTER_NAME)) {
			log.info("NFMPL2VPN: Received Adapter Abort Event: {}, Target: {}, OperationId: {}", event.getEvent(),
					event.getOperationId(), event.getTarget());
			abortTriggered = true;
		}
	}

	public void checkForAbort(String message) throws BatchAbortException {
		if (abortTriggered) {
			throw new BatchAbortException(message);
		}
	}
	
}