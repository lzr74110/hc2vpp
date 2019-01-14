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

package io.fd.hc2vpp.v3po.interfaces;

import static io.fd.hc2vpp.v3po.util.SubInterfaceUtils.subInterfaceFullNameConfig;

import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.honeycomb.translate.write.WriteContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.unnumbered.interfaces.rev180103.unnumbered.config.attributes.Unnumbered;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public final class SubInterfaceUnnumberedCustomizer extends AbstractUnnumberedCustomizer {

    public SubInterfaceUnnumberedCustomizer(@Nonnull final FutureJVppCore futureJVppCore,
                                            @Nonnull final NamingContext interfaceContext) {
        super(futureJVppCore, interfaceContext);
    }

    @Override
    public void writeCurrentAttributes(@Nonnull final InstanceIdentifier<Unnumbered> id,
                                       @Nonnull final Unnumbered dataAfter,
                                       @Nonnull final WriteContext writeContext) throws WriteFailedException {
        setUnnumbered(id, subInterfaceFullNameConfig(id), dataAfter, writeContext);
    }

    @Override
    public void updateCurrentAttributes(@Nonnull final InstanceIdentifier<Unnumbered> id,
                                        @Nonnull final Unnumbered dataBefore, @Nonnull final Unnumbered dataAfter,
                                        @Nonnull final WriteContext writeContext)
        throws WriteFailedException {
        setUnnumbered(id, subInterfaceFullNameConfig(id), dataAfter, writeContext);
    }

    @Override
    public void deleteCurrentAttributes(@Nonnull final InstanceIdentifier<Unnumbered> id,
                                        @Nonnull final Unnumbered dataBefore,
                                        @Nonnull final WriteContext writeContext) throws WriteFailedException {
        disableUnnumbered(id, subInterfaceFullNameConfig(id), dataBefore, writeContext);
    }
}
