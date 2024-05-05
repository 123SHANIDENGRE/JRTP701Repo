package com.nokia.nsw.uiv.adapter;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import com.nokia.nsw.uiv.adapter.transformation.intf.ModelClassMapper;
import com.nokia.nsw.uiv.datatype.Neo4jDomainNodeObject;
import com.nokia.nsw.uiv.datatype.Neo4jDomainObject;
import com.nokia.nsw.uiv.model.common.party.Customer;
import com.nokia.nsw.uiv.model.resource.infra.physical.PhysicalComponent;
import com.nokia.nsw.uiv.model.resource.infra.physical.PhysicalDevice;
import com.nokia.nsw.uiv.model.resource.infra.physical.PhysicalLink;
import com.nokia.nsw.uiv.model.resource.infra.physical.PhysicalPort;
import com.nokia.nsw.uiv.model.resource.infra.virtual.VirtualComponent;
import com.nokia.nsw.uiv.model.resource.infra.virtual.VirtualDevice;
import com.nokia.nsw.uiv.model.resource.infra.virtual.VirtualLink;
import com.nokia.nsw.uiv.model.resource.infra.virtual.VirtualPort;
import com.nokia.nsw.uiv.model.resource.logical.Configuration;
import com.nokia.nsw.uiv.model.resource.logical.Connection;
import com.nokia.nsw.uiv.model.resource.logical.LogicalComponent;
import com.nokia.nsw.uiv.model.resource.logical.LogicalDevice;
import com.nokia.nsw.uiv.model.resource.logical.LogicalInterface;
import com.nokia.nsw.uiv.model.resource.logical.Pipe;
import com.nokia.nsw.uiv.model.resource.logical.Protocol;
import com.nokia.nsw.uiv.model.resource.logical.Trail;
import com.nokia.nsw.uiv.model.service.Service;

public class NFMPL2VPNModelClassMapper implements ModelClassMapper<Class<? extends Neo4jDomainNodeObject>> {

	private final Map<String, Class<? extends Neo4jDomainNodeObject>> MODEL_CLASS_MAP = new LinkedHashMap<>();

	public NFMPL2VPNModelClassMapper() throws ClassNotFoundException {
		MODEL_CLASS_MAP.put(Customer.class.getSimpleName(), Customer.class);
		MODEL_CLASS_MAP.put(Service.class.getSimpleName(), Service.class);
		MODEL_CLASS_MAP.put(Connection.class.getSimpleName(), Connection.class);
		MODEL_CLASS_MAP.put(Trail.class.getSimpleName(), Trail.class);
		MODEL_CLASS_MAP.put(Pipe.class.getSimpleName(), Pipe.class);
		MODEL_CLASS_MAP.put(LogicalInterface.class.getSimpleName(), LogicalInterface.class);
		MODEL_CLASS_MAP.put(LogicalComponent.class.getSimpleName(), LogicalComponent.class);
		MODEL_CLASS_MAP.put(LogicalDevice.class.getSimpleName(), LogicalDevice.class);
		MODEL_CLASS_MAP.put(Configuration.class.getSimpleName(), Configuration.class);
		MODEL_CLASS_MAP.put(Protocol.class.getSimpleName(), Protocol.class);
		MODEL_CLASS_MAP.put(VirtualLink.class.getSimpleName(), VirtualLink.class);
		MODEL_CLASS_MAP.put(VirtualPort.class.getSimpleName(), VirtualPort.class);
		MODEL_CLASS_MAP.put(VirtualComponent.class.getSimpleName(), VirtualComponent.class);
		MODEL_CLASS_MAP.put(VirtualDevice.class.getSimpleName(), VirtualDevice.class);
		MODEL_CLASS_MAP.put(PhysicalLink.class.getSimpleName(), PhysicalLink.class);
		MODEL_CLASS_MAP.put(PhysicalPort.class.getSimpleName(), PhysicalPort.class);
		MODEL_CLASS_MAP.put(PhysicalComponent.class.getSimpleName(), PhysicalComponent.class);
		MODEL_CLASS_MAP.put(PhysicalDevice.class.getSimpleName(), PhysicalDevice.class);
	}

	@Override
	public Class<? extends Neo4jDomainNodeObject> getClassByEntityType(String entityType) {
		return MODEL_CLASS_MAP.get(entityType);
	}

	@Override
	public Collection<Class<? extends Neo4jDomainNodeObject>> getAssociationOrder() {
		return MODEL_CLASS_MAP.values();
	}

	@Override
	public Collection<Class<? extends Neo4jDomainNodeObject>> getOutputOrder() {
		return MODEL_CLASS_MAP.values();
	}
}