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

package io.fd.hc2vpp.lisp.translate.write;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170315.MapRequestMode.DestinationOnly;
import static org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170315.MapRequestMode.SourceDestination;

import io.fd.hc2vpp.common.test.write.WriterCustomizerTest;
import io.fd.vpp.jvpp.core.dto.LispMapRequestMode;
import io.fd.vpp.jvpp.core.dto.LispMapRequestModeReply;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170315.map.request.mode.grouping.MapRequestMode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170315.map.request.mode.grouping.MapRequestModeBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class MapRequestModeCustomizerTest extends WriterCustomizerTest {

    private static final InstanceIdentifier<MapRequestMode> ID = InstanceIdentifier.create(MapRequestMode.class);
    private MapRequestModeCustomizer customizer;
    private MapRequestMode sourceDestinationMode;
    private MapRequestMode destinationOnlyMode;

    @Captor
    private ArgumentCaptor<LispMapRequestMode> requestCaptor;

    @Override
    protected void setUpTest() throws Exception {
        customizer = new MapRequestModeCustomizer(api);
        sourceDestinationMode = new MapRequestModeBuilder()
                .setMode(SourceDestination)
                .build();
        destinationOnlyMode = new MapRequestModeBuilder()
                .setMode(DestinationOnly)
                .build();
        when(api.lispMapRequestMode(any(LispMapRequestMode.class))).thenReturn(future(new LispMapRequestModeReply()));
    }

    @Test
    public void writeCurrentAttributes() throws Exception {
        customizer.writeCurrentAttributes(ID, sourceDestinationMode, writeContext);
        verifyModeRequest(SourceDestination);
    }

    @Test
    public void updateCurrentAttributes() throws Exception {
        customizer.updateCurrentAttributes(ID, sourceDestinationMode, destinationOnlyMode, writeContext);
        verifyModeRequest(DestinationOnly);
    }

    @Test
    public void deleteCurrentAttributes() throws Exception {
        verify(api, times(0)).lispMapRequestMode(any());
    }

    private void verifyModeRequest(
            final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170315.MapRequestMode mode) {
        verify(api, times(1)).lispMapRequestMode(requestCaptor.capture());

        final LispMapRequestMode request = requestCaptor.getValue();
        assertEquals(mode.getIntValue(), request.mode);
    }
}