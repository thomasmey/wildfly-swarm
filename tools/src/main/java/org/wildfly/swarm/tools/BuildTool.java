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
package org.wildfly.swarm.tools;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.ByteArrayAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.impl.base.asset.ZipFileEntryAsset;
import org.wildfly.swarm.bootstrap.util.WildFlySwarmApplicationConf;
import org.wildfly.swarm.bootstrap.util.WildFlySwarmBootstrapConf;
import org.wildfly.swarm.bootstrap.util.WildFlySwarmDependenciesConf;

/**
 * @author Bob McWhirter
 */
public class BuildTool {

    private final JavaArchive archive;

    private String mainClass;

    private String contextPath = "/";

    private boolean bundleDependencies = true;

    private boolean resolveTransitiveDependencies = false;


    private DependencyManager dependencyManager = new DependencyManager();

    private final Set<String> resourceDirectories = new HashSet<>();

    private ProjectAsset projectAsset;

    private Properties properties = new Properties();

    private Set<String> additionnalModules = new HashSet<>();

    public BuildTool() {
        this.archive = ShrinkWrap.create(JavaArchive.class);
    }

    public BuildTool mainClass(String mainClass) {
        this.mainClass = mainClass;
        return this;
    }

    public BuildTool contextPath(String contextPath) {
        this.contextPath = contextPath;
        return this;
    }

    public BuildTool properties(Properties properties) {
        this.properties.putAll(properties);
        return this;
    }

    public BuildTool bundleDependencies(boolean bundleDependencies) {
        this.bundleDependencies = bundleDependencies;
        return this;
    }

    public BuildTool resolveTransitiveDependencies(boolean resolveTransitiveDependencies) {
        this.resolveTransitiveDependencies = resolveTransitiveDependencies;
        return this;
    }

    public BuildTool projectArtifact(String groupId, String artifactId, String version, String packaging, File file) {
        this.projectAsset = new ArtifactAsset(new ArtifactSpec(null, groupId, artifactId, version, packaging, null, file));
        return this;
    }

    public BuildTool projectArchive(Archive archive) {
        this.projectAsset = new ArchiveAsset(archive);
        return this;
    }

    public BuildTool dependency(String scope, String groupId, String artifactId, String version, String packaging, String classifier, File file) {
        this.dependencyManager.addDependency(new ArtifactSpec(scope, groupId, artifactId, version, packaging, classifier, file));
        return this;
    }

    public Set<String> additionnalModules() {
        return this.additionnalModules;
    }

    public BuildTool artifactResolvingHelper(ArtifactResolvingHelper resolver) {
        this.dependencyManager.setArtifactResolvingHelper(resolver);
        return this;
    }

    public BuildTool resourceDirectory(String dir) {
        this.resourceDirectories.add(dir);
        return this;
    }

    public File build(String baseName, Path dir) throws Exception {
        build();
        return createJar(baseName, dir);
    }

    public Archive build() throws Exception {
        analyzeDependencies();
        addWildflySwarmBootstrapJar();
        addWildFlyBootstrapConf();
        addManifest();
        addWildFlySwarmProperties();
        addWildFlySwarmApplicationConf();
        addWildFlySwarmDependenciesConf();
        addAdditionnalModules();
        populateUberJarMavenRepository();
        return this.archive;
    }

    protected void analyzeDependencies() throws Exception {
        this.dependencyManager.analyzeDependencies(this.resolveTransitiveDependencies);
    }

    private void addWildflySwarmBootstrapJar() throws BuildException, IOException {
        ArtifactSpec artifact = this.dependencyManager.findWildFlySwarmBootstrapJar();

        if (!bootstrapJarShadesJBossModules(artifact.file)) {
            ArtifactSpec jbossModules = this.dependencyManager.findJBossModulesJar();
            expandArtifact(jbossModules.file);
        }
        expandArtifact(artifact.file);
    }

    private void addManifest() throws IOException {
        UberJarManifestAsset manifest = new UberJarManifestAsset(this.mainClass);
        this.archive.add(manifest);
    }

    private void addWildFlySwarmProperties() throws IOException {
        Properties props = new Properties();

        Enumeration<?> propNames = this.properties.propertyNames();

        while (propNames.hasMoreElements()) {
            String eachName = (String) propNames.nextElement();
            String eachValue = this.properties.get(eachName).toString();
            props.put(eachName, eachValue);
        }
        props.setProperty("wildfly.swarm.app.artifact", this.projectAsset.getSimpleName());
        props.setProperty("wildfly.swarm.context.path", this.contextPath);

        ByteArrayOutputStream propsBytes = new ByteArrayOutputStream();
        props.store(propsBytes, "Generated by WildFly Swarm");

        this.archive.addAsManifestResource(new ByteArrayAsset(propsBytes.toByteArray()), "wildfly-swarm.properties");
    }


    private void addWildFlyBootstrapConf() throws Exception {
        WildFlySwarmBootstrapConf bootstrapConf = this.dependencyManager.getWildFlySwarmBootstrapConf();
        this.archive.add(new StringAsset(bootstrapConf.toString()), WildFlySwarmBootstrapConf.CLASSPATH_LOCATION);
    }



    private void addWildFlySwarmDependenciesConf() throws IOException {
        WildFlySwarmDependenciesConf depsConf = this.dependencyManager.getWildFlySwarmDependenciesConf();
        this.archive.add(new StringAsset(depsConf.toString()), WildFlySwarmDependenciesConf.CLASSPATH_LOCATION);
    }

    private void addWildFlySwarmApplicationConf() throws Exception {
        WildFlySwarmApplicationConf appConf = this.dependencyManager.getWildFlySwarmApplicationConf(this.projectAsset);
        this.archive.add(new StringAsset(appConf.toString()), WildFlySwarmApplicationConf.CLASSPATH_LOCATION);
        this.archive.add(this.projectAsset);
    }


    private File createJar(String baseName, Path dir) throws IOException {
        File out = new File(dir.toFile(), baseName + "-swarm.jar");
        ZipExporter exporter = this.archive.as(ZipExporter.class);
        exporter.exportTo(out, true);
        return out;
    }

    public boolean bootstrapJarShadesJBossModules(File artifactFile) throws IOException {
        JarFile jarFile = new JarFile(artifactFile);
        Enumeration<JarEntry> entries = jarFile.entries();

        boolean jbossModulesFound = false;

        while (entries.hasMoreElements()) {
            JarEntry each = entries.nextElement();
            if (each.getName().startsWith("org/jboss/modules/ModuleLoader")) {
                jbossModulesFound = true;
            }
        }

        return jbossModulesFound;
    }

    public void expandArtifact(File artifactFile) throws IOException {
        JarFile jarFile = new JarFile(artifactFile);
        Enumeration<JarEntry> entries = jarFile.entries();

        while (entries.hasMoreElements()) {
            JarEntry each = entries.nextElement();
            if (each.getName().startsWith("META-INF")) {
                continue;
            }
            if (each.isDirectory()) {
                continue;
            }
            this.archive.add(new ZipFileEntryAsset(jarFile, each), each.getName());
        }
    }

    private void addAdditionnalModules() {
        for (String additionnalModule : additionnalModules) {
            File file = new File(additionnalModule);
            this.archive.addAsResource(file, "modules");
        }
    }

    private void populateUberJarMavenRepository() throws Exception {
        if ( this.bundleDependencies ) {
            this.dependencyManager.populateUberJarMavenRepository( this.archive );
        }
    }
}

