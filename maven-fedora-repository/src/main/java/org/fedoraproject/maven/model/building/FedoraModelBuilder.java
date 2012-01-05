package org.fedoraproject.maven.model.building;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.building.DefaultModelBuilder;
import org.apache.maven.model.building.ModelBuilder;
import org.apache.maven.model.building.ModelBuildingException;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.apache.maven.model.building.ModelBuildingResult;
import org.apache.maven.model.validation.ModelValidator;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.sonatype.aether.spi.log.Logger;
import org.sonatype.aether.spi.log.NullLogger;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
@Component(role = ModelBuilder.class)
public class FedoraModelBuilder implements ModelBuilder {
    @Requirement
    private Logger logger = NullLogger.INSTANCE;

    @Requirement
    private DefaultModelBuilder delegate;

    @Requirement(hint = "default")
    private ModelValidator defaultModelValidator;

    @Override
    public ModelBuildingResult build(ModelBuildingRequest request) throws ModelBuildingException {
        // The JPP repository contains broken poms which do not properly specify dependency versions.
        // This is a hack to counter such problems.
        final boolean onePhase;
        if (request.isTwoPhaseBuilding()) {
            // just perform phase 1
            return delegate.build(request);
        }
        final ModelBuildingResult result = delegate.build(request);
        return fixUp(request, result);
    }

    @Override
    public ModelBuildingResult build(ModelBuildingRequest request, ModelBuildingResult result) throws ModelBuildingException {
        result = delegate.build(request, result);
        return fixUp(request, result);
    }

    protected ModelBuildingResult fixUp(ModelBuildingRequest request, ModelBuildingResult result) {
        if (!isFromJPP(request, result))
            return result;
        for (Dependency dependency : result.getEffectiveModel().getDependencies()) {
            if (dependency.getVersion() == null) {
                // just to get us past a missing dependency validation error set it to "" and cross your fingers
                logger.warn("No version specified for " + dependency + " in " + request.getModelSource().getLocation() + ", defaulting to none");
                dependency.setVersion("");
            }
        }
        Model resultModel = result.getEffectiveModel();

        DefaultModelProblemCollector problems = new DefaultModelProblemCollector( result.getProblems() );
        problems.setSource( resultModel );
        problems.setRootModel( resultModel );
        defaultModelValidator.validateEffectiveModel(resultModel,  request, problems);
        return result;
    }

    private boolean isFromJPP(ModelBuildingRequest request, ModelBuildingResult result) {
        // TODO: how to implement this?
        return true;
    }
}
