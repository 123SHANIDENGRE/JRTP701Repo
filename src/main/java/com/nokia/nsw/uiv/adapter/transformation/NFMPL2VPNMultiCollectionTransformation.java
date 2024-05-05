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
import com.nokia.nsw.uiv.adapterconfig.AdapterTransformationItem;
import com.nokia.nsw.uiv.datatype.Neo4jDomainObject;
import com.nokia.nsw.uiv.exceptions.BatchAbortException;
import com.nokia.nsw.uiv.framework.context.UivSpringContextAware;

import com.nokia.nsw.uiv.adapter.NFMPL2VPN;
import com.nokia.nsw.uiv.adapter.NFMPL2VPNSeedParam;
import com.nokia.nsw.uiv.adapter.si.ssh.NFMPL2VPNSshClient;
import com.nokia.nsw.uiv.adapter.NFMPL2VPNModelClassMapper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NFMPL2VPNMultiCollectionTransformation {

	private final AdapterTransformationFactory<Neo4jDomainObject, Class<? extends Neo4jDomainObject>, String> adapterFactory;
	private final CacheSubSystem cacheSubSystem;
	private final OutputProcessorSubSystem outputProcessorSubSystem;	
	private final NFMPL2VPNSeedParam adapterSeedParam;
	 private static final Logger log = LoggerFactory.getLogger(NFMPL2VPNMultiCollectionTransformation.class);
	public NFMPL2VPNMultiCollectionTransformation(NFMPL2VPNSeedParam adapterSeedParam)
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

	public void doTransform(String rawDumpDir, AdapterTransformationItem adapterTransformationItem)
			throws AdapterTransformationException, BatchAbortException {
		
		NFMPL2VPNAbortListener adapterAbortListener = UivSpringContextAware
				.getBean(NFMPL2VPNAbortListener.class);
		
		FileParserSubSystem fileParserSubSystem = adapterFactory.newFileParserSubSystem(adapterSeedParam.getConfigDir(),
				rawDumpDir, adapterSeedParam.getDiscoveryType().name());

		if (adapterTransformationItem.isEndOfCollection()) {
			adapterAbortListener.checkForAbort(
					"Abort Triggered : Aborting batch before transform for : " + NFMPL2VPN.ADAPTER_NAME);
			log.info("Starting Transformation for {} ...", adapterTransformationItem.getCollectionName());
			fileParserSubSystem.doTransform(adapterTransformationItem.getCollectionName());
			log.info("Completed Transformation for {} ...", adapterTransformationItem.getCollectionName());

			adapterAbortListener.checkForAbort(
					"Abort Triggered : Aborting batch before asscoaition processing for : " + NFMPL2VPN.ADAPTER_NAME);
			log.info("Starting Association ...");
			cacheSubSystem.processAssociations(true);
			log.info("Completed Association ...");

			adapterAbortListener.checkForAbort(
					"Abort Triggered : Aborting batch before cache processing for : " + NFMPL2VPN.ADAPTER_NAME);
			log.info("Executing Cache Processor ...");
			cacheSubSystem.executeCacheProcessor(true);
			log.info("Completed Cache Processor ...");

			adapterAbortListener
					.checkForAbort("Abort Triggered : Aborting batch before cached entities are streamed for : "
							+ NFMPL2VPN.ADAPTER_NAME);
			log.info("Starting push entities to output ...");
			cacheSubSystem.pushEntitiesToOutput(true);
			log.info("Completed push entities to output ...");

			adapterAbortListener
					.checkForAbort("Abort Triggered : Aborting batch before output processor cleanup for : "
							+ NFMPL2VPN.ADAPTER_NAME);
			log.info("Calling Output Processor Cleanup ...");
			outputProcessorSubSystem.cleanUp();

			printEntityStatusCache();

			adapterAbortListener.checkForAbort(
					"Abort Triggered : Aborting batch before cache cleanup for : " + NFMPL2VPN.ADAPTER_NAME);
			log.info("Calling Cache Sub System Cleanup ...");
			cacheSubSystem.cleanUp();

			log.info("Done");

		} else {
			adapterAbortListener.checkForAbort(
					"Abort Triggered : Aborting batch before transform for : " + NFMPL2VPN.ADAPTER_NAME);
			log.info("Starting Transformation for {} ...", adapterTransformationItem.getCollectionName());
			fileParserSubSystem.doTransform(adapterTransformationItem.getCollectionName());
			log.info("Completed Transformation for {} ...", adapterTransformationItem.getCollectionName());

			adapterAbortListener.checkForAbort(
					"Abort Triggered : Aborting batch before asscoaition processing for : " + NFMPL2VPN.ADAPTER_NAME);
			log.info("Starting Association ...");
			cacheSubSystem.processAssociations();
			log.info("Completed Association ...");

			adapterAbortListener.checkForAbort(
					"Abort Triggered : Aborting batch before cache processing for : " + NFMPL2VPN.ADAPTER_NAME);
			log.info("Executing Cache Processor ...");
			cacheSubSystem.executeCacheProcessor(adapterTransformationItem.getCollectionName());
			log.info("Completed Cache Processor ...");

			adapterAbortListener
					.checkForAbort("Abort Triggered : Aborting batch before cached entities are streamed for : "
							+ NFMPL2VPN.ADAPTER_NAME);
			log.info("Starting push entities to output ...");
			cacheSubSystem.pushEntitiesToOutput();
			log.info("Completed push entities to output ...");

			printEntityStatusCache();

			adapterAbortListener
					.checkForAbort("Abort Triggered : Aborting batch before output processor cleanup for : "
							+ NFMPL2VPN.ADAPTER_NAME);
			log.info("Calling Output Processor Inter Collection  Cleanup ...");
			outputProcessorSubSystem.cleanUp(true);
			log.info("Completed Output Processor Inter Collection  Cleanup ...");

			adapterAbortListener.checkForAbort(
					"Abort Triggered : Aborting batch before cache cleanup for : " + NFMPL2VPN.ADAPTER_NAME);
			log.info("Starting Cache Sub System Inter Collection Cleanup ...");
			cacheSubSystem.cleanUp(true);
			log.info("Completed Cache Sub System Inter Collection Cleanup ...");
		}

	}

	private void printEntityStatusCache() {
		EntityStateCache<Class<? extends Neo4jDomainObject>, String> entityStateCache = adapterFactory.getAdapterCache()
				.getEntityStateCache();
		Map<String, Map<EntityState.State, Integer>> entityStateStatusMap = entityStateCache.getEntityStateStats();

		log.info("Adapter Status");
		for (Map.Entry<String, Map<EntityState.State, Integer>> entityStateStats : entityStateStatusMap.entrySet()) {
			log.info("\tEntity Type : {} ", entityStateStats.getKey());
			for (Map.Entry<EntityState.State, Integer> entityState : entityStateStats.getValue().entrySet()) {
				log.info("\t\tState : {}, Count : {} ", entityState.getKey(), entityState.getValue());
			}
		}

	}
}
