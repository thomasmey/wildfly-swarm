package org.wildfly.swarm.bootstrap.util;

import java.io.ByteArrayOutputStream;
import java.lang.reflect.Method;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.jboss.shrinkwrap.api.asset.ByteArrayAsset;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.wildfly.swarm.bootstrap.AbstractBootstrapIntegrationTestCase;

import static org.fest.assertions.Assertions.assertThat;

/**
 * @author Bob McWhirter
 */
public class LayoutIT extends AbstractBootstrapIntegrationTestCase {

    @Test
    public void testIsUberJar() throws Exception {
        JavaArchive archive = createBootstrapArchive();

        ClassLoader cl = createClassLoader(archive);
        Class<?> layoutClass = cl.loadClass(Layout.class.getName());

        Method getInstance = layoutClass.getMethod("getInstance");
        Object layout = getInstance.invoke(layoutClass);

        Method isUberJar = layoutClass.getMethod("isUberJar");
        Object result = isUberJar.invoke(layout);

        assertThat(result).isEqualTo(true);
    }

    @Test
    public void testGetManifest() throws Exception {
        JavaArchive archive = createBootstrapArchive();

        archive.addAsManifestResource(EmptyAsset.INSTANCE, "wildfly-swarm.properties");

        Manifest manifest = new Manifest();

        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        manifest.getMainAttributes().put(new Attributes.Name("Wildfly-Swarm-Main-Class"), "MyMainClass");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        manifest.write(out);
        out.close();
        archive.addAsManifestResource(new ByteArrayAsset(out.toByteArray()), "MANIFEST.MF");

        ClassLoader cl = createClassLoader(archive);
        Class<?> layoutClass = cl.loadClass(Layout.class.getName());

        Method getInstance = layoutClass.getMethod("getInstance");
        Object layout = getInstance.invoke(layoutClass);

        Method isUberJar = layoutClass.getMethod("isUberJar");
        Object result = isUberJar.invoke(layout);

        assertThat(result).isEqualTo(true);

        Method getManifest = layoutClass.getMethod("getManifest");
        Manifest fetchedManifest = (Manifest) getManifest.invoke(layout);

        assertThat(fetchedManifest).isNotNull();
        assertThat(fetchedManifest.getMainAttributes().get(new Attributes.Name("Wildfly-Swarm-Main-Class"))).isEqualTo("MyMainClass");
    }

}
