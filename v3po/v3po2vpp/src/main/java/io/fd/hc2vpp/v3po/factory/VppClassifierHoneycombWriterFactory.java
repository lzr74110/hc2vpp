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

package io.fd.hc2vpp.v3po.factory;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import io.fd.hc2vpp.v3po.vppclassifier.ClassifySessionWriter;
import io.fd.hc2vpp.v3po.vppclassifier.ClassifyTableWriter;
import io.fd.hc2vpp.v3po.vppclassifier.VppClassifierContextManager;
import io.fd.honeycomb.translate.impl.write.GenericListWriter;
import io.fd.honeycomb.translate.write.WriterFactory;
import io.fd.honeycomb.translate.write.registry.ModifiableWriterRegistryBuilder;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.classifier.rev161214.VppClassifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.classifier.rev161214.classify.table.base.attributes.ClassifySession;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.classifier.rev161214.vpp.classifier.ClassifyTable;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;

public final class VppClassifierHoneycombWriterFactory implements WriterFactory {

    public static final InstanceIdentifier<ClassifyTable> CLASSIFY_TABLE_ID =
            InstanceIdentifier.create(VppClassifier.class).child(ClassifyTable.class);

    public static final InstanceIdentifier<ClassifySession> CLASSIFY_SESSION_ID =
            CLASSIFY_TABLE_ID.child(ClassifySession.class);

    private final FutureJVppCore jvpp;
    private final VppClassifierContextManager classifyTableContext;

    @Inject
    public VppClassifierHoneycombWriterFactory(@Nonnull final FutureJVppCore jvpp,
                                               @Named("classify-table-context") @Nonnull final VppClassifierContextManager classifyTableContext) {
        this.jvpp = jvpp;
        this.classifyTableContext = classifyTableContext;
    }

    @Override
    public void init(@Nonnull final ModifiableWriterRegistryBuilder registry) {
        // Ordering here is: First create table, then create sessions and then assign as ACL
        // ClassifyTable
        registry.addBefore(
                new GenericListWriter<>(CLASSIFY_TABLE_ID, new ClassifyTableWriter(jvpp, classifyTableContext)),
                CLASSIFY_SESSION_ID);
        //  ClassifyTableSession
        registry.addBefore(
                new GenericListWriter<>(CLASSIFY_SESSION_ID, new ClassifySessionWriter(jvpp, classifyTableContext)),
                InterfacesWriterFactory.ACL_ID);
    }
}