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

package io.fd.hc2vpp.vpp.classifier.write;

import static com.google.common.base.Preconditions.checkArgument;

import io.fd.hc2vpp.common.translate.util.FutureJVppCustomizer;
import io.fd.hc2vpp.common.translate.util.JvppReplyConsumer;
import io.fd.hc2vpp.vpp.classifier.context.VppClassifierContextManager;
import io.fd.honeycomb.translate.MappingContext;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.jvpp.VppBaseCallException;
import io.fd.jvpp.core.dto.GetNextIndex;
import io.fd.jvpp.core.dto.GetNextIndexReply;
import io.fd.jvpp.core.future.FutureJVppCore;
import java.util.concurrent.CompletionStage;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.classifier.rev170327.VppNode;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.classifier.rev170327.vpp.classifier.ClassifyTable;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

abstract class VppNodeWriter extends FutureJVppCustomizer implements JvppReplyConsumer {

    protected VppNodeWriter(@Nonnull final FutureJVppCore futureJvpp) {
        super(futureJvpp);
    }

    protected int getNodeIndex(@Nonnull final VppNode node, @Nonnull final ClassifyTable classifyTable,
                               @Nonnull final VppClassifierContextManager vppClassifierContextManager,
                               @Nonnull final MappingContext ctx, @Nonnull final InstanceIdentifier<?> id)
            throws VppBaseCallException, WriteFailedException {
        if (node.getPacketHandlingAction() != null) {
            return node.getPacketHandlingAction().getIntValue();
        } else {
            return nodeNameToIndex(classifyTable, node.getVppNodeName().getValue(), vppClassifierContextManager, ctx,
                    id);
        }
    }

    private int nodeNameToIndex(@Nonnull final ClassifyTable classifyTable, @Nonnull final String nextNodeName,
                                @Nonnull final VppClassifierContextManager vppClassifierContextManager,
                                @Nonnull final MappingContext ctx, @Nonnull final InstanceIdentifier<?> id)
            throws WriteFailedException {
        checkArgument(classifyTable != null && classifyTable.getClassifierNode() != null,
                "to use relative node names, table classifier node needs to be provided");
        final GetNextIndex request = new GetNextIndex();
        request.nodeName = classifyTable.getClassifierNode().getValue().getBytes();
        request.nextName = nextNodeName.getBytes();
        final CompletionStage<GetNextIndexReply> getNextIndexCompletionStage =
                getFutureJVpp().getNextIndex(request);

        final GetNextIndexReply reply;
        try {
            reply = getReplyForRead(getNextIndexCompletionStage.toCompletableFuture(), id);

            // vpp does not provide relative node index to node name conversion (https://jira.fd.io/browse/VPP-219)
            // as a workaround we need to add mapping to vpp-classfier-context
            vppClassifierContextManager.addNodeName(classifyTable.getName(), reply.nextIndex, nextNodeName, ctx);
        } catch (ReadFailedException e) {
            throw new WriteFailedException(id, String.format("Failed to get node index for %s relative to %s",
                    nextNodeName, classifyTable.getClassifierNode()), e);
        }
        return reply.nextIndex;
    }
}
