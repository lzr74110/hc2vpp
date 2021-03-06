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

package io.fd.hc2vpp.lisp.translate.read;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static io.fd.hc2vpp.lisp.translate.read.dump.executor.params.MappingsDumpParams.EidType.valueOf;
import static io.fd.hc2vpp.lisp.translate.read.dump.executor.params.MappingsDumpParams.FilterType;
import static io.fd.hc2vpp.lisp.translate.read.dump.executor.params.MappingsDumpParams.MappingsDumpParamsBuilder;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import io.fd.hc2vpp.common.translate.util.AddressTranslator;
import io.fd.hc2vpp.common.translate.util.ByteDataTranslator;
import io.fd.hc2vpp.common.translate.util.FutureJVppCustomizer;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.hc2vpp.lisp.context.util.EidMappingContext;
import io.fd.hc2vpp.lisp.translate.read.dump.executor.params.LocatorDumpParams;
import io.fd.hc2vpp.lisp.translate.read.dump.executor.params.LocatorDumpParams.LocatorDumpParamsBuilder;
import io.fd.hc2vpp.lisp.translate.read.dump.executor.params.MappingsDumpParams;
import io.fd.hc2vpp.lisp.translate.read.dump.executor.params.MappingsDumpParams.QuantityType;
import io.fd.hc2vpp.lisp.translate.read.init.LispInitPathsMapper;
import io.fd.hc2vpp.lisp.translate.read.trait.LocatorReader;
import io.fd.hc2vpp.lisp.translate.read.trait.MappingReader;
import io.fd.hc2vpp.lisp.translate.util.EidTranslator;
import io.fd.honeycomb.translate.MappingContext;
import io.fd.honeycomb.translate.ModificationCache;
import io.fd.honeycomb.translate.read.ReadContext;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.spi.read.Initialized;
import io.fd.honeycomb.translate.spi.read.InitializingListReaderCustomizer;
import io.fd.honeycomb.translate.util.RWUtils;
import io.fd.honeycomb.translate.util.read.cache.DumpCacheManager;
import io.fd.honeycomb.translate.util.read.cache.TypeAwareIdentifierCacheKeyFactory;
import io.fd.jvpp.core.dto.OneEidTableDetails;
import io.fd.jvpp.core.dto.OneEidTableDetailsReplyDump;
import io.fd.jvpp.core.dto.OneLocatorDetails;
import io.fd.jvpp.core.dto.OneLocatorDetailsReplyDump;
import io.fd.jvpp.core.future.FutureJVppCore;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.lisp.rev171013.MapReplyAction;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.lisp.rev171013.MappingId;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.lisp.rev171013.dp.subtable.grouping.RemoteMappingsBuilder;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.lisp.rev171013.dp.subtable.grouping.remote.mappings.RemoteMapping;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.lisp.rev171013.dp.subtable.grouping.remote.mappings.RemoteMappingBuilder;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.lisp.rev171013.dp.subtable.grouping.remote.mappings.RemoteMappingKey;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.lisp.rev171013.dp.subtable.grouping.remote.mappings.remote.mapping.Eid;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.lisp.rev171013.dp.subtable.grouping.remote.mappings.remote.mapping.EidBuilder;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.lisp.rev171013.dp.subtable.grouping.remote.mappings.remote.mapping.locator.list.NegativeMappingBuilder;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.lisp.rev171013.dp.subtable.grouping.remote.mappings.remote.mapping.locator.list.PositiveMappingBuilder;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.lisp.rev171013.dp.subtable.grouping.remote.mappings.remote.mapping.locator.list.negative.mapping.MapReplyBuilder;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.lisp.rev171013.dp.subtable.grouping.remote.mappings.remote.mapping.locator.list.positive.mapping.RlocsBuilder;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.lisp.rev171013.dp.subtable.grouping.remote.mappings.remote.mapping.locator.list.positive.mapping.rlocs.Locator;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.lisp.rev171013.dp.subtable.grouping.remote.mappings.remote.mapping.locator.list.positive.mapping.rlocs.LocatorBuilder;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.lisp.rev171013.dp.subtable.grouping.remote.mappings.remote.mapping.locator.list.positive.mapping.rlocs.LocatorKey;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.lisp.rev171013.eid.table.grouping.eid.table.VniTable;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.lisp.rev171013.locator.sets.grouping.LocatorSets;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.lisp.rev171013.locator.sets.grouping.locator.sets.LocatorSet;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.lisp.rev171013.locator.sets.grouping.locator.sets.LocatorSetKey;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.lisp.rev171013.locator.sets.grouping.locator.sets.locator.set.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Customizer for reading {@code RemoteMapping}.
 */
