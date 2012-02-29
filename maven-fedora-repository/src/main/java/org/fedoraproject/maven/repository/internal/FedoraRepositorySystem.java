/*
 * Copyright (c) 2012 Red Hat, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
 * CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package org.fedoraproject.maven.repository.internal;

import static java.util.Collections.singletonList;
import static org.apache.maven.artifact.Artifact.LATEST_VERSION;
import static org.fedoraproject.maven.repository.internal.RepositorySystemSessionHelper.validateSession;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.sonatype.aether.RepositorySystem;
import org.sonatype.aether.RepositorySystemSession;
import org.sonatype.aether.SyncContext;
import org.sonatype.aether.artifact.Artifact;
import org.sonatype.aether.collection.CollectRequest;
import org.sonatype.aether.collection.CollectResult;
import org.sonatype.aether.collection.DependencyCollectionException;
import org.sonatype.aether.deployment.DeployRequest;
import org.sonatype.aether.deployment.DeployResult;
import org.sonatype.aether.deployment.DeploymentException;
import org.sonatype.aether.graph.DependencyFilter;
import org.sonatype.aether.graph.DependencyNode;
import org.sonatype.aether.impl.ArtifactDescriptorReader;
import org.sonatype.aether.impl.ArtifactResolver;
import org.sonatype.aether.impl.DependencyCollector;
import org.sonatype.aether.impl.VersionRangeResolver;
import org.sonatype.aether.impl.internal.DefaultRepositorySystem;
import org.sonatype.aether.installation.InstallRequest;
import org.sonatype.aether.installation.InstallResult;
import org.sonatype.aether.installation.InstallationException;
import org.sonatype.aether.repository.LocalRepository;
import org.sonatype.aether.repository.LocalRepositoryManager;
import org.sonatype.aether.repository.MirrorSelector;
import org.sonatype.aether.repository.RemoteRepository;
import org.sonatype.aether.resolution.ArtifactDescriptorException;
import org.sonatype.aether.resolution.ArtifactDescriptorRequest;
import org.sonatype.aether.resolution.ArtifactDescriptorResult;
import org.sonatype.aether.resolution.ArtifactRequest;
import org.sonatype.aether.resolution.ArtifactResolutionException;
import org.sonatype.aether.resolution.ArtifactResult;
import org.sonatype.aether.resolution.DependencyRequest;
import org.sonatype.aether.resolution.DependencyResolutionException;
import org.sonatype.aether.resolution.DependencyResult;
import org.sonatype.aether.resolution.MetadataRequest;
import org.sonatype.aether.resolution.MetadataResult;
import org.sonatype.aether.resolution.VersionRangeRequest;
import org.sonatype.aether.resolution.VersionRangeResolutionException;
import org.sonatype.aether.resolution.VersionRangeResult;
import org.sonatype.aether.resolution.VersionRequest;
import org.sonatype.aether.resolution.VersionResolutionException;
import org.sonatype.aether.resolution.VersionResult;
import org.sonatype.aether.spi.log.Logger;
import org.sonatype.aether.spi.log.NullLogger;
import org.sonatype.aether.util.DefaultRepositorySystemSession;
import org.sonatype.aether.util.artifact.DefaultArtifact;
import org.sonatype.aether.version.Version;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
@Component(role = RepositorySystem.class, hint = "fedora")
public class FedoraRepositorySystem implements ArtifactDescriptorReader, ArtifactResolver, DependencyCollector, RepositorySystem, VersionRangeResolver {
    @Requirement
    private Logger logger = NullLogger.INSTANCE;

    @Requirement
    private ArtifactResolver artifactResolver;

    @Requirement(hint = "default")
    private DefaultRepositorySystem delegate;

    @Requirement
    private DependencyCollector dependencyCollector;

    private final boolean useJPP;
    private final RemoteRepository fedoraRepository;
    private final MirrorSelector mirrorSelector;

//    private WorkspaceReader jppRepository = new JavadirWorkspaceReader();
    private final JPPLocalRepositoryManager jppRepositoryManager = new JPPLocalRepositoryManager();

    public FedoraRepositorySystem() {
        this.useJPP = Boolean.getBoolean("fmvn.useJPP");
        // we only want to use this repository
        this.fedoraRepository = new RemoteRepository("fedora", "default", System.getProperty("fmvn.repo", "file:/usr/share/maven/repository"));
        this.mirrorSelector = new MirrorSelector() {
            @Override
            public RemoteRepository getMirror(RemoteRepository repository) {
                logger.warn("Using mirror " + fedoraRepository + " for " + repository);
                return fedoraRepository;
            }
        };
    }

    private void assertFedoraRepository(List<RemoteRepository> repositories) {
        if (repositories.size() != 1)
            throw new IllegalStateException("Wrong number of repositories in " + repositories);
        if (!repositories.get(0).equals(fedoraRepository))
            throw new IllegalStateException("Fedora repository not listed in " + repositories);
    }

    private void debugf(final String format, final Object... args) {
        if (!logger.isDebugEnabled())
            return;
        final String msg = args == null ? String.format(format) : String.format(format, args);
        logger.debug(msg);
    }

    private boolean hasArtifact(final RepositorySystemSession session, final VersionRangeRequest request, final Version version) {
        if (version == null)
            return false;

        /* this will blow, because it'll try to find a jar for a pom artifact
        try {
            final Artifact artifact = request.getArtifact();
            final Artifact versionedArtifact = new DefaultArtifact( artifact.getGroupId(), artifact.getArtifactId(), artifact.getClassifier(), artifact.getExtension(), version.toString(), artifact.getProperties(), artifact.getFile() );
            final ArtifactRequest alternateRequest = new ArtifactRequest(versionedArtifact, singletonList(fedoraRepository), request.getRequestContext())
                    .setTrace(request.getTrace());
            final ArtifactResult result = artifactResolver.resolveArtifact(session, alternateRequest);
            if (result.getExceptions().isEmpty())
                return true;
        } catch (ArtifactResolutionException e) {
            throw new RuntimeException(e);
        }
        */
        try {
            final ArtifactDescriptorRequest alternateRequest = new ArtifactDescriptorRequest(request.getArtifact(), singletonList(fedoraRepository), request.getRequestContext())
                    .setTrace(request.getTrace());
            final ArtifactDescriptorResult result = delegate.readArtifactDescriptor(session, alternateRequest);
            if (result.getExceptions().isEmpty())
                return true;
        } catch (ArtifactDescriptorException e) {
            throw new RuntimeException(e);
        }
        return false;
    }

    public void setDefaultRepositorySystem(DefaultRepositorySystem delegate) {
        if (delegate == null) {
            throw new IllegalArgumentException("default repository system has not been specified");
        }
        this.delegate = delegate;
    }

    public FedoraRepositorySystem setLogger(Logger logger) {
        this.logger = (logger != null) ? logger : NullLogger.INSTANCE;
        return this;
    }

    @Override
    public VersionRangeResult resolveVersionRange(RepositorySystemSession session, VersionRangeRequest request) throws VersionRangeResolutionException {
        debugf("resolveVersionRange %s", request);
//        assertFedoraRepository(request.getRepositories());
        try {
            // try Fedora local repo
            final VersionRangeRequest alternateRequest = new VersionRangeRequest(request.getArtifact(), singletonList(fedoraRepository), request.getRequestContext())
                    .setTrace(request.getTrace());
            final VersionRangeResult result = delegate.resolveVersionRange(session, alternateRequest);
//            logger.warn("result = " + result.getVersions());
            final Version highestVersion = result.getHighestVersion();
            if (result.getExceptions().isEmpty()) {
                // Did we find something in the repo?
                // TODO: this is the best thing I could think off, what is the correct check?
                if (highestVersion != null && result.getRepository(highestVersion) != null)
                    return result;
            }

            // the Fedora local repo might be running without metadata
            // TODO: how to check that?
            if (hasArtifact(session, request, highestVersion)) {
                result.setRepository(highestVersion, fedoraRepository);
                return result;
            }

            if (!useJPP)
                return result;

            // try JPP local repo
            // JPP will always return something regardless of the version, but without
            // a version readArtifactDescriptor will go into the DefaultVersionResolver
            // so we can't put in LATEST.
//            throw new RuntimeException("NYI");
            result.setRepository(highestVersion, jppRepositoryManager.getRepository());
            logger.warn("Could not resolve version range " + request + ", using JPP " + result);
            return result;
        } catch (VersionRangeResolutionException e) {
            throw e;
        }
    }

    @Override
    public VersionResult resolveVersion(RepositorySystemSession session, VersionRequest request) throws VersionResolutionException {
        throw new RuntimeException("NYI: org.fedoraproject.maven.repository.internal.FedoraRepositorySystem.resolveVersion");
    }

    @Override
    public ArtifactDescriptorResult readArtifactDescriptor(RepositorySystemSession session, ArtifactDescriptorRequest request) throws ArtifactDescriptorException {
        /* bummer
        final RepositorySystemSession alternateSession = new DefaultRepositorySystemSession(session)
                .setOffline(false)
                .setMirrorSelector(mirrorSelector)
                ;
        */
        ArtifactDescriptorException originalException = null;
        final Artifact artifact = request.getArtifact();
        // try Fedora local repo
        {
            try {
                final ArtifactDescriptorRequest alternateRequest = new ArtifactDescriptorRequest(request.getArtifact(), singletonList(fedoraRepository), request.getRequestContext())
                        .setTrace(request.getTrace());
                final ArtifactDescriptorResult result = delegate.readArtifactDescriptor(session, alternateRequest);
                if (result.getExceptions().isEmpty())
                    return result;
            } catch (ArtifactDescriptorException e) {
                originalException = e;
            }
        }
        // try Fedora local repo with LATEST
        {
            try {
                final Artifact alternateArtifact = new DefaultArtifact(artifact.getGroupId(), artifact.getArtifactId(), artifact.getClassifier(), artifact.getExtension(), LATEST_VERSION, artifact.getProperties(), artifact.getFile());
                final ArtifactDescriptorRequest alternateRequest = new ArtifactDescriptorRequest(alternateArtifact, singletonList(fedoraRepository), request.getRequestContext())
                        .setTrace(request.getTrace());
                final ArtifactDescriptorResult result = delegate.readArtifactDescriptor(session, alternateRequest);
                if (result.getExceptions().isEmpty()) {
                    logger.warn("Could not find artifact descriptor " + artifact + ", using LATEST " + result.getArtifact());
                    return result;
                }
            } catch (ArtifactDescriptorException e) {
                logger.debug("LATEST resolution of " + artifact + " failed", e);
                if (originalException == null)
                    originalException = e;
            }
        }
        // try JPP local repo
        if (useJPP) {
            // use maven as much as possible
            final RepositorySystemSession alternateSession = openJPP(session);
            try {
                final ArtifactDescriptorRequest alternateRequest = new ArtifactDescriptorRequest(request.getArtifact(), singletonList(fedoraRepository), request.getRequestContext())
                        .setTrace(request.getTrace());
                final ArtifactDescriptorResult result = delegate.readArtifactDescriptor(alternateSession, alternateRequest);
//                logger.warn("result from JPP " + result);
                if (result.getExceptions().isEmpty()) {
                    // TODO: I may want to muck the result a bit to make sure JPP is also used for resolveArtifact
//                    // JPP probably did not return the proper version, which makes the MavenPluginValidator barf
//                    // lets muck it
//                    result.setArtifact(new JPPArtifact(result.getArtifact()));
                    logger.warn("Could not find artifact descriptor " + artifact + ", using JPP " + result.getArtifact());
                    return result;
                }
            } catch (ArtifactDescriptorException e) {
                logger.debug("JPP resolution of " + artifact + " failed", e);
                if (originalException == null)
                    originalException = e;
            }
        }
        if (originalException != null)
            throw originalException;
        throw new RuntimeException("NYI: org.fedoraproject.maven.repository.internal.FedoraRepositorySystem.readArtifactDescriptor");
    }

    @Override
    public CollectResult collectDependencies(RepositorySystemSession session, CollectRequest request) throws DependencyCollectionException {
        debugf("collectDependencies %s", request);
        validateSession(session);

        // delegate has been wired up to come back to us, so this must be a real implementation

        DependencyCollectionException originalException = null;
        // FIXME: work in progress, we need to aggregate all
        // try JPP local repo
        if (useJPP) {
            // use maven as much as possible
            final RepositorySystemSession alternateSession = openJPP(session);
            try {
                final CollectRequest alternateRequest = new CollectRequest(request.getRoot(), request.getDependencies(), singletonList(fedoraRepository))
                        .setManagedDependencies(request.getManagedDependencies())
                        .setTrace(request.getTrace());
                final CollectResult result = dependencyCollector.collectDependencies(alternateSession, alternateRequest);
                if (result.getExceptions().isEmpty() && result.getRoot() != null) {
                    logger.warn("collectDependencies: result from JPP " + result);
//                    logger.warn("TODO");
                    return result;
                }
            } catch (DependencyCollectionException e) {
                logger.debug("JPP collect dependencies of " + request + " failed", e);
                if (originalException == null)
                    originalException = e;
            }
        }
        // try Fedora local repo
        {
            try {
                final CollectRequest alternateRequest = new CollectRequest()
                        .setRoot(request.getRoot())
                        .setDependencies(request.getDependencies())
                        .setManagedDependencies(request.getManagedDependencies())
                        .setRequestContext(request.getRequestContext())
                        .setTrace(request.getTrace())
                        .setRepositories(singletonList(fedoraRepository));
                final CollectResult result = dependencyCollector.collectDependencies(session, alternateRequest);
                logger.warn("collectDependencies: result = " + result);
                if (result.getExceptions().isEmpty())
                    return result;
            } catch (DependencyCollectionException e) {
                logger.warn("collect dependencies failed ", e);
                originalException = e;
            }
        }
        if (originalException != null)
            throw originalException;
        throw new RuntimeException("NYI: org.fedoraproject.maven.repository.internal.FedoraRepositorySystem.collectDependencies");
    }

    @Override
    public DependencyResult resolveDependencies(RepositorySystemSession session, DependencyRequest request) throws DependencyResolutionException {
        debugf("resolveDependencies %s", request.getCollectRequest());
        // TODO: assert that the request does not traverse repos
        return delegate.resolveDependencies(session, request);
//        throw new RuntimeException("NYI: org.fedoraproject.maven.repository.internal.FedoraRepositorySystem.resolveDependencies");
    }

    @Override
    public List<ArtifactResult> resolveDependencies(RepositorySystemSession session, DependencyNode node, DependencyFilter filter) throws ArtifactResolutionException {
        throw new RuntimeException("NYI: org.fedoraproject.maven.repository.internal.FedoraRepositorySystem.resolveDependencies");
    }

    @Override
    public List<ArtifactResult> resolveDependencies(RepositorySystemSession session, CollectRequest request, DependencyFilter filter) throws DependencyCollectionException, ArtifactResolutionException {
        throw new RuntimeException("NYI: org.fedoraproject.maven.repository.internal.FedoraRepositorySystem.resolveDependencies");
    }

    @Override
    public ArtifactResult resolveArtifact(RepositorySystemSession session, ArtifactRequest request) throws ArtifactResolutionException {
        debugf("resolveArtifact %s", request);
        validateSession(session);

        // delegate has been wired up to come back to us, so this must be a real implementation

        ArtifactResolutionException originalException = null;
        final Artifact artifact = request.getArtifact();
        // try Fedora local repo
        {
            try {
                final ArtifactRequest alternateRequest = new ArtifactRequest(request.getArtifact(), singletonList(fedoraRepository), request.getRequestContext())
                        .setDependencyNode(request.getDependencyNode())
                        .setTrace(request.getTrace());
                final ArtifactResult result = artifactResolver.resolveArtifact(session, alternateRequest);
                if (result.getExceptions().isEmpty())
                    return result;
            } catch (ArtifactResolutionException e) {
                originalException = e;
            }
        }
        // try Fedora local repo with LATEST
        if (!artifact.getVersion().equals(LATEST_VERSION))
//            throw new IllegalStateException("NYI: LATEST should not appear during resolveArtifact, should it?");
        {
            try {
                final Artifact alternateArtifact = new DefaultArtifact(artifact.getGroupId(), artifact.getArtifactId(), artifact.getClassifier(), artifact.getExtension(), LATEST_VERSION, artifact.getProperties(), artifact.getFile());
                final ArtifactRequest alternateRequest = new ArtifactRequest(alternateArtifact, Collections.singletonList(fedoraRepository), request.getRequestContext())
                        .setTrace(request.getTrace());
                final ArtifactResult result = delegate.resolveArtifact(session, alternateRequest);
                if (result.getExceptions().isEmpty()) {
                    logger.warn("Could not find artifact " + artifact + ", using LATEST " + result.getArtifact());
                    return result;
                }
            } catch (ArtifactResolutionException e) {
                logger.debug("LATEST resolution of " + artifact + " failed", e);
                if (originalException == null)
                    originalException = e;
            }
        }
        // try JPP local repo
        if (useJPP) {
            // use maven as much as possible
            final RepositorySystemSession alternateSession = openJPP(session);
            try {
                final ArtifactRequest alternateRequest = new ArtifactRequest(request.getArtifact(), singletonList(fedoraRepository), request.getRequestContext())
                        .setDependencyNode(request.getDependencyNode())
                        .setTrace(request.getTrace());
                final ArtifactResult result = artifactResolver.resolveArtifact(alternateSession, alternateRequest);
                if (result.getExceptions().isEmpty()) {
                    logger.warn("Could not find artifact " + artifact + " in " + fedoraRepository + ", using JPP " + result.getArtifact());
                    return result;
                }
            } catch (ArtifactResolutionException e) {
                logger.debug("JPP resolution of " + artifact + " failed", e);
                if (originalException == null)
                    originalException = e;
            }
        }
        if (originalException != null)
            throw originalException;
        throw new RuntimeException("NYI: org.fedoraproject.maven.repository.internal.FedoraRepositorySystem.resolveArtifact");
    }

    @Override
    public List<ArtifactResult> resolveArtifacts(RepositorySystemSession session, Collection<? extends ArtifactRequest> requests) throws ArtifactResolutionException {
//        throw new RuntimeException("NYI: org.fedoraproject.maven.repository.internal.FedoraRepositorySystem.resolveArtifacts");
        // Do this very straight forward instead of fast and complex
        final List<ArtifactResult> results = new ArrayList<ArtifactResult>(requests.size());
        for (ArtifactRequest request : requests) {
            results.add(resolveArtifact(session, request));
        }
        return results;
    }

    @Override
    public List<MetadataResult> resolveMetadata(RepositorySystemSession session, Collection<? extends MetadataRequest> requests) {
        // TODO: JPP does not support metadata, but the Fedora repo might
        logger.warn("resolveMetadata " + requests);
        //return delegate.resolveMetadata(session, requests);
        throw new RuntimeException("NYI: org.fedoraproject.maven.repository.internal.FedoraRepositorySystem.resolveMetadata");
//        final List<MetadataResult> results = new ArrayList<MetadataResult>(requests.size());
//        for (MetadataRequest request : requests) {
//            results.add(new MetadataResult(request));
//        }
//        return results;
    }

    @Override
    public InstallResult install(RepositorySystemSession session, InstallRequest request) throws InstallationException {
        throw new RuntimeException("NYI: org.fedoraproject.maven.repository.internal.FedoraRepositorySystem.install");
    }

    @Override
    public DeployResult deploy(RepositorySystemSession session, DeployRequest request) throws DeploymentException {
        throw new RuntimeException("NYI: org.fedoraproject.maven.repository.internal.FedoraRepositorySystem.deploy");
    }

    @Override
    public LocalRepositoryManager newLocalRepositoryManager(LocalRepository localRepository) {
        return delegate.newLocalRepositoryManager(localRepository);
    }

    @Override
    public SyncContext newSyncContext(RepositorySystemSession session, boolean shared) {
        throw new RuntimeException("NYI: org.fedoraproject.maven.repository.internal.FedoraRepositorySystem.newSyncContext");
    }

    private RepositorySystemSession openJPP(final RepositorySystemSession current) {
        assert useJPP : "useJPP is not set";
        final RepositorySystemSession alternateSession = new DefaultRepositorySystemSession(current)
                .setOffline(true)
//                    .setWorkspaceReader(jppRepository);
                .setLocalRepositoryManager(jppRepositoryManager);
        return alternateSession;
    }
}