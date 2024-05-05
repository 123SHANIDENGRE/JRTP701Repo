package com.nokia.nsw.uiv.adapter.transformation;

import java.io.File;
import java.util.ArrayList;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nokia.nsw.uiv.adapter.transformation.AdapterTransformationFactory;
import com.nokia.nsw.uiv.adapter.transformation.cache.EntityState;
import com.nokia.nsw.uiv.adapter.transformation.exception.AdapterTransformationException;
import com.nokia.nsw.uiv.adapter.transformation.intf.CacheSubSystem;
import com.nokia.nsw.uiv.adapter.transformation.intf.EntityStateCache;
import com.nokia.nsw.uiv.adapter.transformation.intf.FileParserSubSystem;
import com.nokia.nsw.uiv.adapter.transformation.intf.OutputProcessorSubSystem;
import com.nokia.nsw.uiv.adapter.NFMPL2VPNAbortListener;
import com.nokia.nsw.uiv.datatype.Neo4jDomainObject;
import com.nokia.nsw.uiv.exceptions.BatchAbortException;
import com.nokia.nsw.uiv.framework.context.UivSpringContextAware;

import com.nokia.nsw.uiv.adapter.NFMPL2VPN;
import com.nokia.nsw.uiv.adapter.NFMPL2VPNSeedParam;
import com.nokia.nsw.uiv.adapter.NFMPL2VPNModelClassMapper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NFMPL2VPNTransformation {

	private final AdapterTransformationFactory<Neo4jDomainObject, Class<? extends Neo4jDomainObject>, String> adapterFactory;
	private final CacheSubSystem cacheSubSystem;
	private final OutputProcessorSubSystem outputProcessorSubSystem;	
	private final NFMPL2VPNSeedParam adapterSeedParam;
	private static final Logger log = LoggerFactory.getLogger(NFMPL2VPNTransformation.class);
	public NFMPL2VPNTransformation(NFMPL2VPNSeedParam adapterSeedParam)
			throws ClassNotFoundException, AdapterTransformationException {
		this.adapterSeedParam = adapterSeedParam;
		NFMPL2VPNModelClassMapper modelClassMapper = new NFMPL2VPNModelClassMapper();
		String adapterConfigFile = adapterSeedParam.getConfigDir() + File.separator + adapterSeedParam.getConfigFile();

		this.adapterFactory = AdapterTransformationFactory.newInstance(adapterConfigFile,
				new ArrayList<>(modelClassMapper.getAssociationOrder()),
				new ArrayList<>(modelClassMapper.getOutputOrder()), adapterSeedParam.getBatchId());
		this.cacheSubSystem = adapterFactory.newCacheSubSystem();
		
		if (StringUtils.isNotBlank(adapterSeedParam.getOutputMode())
				&& adapterSeedParam.getOutputMode().equalsIgnoreCase(NFMPL2VPNSeedParam.MDL_OUTPUT)) {
			outputProcessorSubSystem = adapterFactory.newOutputProcessorSubSystem(adapterSeedParam.getLocalDir(),
					adapterSeedParam.getBatchId());
		} else {
			outputProcessorSubSystem = adapterFactory.newOutputProcessorSubSystem(adapterSeedParam.getInventoryOperation());
		}		
	}

	public void doTransform(String rawDumpDir) throws AdapterTransformationException, BatchAbortException {
		
		NFMPL2VPNAbortListener adapterAbortListener = UivSpringContextAware
				.getBean(NFMPL2VPNAbortListener.class);
				
		FileParserSubSystem fileParserSubSystem = adapterFactory.newFileParserSubSystem(adapterSeedParam.getConfigDir(),
				rawDumpDir, adapterSeedParam.getDiscoveryType().name());

		adapterAbortListener.checkForAbort(
				"Abort Triggered : Aborting batch before transform for : " + NFMPL2VPN.ADAPTER_NAME);
		log.info("Starting Transformation ...");
		fileParserSubSystem.doTransform();
		log.info("Completed Transformation ...");

		adapterAbortListener.checkForAbort(
				"Abort Triggered : Aborting batch before asscoaition processing for : " + NFMPL2VPN.ADAPTER_NAME);
		log.info("Starting Association ...");
		cacheSubSystem.processAssociations();
		log.info("Completed Association ...");

		adapterAbortListener.checkForAbort(
				"Abort Triggered : Aborting batch before cache processing for : " + NFMPL2VPN.ADAPTER_NAME);
		log.info("Executing Cache Processor ...");
		cacheSubSystem.executeCacheProcessor();
		log.info("Completed Cache Processor ...");

		adapterAbortListener.checkForAbort(
				"Abort Triggered : Aborting batch before cached entities are streamed for : " + NFMPL2VPN.ADAPTER_NAME);
		log.info("Starting push entities to output ...");
		cacheSubSystem.pushEntitiesToOutput();
		log.info("Completed push entities to output ...");

		adapterAbortListener.checkForAbort(
				"Abort Triggered : Aborting batch before output processor cleanup for : " + NFMPL2VPN.ADAPTER_NAME);
		log.info("Calling Output Processor Cleanup ...");
		outputProcessorSubSystem.cleanUp();
		adapterAbortListener.checkForAbort(
				"Abort Triggered : Aborting batch before cache cleanup for : " + NFMPL2VPN.ADAPTER_NAME);
		log.info("Calling Cache Sub System Cleanup ...");
		cacheSubSystem.cleanUp();

		log.info("Done");
	}
}