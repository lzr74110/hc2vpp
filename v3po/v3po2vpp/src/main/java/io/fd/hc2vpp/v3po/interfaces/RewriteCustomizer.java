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

import com.google.common.base.Preconditions;
import io.fd.hc2vpp.common.translate.util.ByteDataTranslator;
import io.fd.hc2vpp.common.translate.util.FutureJVppCustomizer;
import io.fd.hc2vpp.common.translate.util.JvppReplyConsumer;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.hc2vpp.common.translate.util.TagRewriteOperation;
import io.fd.hc2vpp.v3po.util.SubInterfaceUtils;
import io.fd.honeycomb.translate.spi.write.WriterCustomizer;
import io.fd.honeycomb.translate.write.WriteContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.jvpp.core.dto.L2InterfaceVlanTagRewrite;
import io.fd.jvpp.core.dto.L2InterfaceVlanTagRewriteReply;
import io.fd.jvpp.core.future.FutureJVppCore;
import java.util.List;
import java.util.concurrent.CompletionStage;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.vlan.rev180319._802dot1q;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.vlan.rev180319.interfaces._interface.sub.interfaces.SubInterface;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.vlan.rev180319.rewrite.attributes.Rewrite;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.vlan.rev180319.rewrite.attributes.RewriteBuilder;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.vlan.rev180319.tag.rewrite.PushTags;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Writer Customizer responsible for vlan tag rewrite.<br> Sends {@code l2_interface_vlan_tag_rewrite} message to
 * VPP.<br> Equivalent of invoking {@code vppctl set interface l2 tag-rewrite} command.
 */
public class RewriteCustomizer extends FutureJVppCustomizer
        implements WriterCustomizer<Rewrite>, ByteDataTranslator, JvppReplyConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(RewriteCustomizer.class);
    private final NamingContext interfaceContext;

    public RewriteCustomizer(@Nonnull final FutureJVppCore futureJVppCore,
                             @Nonnull final NamingContext interfaceContext) {
        super(futureJVppCore);
        this.interfaceContext = Preconditions.checkNotNull(interfaceContext, "interfaceContext should not be null");
    }

    @Override
    public void writeCurrentAttributes(final InstanceIdentifier<Rewrite> id, final Rewrite dataAfter,
                                       final WriteContext writeContext)
            throws WriteFailedException {
        final String subifName = getSubInterfaceName(id);
        setTagRewrite(id, subifName, dataAfter, writeContext);
    }

    private static String getSubInterfaceName(final InstanceIdentifier<Rewrite> id) {
        return SubInterfaceUtils.getSubInterfaceName(id.firstKeyOf(Interface.class).getName(),
                Math.toIntExact(id.firstKeyOf(SubInterface.class).getIdentifier()));
    }

    private void setTagRewrite(final InstanceIdentifier<Rewrite> id, final String ifname, final Rewrite rewrite,
                               final WriteContext writeContext) throws WriteFailedException {
        final int swIfIndex = interfaceContext.getIndex(ifname, writeContext.getMappingContext());
        LOG.debug("Setting tag rewrite for interface {}(id=): {}", ifname, swIfIndex, rewrite);

        final CompletionStage<L2InterfaceVlanTagRewriteReply> replyCompletionStage =
                getFutureJVpp().l2InterfaceVlanTagRewrite(getTagRewriteRequest(swIfIndex, rewrite));

        getReplyForWrite(replyCompletionStage.toCompletableFuture(), id);
        LOG.debug("Tag rewrite for interface {}(id=) set successfully: {}", ifname, swIfIndex, rewrite);
    }

    private L2InterfaceVlanTagRewrite getTagRewriteRequest(final int swIfIndex, final Rewrite rewrite) {
        final L2InterfaceVlanTagRewrite request = new L2InterfaceVlanTagRewrite();
        request.swIfIndex = swIfIndex;
        request.pushDot1Q = booleanToByte(_802dot1q.class == rewrite.getVlanType());

        final List<PushTags> pushTags = rewrite.getPushTags();
        final Short popTags = rewrite.getPopTags();

        final int numberOfTagsToPop = popTags == null
                ? 0
                : popTags.intValue();
        final int numberOfTagsToPush = pushTags == null
                ? 0
                : pushTags.size();

        request.vtrOp = TagRewriteOperation.get(numberOfTagsToPop, numberOfTagsToPush).ordinal();

        if (numberOfTagsToPush > 0) {
            for (final PushTags tag : pushTags) {
                if (tag.getIndex() == 0) {
                    request.tag1 = tag.getDot1qTag().getVlanId().getValue();
                } else {
                    request.tag2 = tag.getDot1qTag().getVlanId().getValue();
                }
            }
        }

        LOG.debug("Generated tag rewrite request: {}", request);
        return request;
    }

    @Override
    public void updateCurrentAttributes(@Nonnull final InstanceIdentifier<Rewrite> id,
                                        @Nonnull final Rewrite dataBefore,
                                        @Nonnull final Rewrite dataAfter, @Nonnull final WriteContext writeContext)
            throws WriteFailedException {
        final String subifName = getSubInterfaceName(id);
        setTagRewrite(id, subifName, dataAfter, writeContext);
    }

    @Override
    public void deleteCurrentAttributes(@Nonnull final InstanceIdentifier<Rewrite> id,
                                        @Nonnull final Rewrite dataBefore, @Nonnull final WriteContext writeContext)
            throws WriteFailedException {
        final String subifName = getSubInterfaceName(id);

        LOG.debug("Disabling tag rewrite for interface {}", subifName);
        final Rewrite rewrite = new RewriteBuilder().build(); // rewrite without push and pops will cause delete
        setTagRewrite(id, subifName, rewrite, writeContext);
    }
}
