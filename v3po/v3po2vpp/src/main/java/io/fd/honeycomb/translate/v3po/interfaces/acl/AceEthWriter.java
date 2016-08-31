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

package io.fd.honeycomb.translate.v3po.interfaces.acl;

import com.google.common.annotations.VisibleForTesting;
import io.fd.honeycomb.translate.v3po.util.TranslateUtils;
import java.util.List;
import javax.annotation.Nonnull;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.acl.access.list.entries.ace.actions.PacketHandling;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.acl.access.list.entries.ace.matches.ace.type.AceEth;
import org.openvpp.jvpp.core.dto.ClassifyAddDelSession;
import org.openvpp.jvpp.core.dto.ClassifyAddDelTable;
import org.openvpp.jvpp.core.dto.InputAclSetInterface;
import org.openvpp.jvpp.core.future.FutureJVppCore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class AceEthWriter extends AbstractAceWriter<AceEth> {

    @VisibleForTesting
    static final int MATCH_N_VECTORS = 1;
    private static final Logger LOG = LoggerFactory.getLogger(AceEthWriter.class);

    public AceEthWriter(@Nonnull final FutureJVppCore futureJVppCore) {
        super(futureJVppCore);
    }

    @Override
    public ClassifyAddDelTable createClassifyTable(@Nonnull final PacketHandling action,
                                                   @Nonnull final AceEth aceEth,
                                                   @Nonnull final int nextTableIndex) {
        final ClassifyAddDelTable request = createClassifyTable(action, nextTableIndex);

        request.mask = new byte[16];
        boolean aceIsEmpty = true;

        // destination-mac-address or destination-mac-address-mask is present =>
        // ff:ff:ff:ff:ff:ff:00:00:00:00:00:00:00:00:00:00
        if (aceEth.getDestinationMacAddressMask() != null) {
            aceIsEmpty = false;
            final String macAddress = aceEth.getDestinationMacAddressMask().getValue();
            final List<String> parts = TranslateUtils.COLON_SPLITTER.splitToList(macAddress);
            int i = 0;
            for (String part : parts) {
                request.mask[i++] = TranslateUtils.parseHexByte(part);
            }
        } else if (aceEth.getDestinationMacAddress() != null) {
            aceIsEmpty = false;
            for (int i = 0; i < 6; ++i) {
                request.mask[i] = (byte) 0xff;
            }
        }

        // source-mac-address or source-mac-address-mask =>
        // 00:00:00:00:00:00:ff:ff:ff:ff:ff:ff:00:00:00:00
        if (aceEth.getSourceMacAddressMask() != null) {
            aceIsEmpty = false;
            final String macAddress = aceEth.getSourceMacAddressMask().getValue();
            final List<String> parts = TranslateUtils.COLON_SPLITTER.splitToList(macAddress);
            int i = 6;
            for (String part : parts) {
                request.mask[i++] = TranslateUtils.parseHexByte(part);
            }
        } else if (aceEth.getSourceMacAddress() != null) {
            aceIsEmpty = false;
            for (int i = 6; i < 12; ++i) {
                request.mask[i] = (byte) 0xff;
            }
        }

        if (aceIsEmpty) {
            throw new IllegalArgumentException(
                String.format("Ace %s does not define packet field match values", aceEth.toString()));
        }

        request.skipNVectors = 0;
        request.matchNVectors = MATCH_N_VECTORS;

        if (LOG.isDebugEnabled()) {
            LOG.debug("ACE action={}, rule={} translated to table={}.", action, aceEth,
                ReflectionToStringBuilder.toString(request));
        }
        return request;
    }

    @Override
    public ClassifyAddDelSession createClassifySession(@Nonnull final PacketHandling action,
                                                       @Nonnull final AceEth aceEth,
                                                       @Nonnull final int tableIndex) {
        final ClassifyAddDelSession request = createClassifySession(action, tableIndex);

        request.match = new byte[16];
        boolean noMatch = true;

        if (aceEth.getDestinationMacAddress() != null) {
            noMatch = false;
            final String macAddress = aceEth.getDestinationMacAddress().getValue();
            final List<String> parts = TranslateUtils.COLON_SPLITTER.splitToList(macAddress);
            int i = 0;
            for (String part : parts) {
                request.match[i++] = TranslateUtils.parseHexByte(part);
            }
        }

        if (aceEth.getSourceMacAddress() != null) {
            noMatch = false;
            final String macAddress = aceEth.getSourceMacAddress().getValue();
            final List<String> parts = TranslateUtils.COLON_SPLITTER.splitToList(macAddress);
            int i = 6;
            for (String part : parts) {
                request.match[i++] = TranslateUtils.parseHexByte(part);
            }
        }

        if (noMatch) {
            throw new IllegalArgumentException(
                String.format("Ace %s does not define neither source nor destination MAC address", aceEth.toString()));
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("ACE action={}, rule={} translated to session={}.", action, aceEth,
                ReflectionToStringBuilder.toString(request));
        }
        return request;
    }

    @Override
    protected void setClassifyTable(@Nonnull final InputAclSetInterface request, final int tableIndex) {
        request.l2TableIndex = tableIndex;
    }
}