package org.fedoraproject.maven.repository.internal;

import static java.util.Collections.singletonList;
import static org.apache.maven.artifact.Artifact.LATEST_VERSION;

import java.util.Collection;
import java.util.List;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.fedoraproject.maven.repository.jpp.JavadirWorkspaceReader;
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
import org.sonatype.aether.impl.internal.DefaultRepositorySystem;
import org.sonatype.aether.installation.InstallRequest;
import org.sonatype.aether.installation.InstallResult;
import org.sonatype.aether.installation.InstallationException;
import org.sonatype.aether.repository.LocalRepository;
import org.sonatype.aether.repository.LocalRepositoryManager;
import org.sonatype.aether.repository.MirrorSelector;
import org.sonatype.aether.repository.RemoteRepository;
import org.sonatype.aether.repository.WorkspaceReader;
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
@Component(role = RepositorySystem.class)
public class FedoraRepositorySystem implements RepositorySystem {
    @Requirement
    private Logger logger = NullLogger.INSTANCE;

    @Requirement
    private DefaultRepositorySystem delegate;

    private RemoteRepository fedoraRepository;
    private MirrorSelector mirrorSelector;

    private WorkspaceReader jppRepository = new JavadirWorkspaceReader();

    public FedoraRepositorySystem() {
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
        logger.warn("resolveVersionRange " + request);
        // try Fedora local repo
        assertFedoraRepository(request.getRepositories());
        final VersionRangeResult result = delegate.resolveVersionRange(session, request);
        logger.warn("result = " + result.getVersions());
        // Did we find something in the repo?
        // TODO: this is the best thing I could think off, what is the correct check?
        final Version highestVersion = result.getHighestVersion();
        if (highestVersion != null && result.getRepository(highestVersion) != null)
            return result;
        // try JPP local repo
        // JPP will always return something regardless of the version, but without
        // a version readArtifactDescriptor will go into the DefaultVersionResolver
        // so we can't put in LATEST.
        result.setRepository(highestVersion, jppRepository.getRepository());
        logger.warn("Could not resolve version range " + request + ", using JPP " + result);
        return result;
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
                    logger.warn("Could not find artifact " + artifact + ", using LATEST " + result.getArtifact());
                    return result;
                }
            } catch (ArtifactDescriptorException e) {
                logger.debug("LATEST resolution of " + artifact + " failed", e);
                if (originalException == null)
                    originalException = e;
            }
        }
        // try JPP local repo
        {
            // use maven as much as possible
            final RepositorySystemSession alternateSession = new DefaultRepositorySystemSession(session)
                    .setOffline(true)
                    .setWorkspaceReader(jppRepository);
            try {
                final ArtifactDescriptorRequest alternateRequest = new ArtifactDescriptorRequest(request.getArtifact(), singletonList(fedoraRepository), request.getRequestContext())
                        .setTrace(request.getTrace());
                final ArtifactDescriptorResult result = delegate.readArtifactDescriptor(alternateSession, alternateRequest);
                logger.warn("result from JPP " + result);
                if (result.getExceptions().isEmpty()) {
                    // TODO: I may want to muck the result a bit to make sure JPP is also used for resolveArtifact
                    logger.warn("Could not find artifact " + artifact + ", using JPP " + result.getArtifact());
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
        logger.warn("collectDependencies " + request);
        DependencyCollectionException originalException = null;
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
                final CollectResult result = delegate.collectDependencies(session, alternateRequest);
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
        logger.warn("resolveDependencies " + request.getCollectRequest());
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
        logger.warn("resolveArtifact " + request);
        ArtifactResolutionException originalException = null;
        final Artifact artifact = request.getArtifact();
        // try Fedora local repo
        {
            try {
                final ArtifactRequest alternateRequest = new ArtifactRequest(request.getArtifact(), singletonList(fedoraRepository), request.getRequestContext())
                        .setTrace(request.getTrace());
                final ArtifactResult result = delegate.resolveArtifact(session, alternateRequest);
                if (result.getExceptions().isEmpty())
                    return result;
            } catch (ArtifactResolutionException e) {
                originalException = e;
            }
        }
        // try Fedora local repo with LATEST
        if (artifact.getVersion().equals(LATEST_VERSION))
            throw new IllegalStateException("NYI: LATEST should not appear during resolveArtifact, should it?");
        /*
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
        */
        // try JPP local repo
        {
            // use maven as much as possible
            final RepositorySystemSession alternateSession = new DefaultRepositorySystemSession(session)
                    .setOffline(true)
                    .setWorkspaceReader(jppRepository);
            try {
                final ArtifactRequest alternateRequest = new ArtifactRequest(request.getArtifact(), singletonList(fedoraRepository), request.getRequestContext())
                        .setTrace(request.getTrace());
                final ArtifactResult result = delegate.resolveArtifact(alternateSession, alternateRequest);
                if (result.getExceptions().isEmpty()) {
                    logger.warn("Could not find artifact " + artifact + ", using JPP " + result.getArtifact());
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
        throw new RuntimeException("NYI: org.fedoraproject.maven.repository.internal.FedoraRepositorySystem.resolveArtifacts");
    }

    @Override
    public List<MetadataResult> resolveMetadata(RepositorySystemSession session, Collection<? extends MetadataRequest> requests) {
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
}