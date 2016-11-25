/*
 * Copyright (c) 2016 Cisco and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.fd.hc2vpp.v3po;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.hc2vpp.common.translate.util.ReadTimeoutException;
import io.fd.hc2vpp.common.translate.util.VppStatusListener;
import io.fd.hc2vpp.v3po.cfgattrs.V3poConfiguration;
import io.fd.hc2vpp.v3po.vppstate.BridgeDomainCustomizer;
import io.fd.hc2vpp.v3po.vppstate.L2FibEntryCustomizer;
import io.fd.hc2vpp.v3po.vppstate.VersionCustomizer;
import io.fd.honeycomb.translate.impl.read.GenericInitListReader;
import io.fd.honeycomb.translate.impl.read.GenericReader;
import io.fd.honeycomb.translate.read.ReaderFactory;
import io.fd.honeycomb.translate.read.registry.ModifiableReaderRegistryBuilder;
import io.fd.honeycomb.translate.util.read.KeepaliveReaderWrapper;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import java.util.concurrent.ScheduledExecutorService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev161214.VppState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev161214.VppStateBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev161214.l2.fib.attributes.L2FibTable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev161214.l2.fib.attributes.L2FibTableBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev161214.l2.fib.attributes.l2.fib.table.L2FibEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev161214.vpp.state.BridgeDomains;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev161214.vpp.state.BridgeDomainsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev161214.vpp.state.Version;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev161214.vpp.state.bridge.domains.BridgeDomain;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public final class VppStateHoneycombReaderFactory implements ReaderFactory {

    private final FutureJVppCore jVpp;
    private final NamingContext ifcCtx;
    private final NamingContext bdCtx;
    private final ScheduledExecutorService keepaliveExecutor;
    private final VppStatusListener vppStatusListener;

    @Inject
    private V3poConfiguration v3poConfiguration;

    @Inject
    public VppStateHoneycombReaderFactory(final FutureJVppCore jVpp,
                                          @Named("interface-context") final NamingContext ifcCtx,
                                          @Named("bridge-domain-context") final NamingContext bdCtx,
                                          final ScheduledExecutorService keepaliveExecutorDependency,
                                          final VppStatusListener vppStatusListener) {
        this.jVpp = jVpp;
        this.ifcCtx = ifcCtx;
        this.bdCtx = bdCtx;
        this.keepaliveExecutor = keepaliveExecutorDependency;
        this.vppStatusListener = vppStatusListener;
    }

    @Override
    public void init(final ModifiableReaderRegistryBuilder registry) {
        // VppState(Structural)
        final InstanceIdentifier<VppState> vppStateId = InstanceIdentifier.create(VppState.class);
        registry.addStructuralReader(vppStateId, VppStateBuilder.class);
        //  Version
        // Wrap with keepalive reader to detect connection issues
        // Relying on VersionCustomizer to provide a "timing out read"
        registry.add(new KeepaliveReaderWrapper<>(
                new GenericReader<>(vppStateId.child(Version.class), new VersionCustomizer(jVpp)),
                keepaliveExecutor, ReadTimeoutException.class, v3poConfiguration.getKeepaliveDelay(), vppStatusListener));
        //  BridgeDomains(Structural)
        final InstanceIdentifier<BridgeDomains> bridgeDomainsId = vppStateId.child(BridgeDomains.class);
        registry.addStructuralReader(bridgeDomainsId, BridgeDomainsBuilder.class);
        //   BridgeDomain
        final InstanceIdentifier<BridgeDomain> bridgeDomainId = bridgeDomainsId.child(BridgeDomain.class);
        registry.add(new GenericInitListReader<>(bridgeDomainId, new BridgeDomainCustomizer(jVpp, bdCtx)));
        //    L2FibTable(Structural)
        final InstanceIdentifier<L2FibTable> l2FibTableId = bridgeDomainId.child(L2FibTable.class);
        registry.addStructuralReader(l2FibTableId, L2FibTableBuilder.class);
        //     L2FibEntry
        registry.add(new GenericInitListReader<>(l2FibTableId.child(L2FibEntry.class),
                new L2FibEntryCustomizer(jVpp, bdCtx, ifcCtx)));
    }
}
