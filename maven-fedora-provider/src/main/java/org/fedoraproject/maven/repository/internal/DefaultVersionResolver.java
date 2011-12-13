package org.fedoraproject.maven.repository.internal;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.sonatype.aether.RepositorySystemSession;
import org.sonatype.aether.impl.VersionResolver;
import org.sonatype.aether.resolution.VersionRequest;
import org.sonatype.aether.resolution.VersionResolutionException;
import org.sonatype.aether.resolution.VersionResult;
import org.sonatype.aether.spi.locator.Service;
import org.sonatype.aether.spi.locator.ServiceLocator;
import org.sonatype.aether.spi.log.Logger;
import org.sonatype.aether.spi.log.NullLogger;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
@Component(role = VersionResolver.class)
public class DefaultVersionResolver implements VersionResolver, Service {
    @Requirement
    private Logger logger = NullLogger.INSTANCE;

    @Requirement
    private org.apache.maven.repository.internal.DefaultVersionResolver delegate;

    @Override
    public void initService(ServiceLocator locator) {
        setLogger(locator.getService(Logger.class));
    }

    @Override
    public VersionResult resolveVersion(RepositorySystemSession session, VersionRequest request) throws VersionResolutionException {
        throw new RuntimeException("NYI: org.fedoraproject.maven.repository.internal.DefaultVersionResolver.resolveVersion");
    }

    public DefaultVersionResolver setDefaultVersionResolver(org.apache.maven.repository.internal.DefaultVersionResolver delegate) {
        if (delegate == null) {
            throw new IllegalArgumentException("default version resolver has not been specified");
        }
        this.delegate = delegate;
        return this;
    }

    public DefaultVersionResolver setLogger(Logger logger) {
        this.logger = (logger != null) ? logger : NullLogger.INSTANCE;
        return this;
    }
}
