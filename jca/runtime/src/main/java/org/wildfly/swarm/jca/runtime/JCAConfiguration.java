/**
 * Copyright 2015 Red Hat, Inc, and individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wildfly.swarm.jca.runtime;

import java.util.List;

import org.jboss.dmr.ModelNode;
import org.wildfly.swarm.config.runtime.invocation.Marshaller;
import org.wildfly.swarm.container.runtime.AbstractServerConfiguration;
import org.wildfly.swarm.jca.JCAFraction;

/**
 * @author Bob McWhirter
 * @author Lance Ball
 */
public class JCAConfiguration extends AbstractServerConfiguration<JCAFraction> {

    public JCAConfiguration() {
        super(JCAFraction.class);
    }

    @Override
    public JCAFraction defaultFraction() {
        return JCAFraction.createDefaultFraction();
    }

    @Override
    public List<ModelNode> getList(JCAFraction fraction) throws Exception {
        return Marshaller.marshal(fraction);
    }
}
