package org.fedoraproject.maven.model.validation;

import org.apache.maven.model.Model;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.apache.maven.model.building.ModelProblemCollector;
import org.apache.maven.model.validation.ModelValidator;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;

/**
 * This class is purely in place to skip effective model validation.
 * JPP contains invalid poms, so we want to skip the effective model validation, fix-up
 * the model and then validate.
 *
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
@Component(role = ModelValidator.class, hint = "skip-effective")
public class SkipEffectiveModelValidator implements ModelValidator {
    @Requirement(hint = "default")
    private ModelValidator defaultModelValidator;

    @Override
    public void validateRawModel(Model model, ModelBuildingRequest request, ModelProblemCollector problems) {
        defaultModelValidator.validateRawModel(model, request, problems);
    }

    @Override
    public void validateEffectiveModel(Model model, ModelBuildingRequest request, ModelProblemCollector problems) {
        // noop
    }
}
