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

package io.fd.hc2vpp.l3.write.ipv6;

import io.fd.hc2vpp.common.translate.util.FutureJVppCustomizer;
import io.fd.honeycomb.translate.spi.write.WriterCustomizer;
import io.fd.honeycomb.translate.write.WriteContext;
import io.fd.jvpp.core.future.FutureJVppCore;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces._interface.Ipv6;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Ipv6Customizer extends FutureJVppCustomizer implements WriterCustomizer<Ipv6> {

    private static final Logger LOG = LoggerFactory.getLogger(Ipv6Customizer.class);

    public Ipv6Customizer(final FutureJVppCore vppApi) {
        super(vppApi);
    }

    @Override
    public void writeCurrentAttributes(@Nonnull final InstanceIdentifier<Ipv6> id,
                                       @Nonnull final Ipv6 dataAfter, @Nonnull final WriteContext writeContext) {
        LOG.warn("Unsupported, ignoring configuration {}", dataAfter);
    }

    @Override
    public void deleteCurrentAttributes(@Nonnull final InstanceIdentifier<Ipv6> id,
                                        @Nonnull final Ipv6 dataBefore, @Nonnull final WriteContext writeContext) {
        LOG.warn("Unsupported, ignoring configuration delete {}", id);
    }
}
