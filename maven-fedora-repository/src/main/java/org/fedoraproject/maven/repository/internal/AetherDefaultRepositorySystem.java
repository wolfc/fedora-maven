package org.fedoraproject.maven.repository.internal;

import org.codehaus.plexus.component.annotations.Component;
import org.sonatype.aether.impl.internal.DefaultRepositorySystem;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
@Component(role = DefaultRepositorySystem.class, hint = "default")
public class AetherDefaultRepositorySystem extends DefaultRepositorySystem {
}
