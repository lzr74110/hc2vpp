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

package io.fd.honeycomb.v3po.translate.v3po.interfacesstate;

import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import java.math.BigInteger;
import java.util.concurrent.ExecutionException;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Gauge64;
import org.openvpp.jvpp.dto.SwInterfaceDetails;
import org.openvpp.jvpp.dto.SwInterfaceDetailsReplyDump;
import org.openvpp.jvpp.dto.SwInterfaceDump;
import org.openvpp.jvpp.future.FutureJVpp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InterfaceUtils {
    private static final Logger LOG = LoggerFactory.getLogger(InterfaceUtils.class);

    private static final Gauge64 vppLinkSpeed0 = new Gauge64(BigInteger.ZERO);
    private static final Gauge64 vppLinkSpeed1 = new Gauge64(BigInteger.valueOf(10 * 1000000));
    private static final Gauge64 vppLinkSpeed2 = new Gauge64(BigInteger.valueOf(100 * 1000000));
    private static final Gauge64 vppLinkSpeed4 = new Gauge64(BigInteger.valueOf(1000 * 1000000));
    private static final Gauge64 vppLinkSpeed8 = new Gauge64(BigInteger.valueOf(10000L * 1000000));
    private static final Gauge64 vppLinkSpeed16 = new Gauge64(BigInteger.valueOf(40000L * 1000000));
    private static final Gauge64 vppLinkSpeed32 = new Gauge64(BigInteger.valueOf(100000L * 1000000));

    private static final char[] HEX_CHARS = "0123456789abcdef".toCharArray();

    /**
     * Convert VPP's link speed bitmask to Yang type. 1 = 10M, 2 = 100M, 4 = 1G, 8 = 10G, 16 = 40G, 32 = 100G
     *
     * @param vppLinkSpeed Link speed in bitmask format from VPP.
     * @return Converted value from VPP link speed
     */
    public static Gauge64 vppInterfaceSpeedToYang(byte vppLinkSpeed) {
        switch (vppLinkSpeed) {
            case 1:
                return vppLinkSpeed1;
            case 2:
                return vppLinkSpeed2;
            case 4:
                return vppLinkSpeed4;
            case 8:
                return vppLinkSpeed8;
            case 16:
                return vppLinkSpeed16;
            case 32:
                return vppLinkSpeed32;
            default:
                return vppLinkSpeed0;
        }
    }

    private static final void appendHexByte(final StringBuilder sb, final byte b) {
        final int v = b & 0xFF;
        sb.append(HEX_CHARS[v >>> 4]);
        sb.append(HEX_CHARS[v & 15]);
    }

    /**
     * Convert VPP's physical address stored byte array format to string as Yang dictates <p> Replace later with
     * https://git.opendaylight.org/gerrit/#/c/34869/10/model/ietf/ietf-type- util/src/main/
     * java/org/opendaylight/mdsal/model/ietf/util/AbstractIetfYangUtil.java
     *
     * @param vppPhysAddress byte array of bytes constructing the network IF physical address.
     * @return String like "aa:bb:cc:dd:ee:ff"
     */
    public static String vppPhysAddrToYang(@Nonnull final byte[] vppPhysAddress) {
        Preconditions.checkNotNull(vppPhysAddress, "Empty physicall address bytes");
        Preconditions.checkArgument(vppPhysAddress.length == 6, "Invalid physical address size %s, expected 6",
                vppPhysAddress.length);
        StringBuilder physAddr = new StringBuilder();

        appendHexByte(physAddr, vppPhysAddress[0]);
        for (int i = 1; i < vppPhysAddress.length; i++) {
            physAddr.append(":");
            appendHexByte(physAddr, vppPhysAddress[i]);
        }

        return physAddr.toString();
    }

    /**
     * VPP's interface index is counted from 0, whereas ietf-interface's if-index is from 1. This function converts from
     * VPP's interface index to YANG's interface index.
     *
     * @param vppIfIndex the sw interface index VPP reported.
     * @return VPP's interface index incremented by one
     */
    public static int vppIfIndexToYang(int vppIfIndex) {
        return vppIfIndex + 1;
    }

    /**
     * This function does the opposite of what {@link #vppIfIndexToYang(int)} does.
     *
     * @param yangIfIndex if-index from ietf-interfaces.
     * @return VPP's representation of the if-index
     */
    public static int YangIfIndexToVpp(int yangIfIndex) {
        Preconditions.checkArgument(yangIfIndex >= 1, "YANG if-index has invalid value %s", yangIfIndex);
        return yangIfIndex - 1;
    }


    /**
     * Queries VPP for interface description given interface key.
     *
     * @param futureJvpp VPP Java Future API
     * @param key        interface key
     * @return SwInterfaceDetails DTO or null if interface was not found
     * @throws ExecutionException   if exception has been thrown while executing VPP query
     * @throws InterruptedException if the current thread was interrupted
     */
    @Nullable
    public static SwInterfaceDetails getVppInterfaceDetails(@Nonnull final FutureJVpp futureJvpp,
                                                            @Nonnull InterfaceKey key)
            throws ExecutionException, InterruptedException {
        final SwInterfaceDump request = new SwInterfaceDump();
        request.nameFilter = key.getName().getBytes();
        request.nameFilterValid = 1;

        // TODO should we use timeout?
        SwInterfaceDetailsReplyDump ifaces = futureJvpp.swInterfaceDump(request).toCompletableFuture().get();
        if (null == ifaces) { // TODO can we get null here?
            LOG.warn("VPP returned null instead of interface by key {}", key.getName().getBytes());
            return null;
        }

        return Iterables.getOnlyElement(ifaces.swInterfaceDetails);
    }


}
