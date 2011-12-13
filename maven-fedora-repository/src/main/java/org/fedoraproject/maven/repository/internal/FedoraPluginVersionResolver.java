package org.fedoraproject.maven.repository.internal;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.version.PluginVersionRequest;
import org.apache.maven.plugin.version.PluginVersionResolutionException;
import org.apache.maven.plugin.version.PluginVersionResolver;
import org.apache.maven.plugin.version.PluginVersionResult;
import org.apache.maven.plugin.version.internal.DefaultPluginVersionResolver;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.sonatype.aether.repository.ArtifactRepository;
import org.sonatype.aether.spi.log.Logger;
import org.sonatype.aether.spi.log.NullLogger;

/**
 * The RepositorySystem must return a File in Metadata, which is not available
 * when using the JPP repository. So we need to bypass resolving plugins earlier.
 *
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
@Component(role = PluginVersionResolver.class)
public class FedoraPluginVersionResolver implements PluginVersionResolver {
    @Requirement
    private Logger logger = NullLogger.INSTANCE;

    @Requirement
    private DefaultPluginVersionResolver delegate;

    @Override
    public PluginVersionResult resolve(PluginVersionRequest request) throws PluginVersionResolutionException {
        //final PluginVersionRequest alternate = new DefaultPluginVersionRequest(plugin(request), request.getRepositorySession(), remotes()).setPom(request.getPom());
        logger.warn("No version specified for plugin " + request.getGroupId() + ":" + request.getArtifactId() + ", falling back to RELEASE");
        //final PluginVersionResult result = delegate.resolve(request);
        //logger.warn("Would have been " + result.getVersion() + " from " + result.getRepository());
        return new PluginVersionResult() {
            @Override
            public String getVersion() {
                return Artifact.RELEASE_VERSION;
            }

            @Override
            public ArtifactRepository getRepository() {
                throw new RuntimeException("NYI: .getRepository");
            }
        };
    }

    public FedoraPluginVersionResolver setDefaultPluginVersionResolver(DefaultPluginVersionResolver delegate) {
        if (delegate == null) {
            throw new IllegalArgumentException("default plugin version resolver has not been specified");
        }
        this.delegate = delegate;
        return this;
    }

    public FedoraPluginVersionResolver setLogger(Logger logger) {
        this.logger = (logger != null) ? logger : NullLogger.INSTANCE;
        return this;
    }
}
