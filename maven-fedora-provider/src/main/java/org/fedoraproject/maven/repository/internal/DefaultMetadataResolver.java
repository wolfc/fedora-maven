package org.fedoraproject.maven.repository.internal;

import java.util.Collection;
import java.util.List;

import org.codehaus.plexus.component.annotations.Component;
import org.sonatype.aether.RepositorySystemSession;
import org.sonatype.aether.impl.MetadataResolver;
import org.sonatype.aether.resolution.MetadataRequest;
import org.sonatype.aether.resolution.MetadataResult;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
@Component(role = MetadataResolver.class)
public class DefaultMetadataResolver implements MetadataResolver {
    @Override
    public List<MetadataResult> resolveMetadata(RepositorySystemSession session, Collection<? extends MetadataRequest> requests) {
        throw new RuntimeException("NYI: org.fedoraproject.maven.repository.internal.DefaultMetadataResolver.resolveMetadata");
    }
}
