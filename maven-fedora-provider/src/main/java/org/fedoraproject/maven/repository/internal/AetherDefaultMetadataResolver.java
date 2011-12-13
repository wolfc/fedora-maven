package org.fedoraproject.maven.repository.internal;

import org.codehaus.plexus.component.annotations.Component;
import org.sonatype.aether.impl.internal.DefaultMetadataResolver;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
@Component(role = DefaultMetadataResolver.class, hint = "default")
public class AetherDefaultMetadataResolver extends DefaultMetadataResolver {
}