public class RemoteMappingCustomizer extends FutureJVppCustomizer
        implements InitializingListReaderCustomizer<RemoteMapping, RemoteMappingKey, RemoteMappingBuilder>,
        EidTranslator, AddressTranslator, ByteDataTranslator, MappingReader, LocatorReader, LispInitPathsMapper {

    private static final Logger LOG = LoggerFactory.getLogger(RemoteMappingCustomizer.class);

    private final DumpCacheManager<OneEidTableDetailsReplyDump, MappingsDumpParams> dumpManager;
    private final DumpCacheManager<OneLocatorDetailsReplyDump, LocatorDumpParams> locatorsDumpManager;
    private final NamingContext locatorSetContext;
    private final EidMappingContext remoteMappingContext;

    public RemoteMappingCustomizer(@Nonnull final FutureJVppCore futureJvpp,
                                   @Nonnull final NamingContext locatorSetContext,
                                   @Nonnull final EidMappingContext remoteMappingContext) {
        super(futureJvpp);
        this.locatorSetContext = checkNotNull(locatorSetContext, "Locator sets context not present");
        this.remoteMappingContext = checkNotNull(remoteMappingContext, "Remote mappings not present");
        // this one should have default scope == RemoteMapping
        this.dumpManager =
                new DumpCacheManager.DumpCacheManagerBuilder<OneEidTableDetailsReplyDump, MappingsDumpParams>()
                        .withExecutor(createMappingDumpExecutor(futureJvpp))
                        .acceptOnly(OneEidTableDetailsReplyDump.class)
                        .build();

        // cache key needs to have locator set scope to not mix with cached data
        this.locatorsDumpManager =
                new DumpCacheManager.DumpCacheManagerBuilder<OneLocatorDetailsReplyDump, LocatorDumpParams>()
                        .withExecutor(createLocatorDumpExecutor(futureJvpp))
                        .withCacheKeyFactory(new TypeAwareIdentifierCacheKeyFactory(OneLocatorDetailsReplyDump.class,
                                ImmutableSet.of(LocatorSet.class)))
                        .build();
    }

    //compensate ~0 as default value of ttl
    private static long resolveTtl(final int ttlValue) {
        return ttlValue == -1
                ? Integer.MAX_VALUE
                : ttlValue;
    }

    @Override
    public RemoteMappingBuilder getBuilder(InstanceIdentifier<RemoteMapping> id) {
        return new RemoteMappingBuilder();
    }

    private Eid copyEid(
            org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.eid.mapping.context.rev160801.contexts.eid.mapping.context.mappings.mapping.Eid eid) {
        return new EidBuilder().setAddress(eid.getAddress()).setAddressType(eid.getAddressType())
                .setVirtualNetworkId(eid.getVirtualNetworkId()).build();
    }

    @Override
    public void readCurrentAttributes(InstanceIdentifier<RemoteMapping> id, RemoteMappingBuilder builder,
                                      ReadContext ctx)
            throws ReadFailedException {
        checkState(id.firstKeyOf(RemoteMapping.class) != null, "No key present for id({})", id);
        checkState(id.firstKeyOf(VniTable.class) != null, "Parent VNI table not specified");

        final MappingId mappingId = id.firstKeyOf(RemoteMapping.class).getId();
        checkState(remoteMappingContext.containsEid(mappingId, ctx.getMappingContext()),
                "No mapping stored for id %s", mappingId);

        final long vni = id.firstKeyOf(VniTable.class).getVirtualNetworkIdentifier();
        final Eid eid = copyEid(remoteMappingContext.getEid(mappingId, ctx.getMappingContext()));
        final MappingsDumpParams dumpParams = new MappingsDumpParamsBuilder()
                .setVni((int) vni)
                .setEidSet(QuantityType.SPECIFIC)
                .setEidType(getEidType(eid))
                .setEid(getEidAsByteArray(eid))
                .setPrefixLength(getPrefixLength(eid))
                .setFilter(FilterType.REMOTE)
                .build();

        LOG.debug("Dumping data for LocalMappings(id={})", id);
        final Optional<OneEidTableDetailsReplyDump> replyOptional =
                dumpManager.getDump(id, ctx.getModificationCache(), dumpParams);

        if (!replyOptional.isPresent() || replyOptional.get().oneEidTableDetails.isEmpty()) {
            return;
        }

        LOG.debug("Valid dump loaded");

        OneEidTableDetails details = replyOptional.get().oneEidTableDetails.stream()
                .filter(subtableFilterForRemoteMappings(id))
                .filter(a -> compareAddresses(eid.getAddress(),
                        getArrayAsEidLocal(valueOf(a.eidType), a.eid, a.eidPrefixLen, a.vni).getAddress()))
                .collect(
                        RWUtils.singleItemCollector());

        builder.setEid(getArrayAsEidRemote(valueOf(details.eidType), details.eid, details.eidPrefixLen, details.vni));
        builder.withKey(new RemoteMappingKey(new MappingId(id.firstKeyOf(RemoteMapping.class).getId())));
        builder.setTtl(resolveTtl(details.ttl));
        builder.setAuthoritative(
                new RemoteMapping.Authoritative(byteToBoolean(details.authoritative)));
        resolveMappings(builder, details, ctx.getModificationCache(), ctx.getMappingContext());
    }

    @Override
    public List<RemoteMappingKey> getAllIds(InstanceIdentifier<RemoteMapping> id, ReadContext context)
            throws ReadFailedException {

        checkState(id.firstKeyOf(VniTable.class) != null, "Parent VNI table not specified");
        final int vni = id.firstKeyOf(VniTable.class).getVirtualNetworkIdentifier().intValue();

        if (vni == 0) {
            // ignoring default vni mapping
            // it's not relevant for us and we also don't store mapping for such eid's
            // such mapping is used to create helper local mappings to process remote ones
            return Collections.emptyList();
        }

        //requesting all remote with specific vni
        final MappingsDumpParams dumpParams = new MappingsDumpParamsBuilder()
                .setEidSet(QuantityType.ALL)
                .setFilter(FilterType.REMOTE)
                .build();

        LOG.debug("Dumping data for LocalMappings(id={})", id);
        final Optional<OneEidTableDetailsReplyDump> replyOptional =
                dumpManager.getDump(id, context.getModificationCache(), dumpParams);

        if (!replyOptional.isPresent() || replyOptional.get().oneEidTableDetails.isEmpty()) {
            return Collections.emptyList();
        }

        return replyOptional.get()
                .oneEidTableDetails
                .stream()
                .filter(a -> a.vni == vni)
                .filter(subtableFilterForRemoteMappings(id))
                .map(detail ->
                    getArrayAsEidRemote(valueOf(detail.eidType), detail.eid, detail.eidPrefixLen, detail.vni))
                .map(remoteEid -> remoteMappingContext.getId(remoteEid, context.getMappingContext()))
                .map(MappingId::new)
                .map(RemoteMappingKey::new)
                .collect(Collectors.toList());
    }

    @Override
    public void merge(Builder<? extends DataObject> builder, List<RemoteMapping> readData) {
        ((RemoteMappingsBuilder) builder).setRemoteMapping(readData);
    }

    private void resolveMappings(final RemoteMappingBuilder builder,
                                 final OneEidTableDetails details,
                                 final ModificationCache cache,
                                 final MappingContext mappingContext) throws ReadFailedException {

        if (details.locatorSetIndex == -1) {
            bindNegativeMapping(builder, MapReplyAction.forValue(details.action));
        } else {
            // cache key needs to have locator set scope to not mix with cached data
            final Optional<OneLocatorDetailsReplyDump> reply;

            // this will serve to achieve that locators have locator set scope
            final InstanceIdentifier<Interface> locatorIfaceIdentifier = InstanceIdentifier.create(LocatorSets.class)
                    .child(LocatorSet.class,
                            new LocatorSetKey(locatorSetContext.getName(details.locatorSetIndex, mappingContext)))
                    .child(Interface.class);
            reply = locatorsDumpManager.getDump(locatorIfaceIdentifier, cache,
                    new LocatorDumpParamsBuilder().setLocatorSetIndex(details.locatorSetIndex).build());

            bindPositiveMapping(builder, reply.or(new OneLocatorDetailsReplyDump()));
        }
    }

    private void bindNegativeMapping(final RemoteMappingBuilder builder,
                                     final MapReplyAction action) {
        builder.setLocatorList(
                new NegativeMappingBuilder().setMapReply(new MapReplyBuilder().setMapReplyAction(action).build())
                        .build());
    }

    private void bindPositiveMapping(final RemoteMappingBuilder builder, final OneLocatorDetailsReplyDump reply) {
        builder.setLocatorList(
                new PositiveMappingBuilder()
                        .setRlocs(
                                new RlocsBuilder()
                                        .setLocator(reply
                                                .oneLocatorDetails
                                                .stream()
                                                .map(this::detailsToLocator)
                                                .collect(Collectors.toList()))
                                        .build()
                        )
                        .build()
        );
    }

    private Locator detailsToLocator(final OneLocatorDetails details) {
        final IpAddress address = arrayToIpAddress(byteToBoolean(details.isIpv6), details.ipAddress);
        return new LocatorBuilder()
                .setAddress(address)
                .withKey(new LocatorKey(address))
                .setPriority((short) details.priority)
                .setWeight((short) details.weight)
                .build();
    }

    @Nonnull
    @Override
    public Initialized<? extends DataObject> init(@Nonnull InstanceIdentifier<RemoteMapping> instanceIdentifier,
                                                  @Nonnull RemoteMapping remoteMapping,
                                                  @Nonnull ReadContext readContext) {
        return Initialized.create(remoteMappingPath(instanceIdentifier), remoteMapping);
    }
}
