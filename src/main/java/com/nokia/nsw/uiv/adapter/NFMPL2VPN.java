package com.nokia.nsw.uiv.adapter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.nokia.nsw.uiv.adapter.intf.AdapterPipeline;
import com.nokia.nsw.uiv.adapterworkflow.metadata.WorkflowData;
import com.nokia.oss.sure.adapter.intf.AdapterInterface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.nokia.nsw.uiv.adapter.NFMPL2VPNAbortListener;
import com.nokia.nsw.uiv.adapter.NFMPL2VPNSeedParam;
import com.nokia.oss.sure.adapter.common.FloeAdapterConstants;
import com.nokia.nsw.uiv.adapter.pipeline.NFMPL2VPNDefaultPipeline;
import com.nokia.nsw.uiv.adapter.pipeline.NFMPL2VPNSftpPipeline;
import com.nokia.nsw.uiv.adapter.transformation.NFMPL2VPNTransformation;
import com.nokia.nsw.uiv.adapter.transformation.exception.AdapterTransformationException;
import com.nokia.nsw.uiv.exceptions.BatchAbortException;
import java.util.Map;
import java.util.List;
import com.nokia.nsw.uiv.rda.discovery.filterconfig.FilterConfiguration;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@Profile("FloeAdapters")
public class NFMPL2VPN implements AdapterInterface {
	
	public static final String ADAPTER_NAME = NFMPL2VPN.class.getSimpleName();
	public static NFMPL2VPNSeedParam adapterSeedParam;
	
	 private static final Logger log = LoggerFactory.getLogger(NFMPL2VPN.class);
	@Autowired
	NFMPL2VPNAbortListener adapterAbortListener;
	String collectionPipeline = null; 
	protected Map<String, String> hints;

	@Override
	public void execute(WorkflowData workflowData) throws Exception {
		log.info("Process Request : {}", workflowData);

		workflowData.getConfiguration().setGlobalStreamingallowed(true);
		workflowData.getConfiguration().setLastPiplineExecuted(true);
		
		adapterSeedParam = new NFMPL2VPNSeedParam(workflowData);
		
		adapterAbortListener.init(adapterSeedParam);
		hints = adapterSeedParam.getSeedFeatureMap();
		log.info("Printing Adapter Seed Features");
		for (String x: hints.keySet()) {
			String value = hints.get(x);
			log.info(x + " " + value);
		}

		String[] pipelines = adapterSeedParam.getPipeline();
		
		for (String pipeline : pipelines){
			if (!pipeline.equalsIgnoreCase(NFMPL2VPNDefaultPipeline.PIPELINE_NAME) && !pipeline.equalsIgnoreCase(NFMPL2VPNSftpPipeline.PIPELINE_NAME)) {
				throw new Exception("not a valid pipeline");
			}
		}
		boolean isPartialDiscoveryRequest=false;
		List<FilterConfiguration.Filters.Filter> filters = adapterSeedParam.getPartialDiscoveryFilterRules();
		isPartialDiscoveryRequest = filters!=null && (filters.isEmpty() ? false : true );
		log.info("isPartialDiscoveryRequest : " + isPartialDiscoveryRequest);
		if(isPartialDiscoveryRequest)
		{
			for (String pipeline : pipelines){
				if (pipeline.equals(NFMPL2VPNSftpPipeline.PIPELINE_NAME)) {
					throw new Exception(pipeline + " is set so cannot do partial discovery");
				}
			}
			if (adapterSeedParam.skipCollection()){
				throw new Exception("skipcollection is set to True so cannot do partial discovery");
			}
		}
				
		AdapterPipeline adapterPipeline = null;
		
		for(String pipeline : pipelines) {
			if(NFMPL2VPNSftpPipeline.PIPELINE_NAME.equalsIgnoreCase(pipeline)) {
				adapterPipeline = new NFMPL2VPNSftpPipeline(adapterSeedParam);
				collectionPipeline=pipeline;
			} 
			if(NFMPL2VPNDefaultPipeline.PIPELINE_NAME.equalsIgnoreCase(pipeline)) {
				adapterPipeline = new NFMPL2VPNDefaultPipeline(adapterSeedParam);
				collectionPipeline=pipeline;
			}
			adapterPipeline.execute();
			transform(adapterSeedParam);
		}
	
	}

	private void transform(NFMPL2VPNSeedParam adapterSeedParam)
			throws ClassNotFoundException, AdapterTransformationException, BatchAbortException {
		NFMPL2VPNTransformation adapterTransformation = new NFMPL2VPNTransformation(adapterSeedParam);
		if(collectionPipeline.equalsIgnoreCase(NFMPL2VPNDefaultPipeline.PIPELINE_NAME))
		    adapterTransformation.doTransform(adapterSeedParam.getSeedFeatureMap().get(FloeAdapterConstants.LOCAL_TARGET_DIRECTORY));
		if(collectionPipeline.equalsIgnoreCase(NFMPL2VPNSftpPipeline.PIPELINE_NAME))
			adapterTransformation.doTransform(adapterSeedParam.getLocalDir());	
	}

}