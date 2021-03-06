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
package org.wildfly.swarm.jgroups;

import org.wildfly.swarm.config.JGroups;
import org.wildfly.swarm.container.Container;
import org.wildfly.swarm.container.Fraction;
import org.wildfly.swarm.container.SocketBinding;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Bob McWhirter
 */
public class JGroupsFraction extends JGroups<JGroupsFraction> implements Fraction {

    public JGroupsFraction() {
    }


    public static JGroupsFraction defaultFraction() {
        return new JGroupsFraction()
                .defaultChannel( "swarm-jgroups")
                .stack( "udp", (s)->{
                    s.transport( "UDP", (t)->{
                        t.socketBinding("jgroups-udp");
                    });
                    if (System.getenv("OPENSHIFT_BUILD_NAME") != null ||
                            System.getenv("OPENSHIFT_BUILD_REFERENCE") != null ||
                            "openshift".equalsIgnoreCase(System.getProperty("wildfly.swarm.environment"))) {
                        s.protocol( "openshift.KUBE_PING" );
                    } else {
                        s.protocol( "PING" );
                    }
                    s.protocol( "FD_SOCK", (p)->{
                        p.socketBinding( "jgroups-udp-fd" );
                    });
                    s.protocol( "FD_ALL" );
                    s.protocol( "VERIFY_SUSPECT" );
                    s.protocol( "pbcast.NAKACK2" );
                    s.protocol( "UNICAST3" );
                    s.protocol( "pbcast.STABLE" );
                    s.protocol( "pbcast.GMS" );
                    s.protocol( "UFC" );
                    s.protocol( "MFC" );
                    s.protocol( "FRAG2" );
                    s.protocol( "RSVP" );
                })
                .channel( "swarm-jgroups", (c)->{
                    c.stack( "udp" );
                });
    }

    @Override
    public void initialize(Container.InitContext initContext) {
        initContext.socketBinding(
                new SocketBinding("jgroups-udp")
                        .port(55200)
                        .multicastAddress("${jboss.default.multicast.address:230.0.0.4}")
                        .multicastPort(45688));

        initContext.socketBinding(
                new SocketBinding("jgroups-udp-fd")
                        .port(54200));

        initContext.socketBinding(
                new SocketBinding("jgroups-mping")
                        .port(0)
                        .multicastAddress("${jboss.default.multicast.address:230.0.0.4}")
                        .multicastPort(45700));

        initContext.socketBinding(
                new SocketBinding("jgroups-tcp")
                        .port(7600));

        initContext.socketBinding(
                new SocketBinding("jgroups-tcp-fd")
                        .port(57600));

    }
}
