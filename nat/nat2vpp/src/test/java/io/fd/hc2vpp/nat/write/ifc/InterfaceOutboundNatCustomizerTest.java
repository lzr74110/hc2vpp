/*
 * Copyright (c) 2017 Cisco and/or its affiliates.
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

package io.fd.hc2vpp.nat.write.ifc;

import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.jvpp.nat.future.FutureJVppNatFacade;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang._interface.nat.rev170816.NatInterfaceAugmentation;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang._interface.nat.rev170816._interface.nat.attributes.Nat;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang._interface.nat.rev170816._interface.nat.attributes.nat.Outbound;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang._interface.nat.rev170816._interface.nat.attributes.nat.OutboundBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.Interfaces;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class InterfaceOutboundNatCustomizerTest
        extends AbstractNatCustomizerTest<Outbound, InterfaceOutboundNatCustomizer> {

    @Override
    protected Outbound getPreRoutingConfig() {
        return new OutboundBuilder().setPostRouting(false).setNat44Support(true).setNat64Support(true).build();
    }

    @Override
    protected Outbound getPostRoutingConfig() {
        return new OutboundBuilder().setPostRouting(true).setNat44Support(true).setNat64Support(false).build();
    }

    @Override
    protected InstanceIdentifier<Outbound> getIId(final String ifaceName) {
        return InstanceIdentifier.create(Interfaces.class)
                .child(Interface.class, new InterfaceKey(ifaceName)).augmentation(NatInterfaceAugmentation.class)
                .child(Nat.class).child(Outbound.class);
    }

    @Override
    protected InterfaceOutboundNatCustomizer getCustomizer(final FutureJVppNatFacade natApi,
                                                           final NamingContext ifcNamingCtx) {
        return new InterfaceOutboundNatCustomizer(natApi, ifcNamingCtx);
    }
}
