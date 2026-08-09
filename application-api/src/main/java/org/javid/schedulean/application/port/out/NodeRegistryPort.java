package org.javid.schedulean.application.port.out;

import org.javid.schedulean.domain.valueobject.NodeInstanceId;

import java.util.List;

public interface NodeRegistryPort {
    void register(NodeInstanceId nodeId);

    void heartbeat();

    void deregister();

    boolean isMaster();

    boolean tryAcquireMastership();

    List<NodeInstanceId> deadNodes();

    NodeInstanceId nodeId();
}
