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
@Component(role = RepositorySystem.class, hint = "foss")
public class FossRepositorySystem
        implements ArtifactDescriptorReader, ArtifactResolver,
        DependencyCollector, RepositorySystem, VersionRangeResolver {

    @Requirement
    private Logger logger = NullLogger.INSTANCE;

    @Requirement
    private ArtifactResolver artifactResolver;

    @Requirement(hint = "default")
    private DefaultRepositorySystem delegate;

    @Requirement
    private DependencyCollector dependencyCollector;

    private boolean useJpp;

    private final RemoteRepository fossRepository;

//    private WorkspaceReader jppRepository = new JavadirWorkspaceReader();
    private final JPPLocalRepositoryManager jppRepositoryManager =
        new JPPLocalRepositoryManager();

    public FossRepositorySystem() {
        this.useJpp = Boolean.getBoolean("fre.useJpp");

        // we only want to use this repository
        this.fossRepository = new RemoteRepository("foss", "default",
                System.getProperty("fre.repo", "file:/usr/share/maven/repository"));
    }

    private void assertFedoraRepository(List<RemoteRepository> repositories) {
        if (repositories.size() != 1)
            throw new IllegalStateException(
                    "Wrong number of repositories in " + repositories);

        if (!repositories.get(0).equals(fossRepository))
            throw new IllegalStateException(
                    "Fedora repository not listed in " + repositories);
    }

    private void debugf(final String format, final Object... args) {
        if (!logger.isDebugEnabled())
            return;

        final String msg = args == null
                ? String.format(format)
                : String.format(format, args);

        logger.debug(msg);
    }

    private boolean hasArtifact(
            final RepositorySystemSession session,
            final VersionRangeRequest request,
            final Version version) {

        if (version == null) {
            return false;
        }

        /* this will blow, because it'll try to find a jar for a pom artifact
        try {
            final Artifact artifact = request.getArtifact();
            final Artifact versionedArtifact =
                    new DefaultArtifact(artifact.getGroupId(),
                            artifact.getArtifactId(), artifact.getClassifier(),
                            artifact.getExtension(), version.toString(),
                            artifact.getProperties(), artifact.getFile());

            final ArtifactRequest alternateRequest =
                    new ArtifactRequest(versionedArtifact,
                            singletonList(fossRepository),
                            request.getRequestContext());

            alternateRequest.setTrace(request.getTrace());

            final ArtifactResult result =
                    artifactResolver.resolveArtifact(session, alternateRequest);

            if (result.getExceptions().isEmpty())
                return true;
        } catch (ArtifactResolutionException e) {
            throw new RuntimeException(e);
        }
        */

        try {
            final ArtifactDescriptorRequest alternateRequest =
                    new ArtifactDescriptorRequest(
                            request.getArtifact(),
                            singletonList(fossRepository),
                            request.getRequestContext())
                            .setTrace(request.getTrace());

            final ArtifactDescriptorResult result =
                    delegate.readArtifactDescriptor(session, alternateRequest);
            if (result.getExceptions().isEmpty()) {
                return true;
            }
        } catch (ArtifactDescriptorException e) {
            throw new RuntimeException(e);
        }

        return false;
    }

    public FossRepositorySystem setDefaultRepositorySystem(
            DefaultRepositorySystem delegate) {

        if (delegate == null) {
            throw new IllegalArgumentException(
                    "default repository system has not been specified");
        }

        this.delegate = delegate;
        return this;
    }

    public FossRepositorySystem setArtifactResolver(
            ArtifactResolver artifactResolver) {

        if (artifactResolver == null) {
            throw new IllegalArgumentException(
                    "artifact resolver has not been specified");
        }

        this.artifactResolver = artifactResolver;
        return this;
    }

    public FossRepositorySystem setUseJpp(boolean value) {
        this.useJpp = value;
        return this;
    }

    public FossRepositorySystem setLogger(Logger logger) {
        this.logger = (logger != null) ? logger : NullLogger.INSTANCE;
        return this;
    }

    public RemoteRepository getRemoteRepository() {
        return fossRepository;
    }

    public JPPLocalRepositoryManager getJppRepositoryManager() {
        return jppRepositoryManager;
    }

    @Override
    public VersionRangeResult resolveVersionRange(
            RepositorySystemSession session,
            VersionRangeRequest request)
            throws VersionRangeResolutionException {

        debugf("resolveVersionRange %s", request);
//        assertFedoraRepository(request.getRepositories());
        try {
            // try FOSS local repo
            final VersionRangeRequest alternateRequest =
                    new VersionRangeRequest(request.getArtifact(),
                            singletonList(fossRepository),
                            request.getRequestContext())
                            .setTrace(request.getTrace());

            final VersionRangeResult result =
                    delegate.resolveVersionRange(session, alternateRequest);
//            logger.warn("result = " + result.getVersions());

            final Version highestVersion = result.getHighestVersion();

            if (result.getExceptions().isEmpty()) {
                // Did we find something in the repo?
                // TODO: this is the best thing I could think off, what is the correct check?
                if (highestVersion != null
                        && result.getRepository(highestVersion) != null) {
                    return result;
                }
            }

            // the FOSS local repo might be running without metadata
            // TODO: how to check that?
            if (hasArtifact(session, request, highestVersion)) {
                result.setRepository(highestVersion, fossRepository);
                return result;
            }

            if (!useJpp) {
                return result;
            }

            // try JPP local repo
            // JPP will always return something regardless of the version, but
            // without a version readArtifactDescriptor will go into the
            // DefaultVersionResolver so we can't put in LATEST.
//            throw new RuntimeException("NYI");
            // TODO: what if highestVersion is null?
            result.setRepository(highestVersion,
                    jppRepositoryManager.getRepository());
            logger.warn("Could not resolve version range " + request +
                    ", using JPP " + result);
            return result;
        } catch (VersionRangeResolutionException e) {
            throw e;
        }
    }

    @Override
    public VersionResult resolveVersion(
            RepositorySystemSession session,
            VersionRequest request)
            throws VersionResolutionException {

        debugf("resolveVersion %s", request);
        try {
            // try FOSS repo
            final VersionRequest alternateRequest =
                    new VersionRequest(request.getArtifact(),
                            singletonList(fossRepository),
                            request.getRequestContext())
                            .setTrace(request.getTrace());

            final VersionResult result =
                    delegate.resolveVersion(session, alternateRequest);
            if (result.getExceptions().isEmpty()) {
                return result;
            }
        } catch (VersionResolutionException e) {
            throw e;
        }

        throw new RuntimeException(
                "NYI: org.fedoraproject.maven.repository.internal." +
                "FossRepositorySystem.resolveVersion");
    }

    @Override
    public ArtifactDescriptorResult readArtifactDescriptor(
            RepositorySystemSession session,
            ArtifactDescriptorRequest request)
            throws ArtifactDescriptorException {

        ArtifactDescriptorException originalException = null;
        final Artifact artifact = request.getArtifact();
        // try FOSS local repo
        {
            try {
                final ArtifactDescriptorRequest alternateRequest =
                        new ArtifactDescriptorRequest(request.getArtifact(),
                                singletonList(fossRepository),
                                request.getRequestContext())
                                .setTrace(request.getTrace());

                final ArtifactDescriptorResult result =
                        delegate.readArtifactDescriptor(session, alternateRequest);
                if (result.getExceptions().isEmpty()) {
                    return result;
                }
            } catch (ArtifactDescriptorException e) {
                originalException = e;
            }
        }
        // try FOSS local repo with LATEST
        if (!artifact.getVersion().equals(LATEST_VERSION))
        {
            try {
                final Artifact alternateArtifact =
                        artifact.setVersion(LATEST_VERSION);

                final ArtifactDescriptorRequest alternateRequest =
                        new ArtifactDescriptorRequest(alternateArtifact,
                                singletonList(fossRepository),
                                request.getRequestContext())
                                .setTrace(request.getTrace());

                final ArtifactDescriptorResult result =
                        delegate.readArtifactDescriptor(session, alternateRequest);
                if (result.getExceptions().isEmpty()) {
                    logger.warn("Could not find artifact descriptor "
                            + artifact + ", using LATEST "
                            + result.getArtifact());
                    return result;
                }
            } catch (ArtifactDescriptorException e) {
                logger.debug("LATEST resolution of " + artifact + " failed", e);
                if (originalException == null) {
                    originalException = e;
                }
            }
        }
        // try JPP local repo
        if (useJpp) {
            // use maven as much as possible
            final RepositorySystemSession alternateSession = openJpp(session);
            try {
                final ArtifactDescriptorRequest alternateRequest =
                        new ArtifactDescriptorRequest(request.getArtifact(),
                                singletonList(fossRepository),
                                request.getRequestContext());

                alternateRequest.setTrace(request.getTrace());

                final ArtifactDescriptorResult result =
                        delegate.readArtifactDescriptor(alternateSession,
                                alternateRequest);
//                logger.warn("result from JPP " + result);
                if (result.getExceptions().isEmpty()) {
                    // TODO: I may want to muck the result a bit to make sure JPP is also used for resolveArtifact
                    // JPP probably did not return the proper version, which
                    // makes the MavenPluginValidator barf
                    // lets muck it
//                    result.setArtifact(new JPPArtifact(result.getArtifact()));
                    logger.warn("Could not find artifact descriptor "
                            + artifact + ", using JPP " + result.getArtifact());
                    return result;
                }
            } catch (ArtifactDescriptorException e) {
                logger.debug("JPP resolution of " + artifact + " failed", e);
                if (originalException == null) {
                    originalException = e;
                }
            }
        }

        if (originalException != null) {
            throw originalException;
        }

        throw new RuntimeException(
                "NYI: org.fedoraproject.maven.repository.internal." +
                "FossRepositorySystem.readArtifactDescriptor");
    }

    @Override
    public CollectResult collectDependencies(
            RepositorySystemSession session,
            CollectRequest request)
            throws DependencyCollectionException {

        debugf("collectDependencies %s", request);
        validateSession(session);

        // delegate has been wired up to come back to us, so this must be a real
        // implementation

        DependencyCollectionException originalException = null;

        // TODO: work in progress, we need to aggregate all
        // try JPP local repo
        if (useJpp) {
            // use maven as much as possible
            final RepositorySystemSession alternateSession = openJpp(session);
            try {
                final CollectRequest alternateRequest =
                        new CollectRequest(request.getRoot(),
                                request.getDependencies(),
                                singletonList(fossRepository))
                                .setManagedDependencies(request.getManagedDependencies())
                                .setTrace(request.getTrace());

                final CollectResult result =
                        dependencyCollector.collectDependencies(
                                alternateSession, alternateRequest);

                if (result.getExceptions().isEmpty()
                        && result.getRoot() != null) {
                    logger.warn("collectDependencies: result from JPP " + result);
//                    logger.warn("TODO");
                    return result;
                }
            } catch (DependencyCollectionException e) {
                logger.debug("JPP collect dependencies of " + request +
                        " failed", e);
                originalException = e;
            }
        }

        // try FOSS local repo
        {
            try {
                final CollectRequest alternateRequest = new CollectRequest()
                        .setRoot(request.getRoot())
                        .setDependencies(request.getDependencies())
                        .setManagedDependencies(request.getManagedDependencies())
                        .setRequestContext(request.getRequestContext())
                        .setTrace(request.getTrace())
                        .setRepositories(singletonList(fossRepository));

                final CollectResult result =
                        dependencyCollector.collectDependencies(session,
                                alternateRequest);

                logger.warn("collectDependencies: result = " + result);
                if (result.getExceptions().isEmpty()) {
                    return result;
                }
            } catch (DependencyCollectionException e) {
                logger.warn("collect dependencies failed ", e);
                if (originalException == null) {
                    originalException = e;
                }
            }
        }

        if (originalException != null) {
            throw originalException;
        }

        throw new RuntimeException(
                "NYI: org.fedoraproject.maven.repository.internal." +
                "FossRepositorySystem.collectDependencies");
    }

    @Override
    public DependencyResult resolveDependencies(
            RepositorySystemSession session,
            DependencyRequest request)
            throws DependencyResolutionException {

        debugf("resolveDependencies %s", request.getCollectRequest());
        // TODO: assert that the request does not traverse repos
        return delegate.resolveDependencies(session, request);
//        throw new RuntimeException(
//                "NYI: org.fedoraproject.maven.repository.internal." +
//                "FossRepositorySystem.resolveDependencies");
    }

    @Override
    public List<ArtifactResult> resolveDependencies(
            RepositorySystemSession session,
            DependencyNode node,
            DependencyFilter filter)
            throws ArtifactResolutionException {

        throw new RuntimeException(
                "NYI: org.fedoraproject.maven.repository.internal." +
                "FossRepositorySystem.resolveDependencies");
    }

    @Override
    public List<ArtifactResult> resolveDependencies(
            RepositorySystemSession session,
            CollectRequest request,
            DependencyFilter filter)
            throws DependencyCollectionException, ArtifactResolutionException {

        throw new RuntimeException(
                "NYI: org.fedoraproject.maven.repository.internal." +
                "FossRepositorySystem.resolveDependencies");
    }

    @Override
    public ArtifactResult resolveArtifact(
            RepositorySystemSession session,
            ArtifactRequest request)
            throws ArtifactResolutionException {

        debugf("resolveArtifact %s", request);
        validateSession(session);

        // delegate has been wired up to come back to us, so this must be a real
        // implementation

        ArtifactResolutionException originalException = null;
        final Artifact artifact = request.getArtifact();

        // try FOSS local repo
        {
            try {
                final ArtifactRequest alternateRequest =
                        new ArtifactRequest(request.getArtifact(),
                                singletonList(fossRepository),
                                request.getRequestContext())
                                .setDependencyNode(request.getDependencyNode())
                                .setTrace(request.getTrace());

                final ArtifactResult result = artifactResolver.resolveArtifact(
                        session, alternateRequest);

                // A successful result can contain exceptions
                //if (result.getExceptions().isEmpty()) {
                    return result;
                //}
            } catch (ArtifactResolutionException e) {
                originalException = e;
            }
        }

        // try FOSS local repo with LATEST
        if (!artifact.getVersion().equals(LATEST_VERSION))
//            throw new IllegalStateException("NYI: LATEST should not appear " +
//                    "during resolveArtifact, should it?");
        {
            try {
                final Artifact alternateArtifact =
                        new DefaultArtifact(artifact.getGroupId(),
                                artifact.getArtifactId(), artifact.getClassifier(),
                                artifact.getExtension(), LATEST_VERSION,
                                artifact.getProperties(), artifact.getFile());

                final ArtifactRequest alternateRequest =
                        new ArtifactRequest(alternateArtifact,
                                Collections.singletonList(fossRepository),
                                request.getRequestContext())
                                .setDependencyNode(request.getDependencyNode())
                                .setTrace(request.getTrace());

                final ArtifactResult result = artifactResolver.resolveArtifact(
                        session, alternateRequest);

                // A successful result can contain exceptions
                //if (result.getExceptions().isEmpty()) {
                    logger.warn("Could not find artifact " + artifact +
                            ", using LATEST " + result.getArtifact());
                    return result;
                //}
            } catch (ArtifactResolutionException e) {
                logger.debug("LATEST resolution of " + artifact + " failed", e);
                if (originalException == null) {
                    originalException = e;
                }
            }
        }

        // try JPP local repo
        if (useJpp) {
            // use maven as much as possible
            final RepositorySystemSession alternateSession = openJpp(session);
            try {
                final ArtifactRequest alternateRequest =
                        new ArtifactRequest(request.getArtifact(),
                                singletonList(fossRepository),
                                request.getRequestContext())
                                .setDependencyNode(request.getDependencyNode())
                                .setTrace(request.getTrace());

                final ArtifactResult result = artifactResolver.resolveArtifact(
                        alternateSession, alternateRequest);

                // A successful result can contain exceptions
                //if (result.getExceptions().isEmpty()) {
                    logger.warn("Could not find artifact " + artifact + " in " +
                            fossRepository + ", using JPP " +
                            result.getArtifact());
                    return result;
                //}
            } catch (ArtifactResolutionException e) {
                logger.debug("JPP resolution of " + artifact + " failed", e);
                if (originalException == null) {
                    originalException = e;
                }
            }
        }

        if (originalException != null) {
            throw originalException;
        }

        throw new RuntimeException(
                "NYI: org.fedoraproject.maven.repository.internal." +
                "FossRepositorySystem.resolveArtifact");
    }

    @Override
    public List<ArtifactResult> resolveArtifacts(
            RepositorySystemSession session,
            Collection<? extends ArtifactRequest> requests)
            throws ArtifactResolutionException {

//        throw new RuntimeException(
//                "NYI: org.fedoraproject.maven.repository.internal." +
//                "FossRepositorySystem.resolveArtifacts");
        // Do this very straight forward instead of fast and complex
        final List<ArtifactResult> results = new ArrayList<ArtifactResult>(requests.size());
        for (ArtifactRequest request : requests) {
            results.add(resolveArtifact(session, request));
        }
        return results;
    }

    @Override
    public List<MetadataResult> resolveMetadata(
            RepositorySystemSession session,
            Collection<? extends MetadataRequest> requests) {

        // TODO: JPP does not support metadata, but the FOSS repo might
        logger.warn("resolveMetadata " + requests);
        //return delegate.resolveMetadata(session, requests);
        throw new RuntimeException(
                "NYI: org.fedoraproject.maven.repository.internal." +
                "FossRepositorySystem.resolveMetadata");
//        final List<MetadataResult> results =
//                new ArrayList<MetadataResult>(requests.size());
//        for (MetadataRequest request : requests) {
//            results.add(new MetadataResult(request));
//        }
//        return results;
    }

    @Override
    public InstallResult install(
            RepositorySystemSession session,
            InstallRequest request)
            throws InstallationException {

        throw new RuntimeException(
                "NYI: org.fedoraproject.maven.repository.internal." +
                "FossRepositorySystem.install");
    }

    @Override
    public DeployResult deploy(
            RepositorySystemSession session,
            DeployRequest request)
            throws DeploymentException {

        throw new RuntimeException(
                "NYI: org.fedoraproject.maven.repository.internal." +
                "FossRepositorySystem.deploy");
    }

    @Override
    public LocalRepositoryManager newLocalRepositoryManager(
            LocalRepository localRepository) {

        return delegate.newLocalRepositoryManager(localRepository);
    }

    @Override
    public SyncContext newSyncContext(
            RepositorySystemSession session,
            boolean shared) {

        throw new RuntimeException(
                "NYI: org.fedoraproject.maven.repository.internal." +
                "FossRepositorySystem.newSyncContext");
    }

    private RepositorySystemSession openJpp(
            final RepositorySystemSession current) {

        assert useJpp : "useJpp is not set";
        return new DefaultRepositorySystemSession(current)
                .setOffline(true)
//                .setWorkspaceReader(jppRepository);
                .setLocalRepositoryManager(jppRepositoryManager);
    }
}