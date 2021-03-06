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

package io.fd.hc2vpp.v3po.l2state;

import io.fd.hc2vpp.common.test.read.ListReaderCustomizerTest;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.honeycomb.translate.spi.read.ReaderCustomizer;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190128.BridgeDomainsStateBuilder;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190128.bridge.domains.state.BridgeDomain;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190128.bridge.domains.state.BridgeDomainBuilder;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190128.bridge.domains.state.BridgeDomainKey;


public class BridgeDomainCustomizerTest
    extends ListReaderCustomizerTest<BridgeDomain, BridgeDomainKey, BridgeDomainBuilder> {

    private NamingContext bdContext;
    private NamingContext interfacesContext;

    public BridgeDomainCustomizerTest() {
        super(BridgeDomain.class, BridgeDomainsStateBuilder.class);
    }

    @Override
    public void setUp() {
        bdContext = new NamingContext("generatedBdName", "bd-test-instance");
        interfacesContext = new NamingContext("generatedIfaceName", "ifc-test-instance");
    }

    @Override
    protected ReaderCustomizer<BridgeDomain, BridgeDomainBuilder> initCustomizer() {
        return new BridgeDomainCustomizer(api, bdContext);
    }
}
