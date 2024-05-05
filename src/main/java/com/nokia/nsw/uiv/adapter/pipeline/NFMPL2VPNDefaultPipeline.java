package com.nokia.nsw.uiv.adapter.pipeline;

import com.nokia.nsw.uiv.adapter.intf.AdapterPipeline;
import com.nokia.nsw.uiv.adapter.transformation.exception.AdapterTransformationException;
import com.nokia.nsw.uiv.rda.discovery.filterconfig.FilterConfiguration.Filters.Filter;
import com.nokia.nsw.uiv.springcontext.SpringBeanUtil;

import java.util.List;

import com.nokia.nsw.uiv.adapter.NFMPL2VPNSeedParam;
import com.nokia.nsw.uiv.adapter.collector.NFMPL2VPNCollector;
import com.nokia.nsw.uiv.adapter.collector.NFMPL2VPNPartialDiscoveryCollector;
import com.nokia.nsw.uiv.adapter.transformation.NFMPL2VPNTransformation;

public class NFMPL2VPNDefaultPipeline extends AdapterPipeline {

	public final static String PIPELINE_NAME = "restpipeline";

	private  NFMPL2VPNCollector adapterCollector;
	private final NFMPL2VPNTransformation adapterTransformation;
	private final NFMPL2VPNSeedParam adapterSeedParam;

	public NFMPL2VPNDefaultPipeline(NFMPL2VPNSeedParam adapterSeedParam)
			throws ClassNotFoundException, AdapterTransformationException {
		this.adapterSeedParam = adapterSeedParam;
		adapterCollector = SpringBeanUtil.getBean(NFMPL2VPNCollector.class);
		adapterTransformation = new NFMPL2VPNTransformation(adapterSeedParam);
	}

	@Override
	public void collect() throws Exception {
		try {
			if (!adapterSeedParam.skipCollection()) {
				
				boolean isPartialDiscoveryRequest=false;
				List<Filter> filters = adapterSeedParam.getPartialDiscoveryFilterRules();
				isPartialDiscoveryRequest = (filters == null) ? false : (filters.isEmpty() ? false : true );
				if(isPartialDiscoveryRequest)
				{
					adapterCollector = new NFMPL2VPNPartialDiscoveryCollector();
				}
				adapterCollector.init(adapterSeedParam);
				
				adapterCollector.assembleData();

				//adapterCollector.fetchData();

				
			}
		} catch (Exception e) {
			throw e;
		} finally {
			if (!adapterSeedParam.skipCollection()) {
				adapterCollector.cleanUp();
			}
		}

	}

	@Override
	public void transform() throws Exception {
		//adapterTransformation.doTransform(adapterSeedParam.getLocalDir());
	}

	@Override
	public void stream() throws Exception {
		// NA
	}

	@Override
	public String getName() {
		return PIPELINE_NAME;
	}

}