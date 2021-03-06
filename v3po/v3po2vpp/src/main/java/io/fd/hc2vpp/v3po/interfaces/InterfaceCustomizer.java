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

package io.fd.hc2vpp.v3po.interfaces;

import io.fd.hc2vpp.common.translate.util.FutureJVppCustomizer;
import io.fd.hc2vpp.common.translate.util.JvppReplyConsumer;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.honeycomb.translate.spi.write.ListWriterCustomizer;
import io.fd.honeycomb.translate.write.WriteContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.jvpp.core.dto.SwInterfaceSetFlags;
import io.fd.jvpp.core.dto.SwInterfaceSetFlagsReply;
import io.fd.jvpp.core.future.FutureJVppCore;
import java.util.concurrent.CompletionStage;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Ietf interface write customizer that only caches interface objects for child writers
 */
public class InterfaceCustomizer extends FutureJVppCustomizer
        implements ListWriterCustomizer<Interface, InterfaceKey>, JvppReplyConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(InterfaceCustomizer.class);
    private static final String LOCAL0_NAME = "local0";

    private final NamingContext interfaceContext;

    public InterfaceCustomizer(final FutureJVppCore vppApi, final NamingContext interfaceContext) {
        super(vppApi);
        this.interfaceContext = interfaceContext;
    }

    @Override
    public void writeCurrentAttributes(@Nonnull final InstanceIdentifier<Interface> id,
                                       @Nonnull final Interface dataAfter,
                                       @Nonnull final WriteContext writeContext)
            throws WriteFailedException {

        setInterface(id, dataAfter, writeContext);
    }

    @Override
    public void updateCurrentAttributes(@Nonnull final InstanceIdentifier<Interface> id,
                                        @Nonnull final Interface dataBefore,
                                        @Nonnull final Interface dataAfter,
                                        @Nonnull final WriteContext writeContext)
            throws WriteFailedException {
        updateInterface(id, dataAfter, writeContext);
    }

    @Override
    public void deleteCurrentAttributes(@Nonnull final InstanceIdentifier<Interface> id,
                                        @Nonnull final Interface dataBefore,
                                        @Nonnull final WriteContext writeContext)
        throws WriteFailedException.DeleteFailedException {
        // Special handling for local0 interface (HC2VPP-308):
        if (LOCAL0_NAME.equals(dataBefore.getName())) {
            throw new WriteFailedException.DeleteFailedException(id,
                new UnsupportedOperationException("Removing " + LOCAL0_NAME + " interface is not supported"));
        }
        // For other interfaces, delegate delete  to customizers for specific interface types (e.g. VXLan, Tap).
    }

    private void setInterface(final InstanceIdentifier<Interface> id, final Interface swIf,
                              final WriteContext writeContext) throws WriteFailedException {
        LOG.debug("Setting interface: {} to: {}", id, swIf);
        setInterfaceAttributes(id, swIf, swIf.getName(), writeContext);
    }

    private void setInterfaceAttributes(final InstanceIdentifier<Interface> id, final Interface swIf,
                                        final String swIfName, final WriteContext writeContext)
            throws WriteFailedException {

        setInterfaceFlags(id, swIfName, interfaceContext.getIndex(swIfName, writeContext.getMappingContext()),
                swIf.isEnabled()
                        ? (byte) 1
                        : (byte) 0);
    }

    private void updateInterface(final InstanceIdentifier<Interface> id,
                                 final Interface dataAfter, final WriteContext writeContext)
            throws WriteFailedException {
        LOG.debug("Updating interface:{} to: {}", id, dataAfter);
        setInterfaceAttributes(id, dataAfter, dataAfter.getName(), writeContext);
    }

    private void setInterfaceFlags(final InstanceIdentifier<Interface> id, final String swIfName, final int swIfIndex,
                                   final byte enabled) throws WriteFailedException {
        final CompletionStage<SwInterfaceSetFlagsReply> swInterfaceSetFlagsReplyFuture =
                getFutureJVpp().swInterfaceSetFlags(getSwInterfaceSetFlagsInput(swIfIndex, enabled));

        LOG.debug("Updating interface flags for: {}, index: {}, enabled: {}", swIfName, swIfIndex, enabled);

        getReplyForWrite(swInterfaceSetFlagsReplyFuture.toCompletableFuture(), id);
        LOG.debug("Interface flags updated successfully for: {}, index: {}, enabled: {}",
                swIfName, swIfIndex, enabled);
    }

    private SwInterfaceSetFlags getSwInterfaceSetFlagsInput(final int swIfIndex, final byte enabled) {
        final SwInterfaceSetFlags swInterfaceSetFlags = new SwInterfaceSetFlags();
        swInterfaceSetFlags.swIfIndex = swIfIndex;
        swInterfaceSetFlags.adminUpDown = enabled;
        return swInterfaceSetFlags;
    }
}
