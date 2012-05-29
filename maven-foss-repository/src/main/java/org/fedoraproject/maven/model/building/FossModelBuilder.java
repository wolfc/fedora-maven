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
public class FossModelBuilder implements ModelBuilder {
    @Requirement
    private Logger logger = NullLogger.INSTANCE;

    @Requirement
    private DefaultModelBuilder delegate;

    @Requirement(hint = "default")
    private ModelValidator defaultModelValidator;

    @Override
    public ModelBuildingResult build(ModelBuildingRequest request)
            throws ModelBuildingException {
        // The JPP repository contains broken poms which do not properly specify
        // dependency versions. This is a hack to counter such problems.
        final boolean onePhase;
        if (request.isTwoPhaseBuilding()) {
            // just perform phase 1
            return delegate.build(request);
        }
        final ModelBuildingResult result = delegate.build(request);
        return fixUp(request, result);
    }

    @Override
    public ModelBuildingResult build(
            ModelBuildingRequest request,
            ModelBuildingResult result)
            throws ModelBuildingException {

        result = delegate.build(request, result);
        return fixUp(request, result);
    }

    protected ModelBuildingResult fixUp(
            ModelBuildingRequest request,
            ModelBuildingResult result) {

        if (!isFromJPP(request, result))
            return result;

        for (Dependency dependency : result.getEffectiveModel().getDependencies()) {
            if (dependency.getVersion() == null) {
                // just to get us past a missing dependency validation error set
                // it to "" and cross your fingers
                logger.warn("No version specified for " + dependency + " in "
                        + request.getModelSource().getLocation() +
                        ", defaulting to none");
                dependency.setVersion("");
            }
        }
        Model resultModel = result.getEffectiveModel();

        final FossModelProblemCollector problems =
                new FossModelProblemCollector(result);
        defaultModelValidator.validateEffectiveModel(resultModel, request, problems);

        return result;
    }

    private boolean isFromJPP(
            ModelBuildingRequest request,
            ModelBuildingResult result) {

        // TODO: how to implement this?
        return true;
    }
}
