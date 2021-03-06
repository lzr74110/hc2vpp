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

package io.fd.hc2vpp.v3po.l2;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.stream.Collectors.toList;

import com.google.common.base.Preconditions;
import io.fd.hc2vpp.common.translate.util.ByteDataTranslator;
import io.fd.hc2vpp.common.translate.util.FutureJVppCustomizer;
import io.fd.hc2vpp.common.translate.util.JvppReplyConsumer;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.hc2vpp.common.translate.util.ReferenceCheck;
import io.fd.honeycomb.translate.spi.write.ListWriterCustomizer;
import io.fd.honeycomb.translate.write.WriteContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.jvpp.core.dto.BridgeDomainAddDel;
import io.fd.jvpp.core.dto.BridgeDomainAddDelReply;
import io.fd.jvpp.core.future.FutureJVppCore;
import java.util.Collections;
import java.util.Optional;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.GuardedBy;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190128.L2ConfigAttributes;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190128.VppInterfaceAugmentation;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190128.bridge.domains.BridgeDomain;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190128.bridge.domains.BridgeDomainKey;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190128.l2.config.attributes.interconnection.BridgeBased;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.Interfaces;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BridgeDomainCustomizer extends FutureJVppCustomizer
        implements ListWriterCustomizer<BridgeDomain, BridgeDomainKey>, ByteDataTranslator, JvppReplyConsumer,
        ReferenceCheck {

    private static final Logger LOG = LoggerFactory.getLogger(BridgeDomainCustomizer.class);

    private static final byte ADD_OR_UPDATE_BD = (byte) 1;
    private final NamingContext bdContext;
    @GuardedBy("this")
    private int bridgeDomainIndexCounter = 1;

    public BridgeDomainCustomizer(@Nonnull final FutureJVppCore futureJVppCore,
                                  @Nonnull final NamingContext bdContext) {
        super(futureJVppCore);
        this.bdContext = Preconditions.checkNotNull(bdContext, "bdContext should not be null");
    }

    private BridgeDomainAddDelReply addOrUpdateBridgeDomain(@Nonnull final InstanceIdentifier<BridgeDomain> id,
                                                            final int bdId, @Nonnull final BridgeDomain bd)
            throws WriteFailedException {
        final BridgeDomainAddDelReply reply;
        final BridgeDomainAddDel request = new BridgeDomainAddDel();
        request.bdId = bdId;
        request.flood = booleanToByte(bd.isFlood());
        request.forward = booleanToByte(bd.isForward());
        request.learn = booleanToByte(bd.isLearn());
        request.uuFlood = booleanToByte(bd.isUnknownUnicastFlood());
        request.arpTerm = booleanToByte(bd.isArpTermination());
        request.isAdd = ADD_OR_UPDATE_BD;

        reply = getReplyForWrite(getFutureJVpp().bridgeDomainAddDel(request).toCompletableFuture(), id);
        LOG.debug("Bridge domain {} (id={}) add/update successful", bd.getName(), bdId);
        return reply;
    }

    @Override
    public void writeCurrentAttributes(@Nonnull final InstanceIdentifier<BridgeDomain> id,
                                       @Nonnull final BridgeDomain dataBefore,
                                       @Nonnull final WriteContext ctx)
            throws WriteFailedException {
        LOG.debug("writeCurrentAttributes: id={}, current={}, ctx={}", id, dataBefore, ctx);
        final String bdName = dataBefore.getName();

        // Invoke 1. check index, 2. increase index 3. create ND 4. store mapping in a synchronized block to prevent
        // race conditions in case of concurrent invocation
        synchronized (this) {
            int index;
            if (bdContext.containsIndex(bdName, ctx.getMappingContext())) {
                index = bdContext.getIndex(bdName, ctx.getMappingContext());
            } else {
                // Critical section due to bridgeDomainIndexCounter read and write access
                // TODO HONEYCOMB-199 move this "get next available index" into naming context or an adapter
                // or a dedicated object

                // Use counter to assign bridge domain index, but still check naming context if it's not taken there
                while (bdContext.containsName(bridgeDomainIndexCounter, ctx.getMappingContext())) {
                    bridgeDomainIndexCounter++;
                }
                index = bridgeDomainIndexCounter;
            }
            addOrUpdateBridgeDomain(id, index, dataBefore);
            bdContext.addName(index, bdName, ctx.getMappingContext());
        }
    }

    @Override
    public void deleteCurrentAttributes(@Nonnull final InstanceIdentifier<BridgeDomain> id,
                                        @Nonnull final BridgeDomain dataBefore,
                                        @Nonnull final WriteContext ctx)
            throws WriteFailedException {
        LOG.debug("deleteCurrentAttributes: id={}, dataBefore={}, ctx={}", id, dataBefore, ctx);
        final String bdName = id.firstKeyOf(BridgeDomain.class).getName();

        final com.google.common.base.Optional<Interfaces> after =
                ctx.readAfter(InstanceIdentifier.create(Interfaces.class));

        if (after.isPresent()) {
            checkReferenceExist(id, Optional.ofNullable(after.get().getInterface())
                    .orElse(Collections.emptyList())
                    .stream()
                    .map(iface -> Optional.ofNullable(iface.augmentation(VppInterfaceAugmentation.class))
                            .map(VppInterfaceAugmentation::getL2)
                            .map(L2ConfigAttributes::getInterconnection)
                            .orElse(null))
                    .filter(interconnection -> interconnection instanceof BridgeBased)
                    .map(BridgeBased.class::cast)
                    .filter(bridgeBased -> bdName.equals(bridgeBased.getBridgeDomain()))
                    .collect(toList()));
        }

        int bdId = bdContext.getIndex(bdName, ctx.getMappingContext());

        final BridgeDomainAddDel request = new BridgeDomainAddDel();
        request.bdId = bdId;

        getReplyForWrite(getFutureJVpp().bridgeDomainAddDel(request).toCompletableFuture(), id);
        LOG.debug("Bridge domain {} (id={}) deleted successfully", bdName, bdId);
    }

    @Override
    public void updateCurrentAttributes(@Nonnull final InstanceIdentifier<BridgeDomain> id,
                                        @Nonnull final BridgeDomain dataBefore, @Nonnull final BridgeDomain dataAfter,
                                        @Nonnull final WriteContext ctx)
            throws WriteFailedException {
        LOG.debug("updateCurrentAttributes: id={}, dataBefore={}, dataAfter={}, ctx={}", id, dataBefore, dataAfter,
                ctx);

        final String bdName = checkNotNull(dataAfter.getName());
        checkArgument(bdName.equals(dataBefore.getName()),
                "BridgeDomain name changed. It should be deleted and then created.");

        addOrUpdateBridgeDomain(id, bdContext.getIndex(bdName, ctx.getMappingContext()), dataAfter);
    }

}
