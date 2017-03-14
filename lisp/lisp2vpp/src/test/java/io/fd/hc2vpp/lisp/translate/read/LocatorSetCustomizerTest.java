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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import io.fd.hc2vpp.common.test.read.InitializingListReaderCustomizerTest;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.honeycomb.test.tools.HoneycombTestRunner;
import io.fd.honeycomb.test.tools.annotations.InjectTestData;
import io.fd.honeycomb.test.tools.annotations.InjectablesProcessor;
import io.fd.honeycomb.test.tools.annotations.SchemaContextProvider;
import io.fd.honeycomb.translate.spi.read.ReaderCustomizer;
import io.fd.vpp.jvpp.core.dto.LispLocatorSetDetails;
import io.fd.vpp.jvpp.core.dto.LispLocatorSetDetailsReplyDump;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170315.$YangModuleInfoImpl;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170315.Lisp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170315.LispState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170315.lisp.feature.data.grouping.LispFeatureData;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170315.locator.sets.grouping.LocatorSets;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170315.locator.sets.grouping.LocatorSetsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170315.locator.sets.grouping.locator.sets.LocatorSet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170315.locator.sets.grouping.locator.sets.LocatorSetBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170315.locator.sets.grouping.locator.sets.LocatorSetKey;
import org.opendaylight.yangtools.sal.binding.generator.impl.ModuleInfoBackedContext;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

@RunWith(HoneycombTestRunner.class)
public class LocatorSetCustomizerTest
        extends InitializingListReaderCustomizerTest<LocatorSet, LocatorSetKey, LocatorSetBuilder>
        implements LispInitTest {

    private static final String LOC_1_PATH = "/lisp:lisp-state" +
            "/lisp:lisp-feature-data" +
            "/lisp:locator-sets" +
            "/lisp:locator-set[lisp:name='loc1']";
    private InstanceIdentifier<LocatorSet> emptyId;
    private InstanceIdentifier<LocatorSet> validId;

    public LocatorSetCustomizerTest() {
        super(LocatorSet.class, LocatorSetsBuilder.class);
    }

    @Before
    public void init() {
        emptyId = InstanceIdentifier.create(LocatorSet.class);
        validId = InstanceIdentifier.create(LocatorSets.class).child(LocatorSet.class, new LocatorSetKey("loc-set"));

        defineDumpData();
        defineMapping(mappingContext, "loc-set", 1, "locator-set-context");
    }

    private void defineDumpData() {
        LispLocatorSetDetailsReplyDump dump = new LispLocatorSetDetailsReplyDump();
        LispLocatorSetDetails detail = new LispLocatorSetDetails();
        detail.context = 4;
        detail.lsName = "loc-set".getBytes(StandardCharsets.UTF_8);
        detail.lsIndex = 1;

        dump.lispLocatorSetDetails = ImmutableList.of(detail);

        when(api.lispLocatorSetDump(any())).thenReturn(future(dump));
    }


    @Test
    public void readCurrentAttributes() throws Exception {
        LocatorSetBuilder builder = new LocatorSetBuilder();
        getCustomizer().readCurrentAttributes(validId, builder, ctx);

        assertNotNull(builder);
        assertEquals("loc-set", builder.getName());
        assertEquals("loc-set", builder.getKey().getName());
    }

    @Test
    public void getAllIds() throws Exception {
        final List<LocatorSetKey> keys = getCustomizer().getAllIds(emptyId, ctx);

        assertEquals(1, keys.size());
        assertEquals("loc-set", keys.get(0).getName());
    }

    @Test
    public void testInit(@InjectTestData(resourcePath = "/locator-set.json", id = LOC_1_PATH) LocatorSet locatorSet) {
        final LocatorSetKey loc1Key = new LocatorSetKey("loc1");
        final KeyedInstanceIdentifier<LocatorSet, LocatorSetKey> operationalPath = InstanceIdentifier.create(LispState.class)
                .child(LispFeatureData.class)
                .child(LocatorSets.class)
                .child(LocatorSet.class, loc1Key);

        final KeyedInstanceIdentifier<LocatorSet, LocatorSetKey> configPath = InstanceIdentifier.create(Lisp.class)
                .child(LispFeatureData.class)
                .child(LocatorSets.class)
                .child(LocatorSet.class, loc1Key);

        invokeInitTest(operationalPath, locatorSet, configPath, locatorSet);
    }

    @Override
    protected ReaderCustomizer<LocatorSet, LocatorSetBuilder> initCustomizer() {
        return new LocatorSetCustomizer(api, new NamingContext("loc", "locator-set-context"));
    }
}