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

import java.io.File;
import java.util.List;

import org.apache.maven.model.InputLocation;
import org.apache.maven.model.Model;
import org.apache.maven.model.building.DefaultModelProblem;
import org.apache.maven.model.building.ModelBuildingResult;
import org.apache.maven.model.building.ModelProblem;
import org.apache.maven.model.building.ModelProblemCollector;
import org.apache.maven.model.io.ModelParseException;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
public class FossModelProblemCollector implements ModelProblemCollector {
    private final List<ModelProblem> problems;
    private final String modelId;
    private final String source;

    public FossModelProblemCollector(final ModelBuildingResult result) {
        final Model model = result.getEffectiveModel();
        this.problems = result.getProblems();
        this.modelId = toId(model);
        this.source = toPath(model);
    }

    private void add(final ModelProblem problem) {
        problems.add(problem);
    }

    @Override
    public void add(
            final ModelProblem.Severity severity,
            final String message,
            final InputLocation location,
            final Exception cause) {

        int line = -1;
        int column = -1;
        String source = null;
        String modelId = null;

        if (location != null) {
            line = location.getLineNumber();
            column = location.getColumnNumber();
            if (location.getSource() != null) {
                modelId = location.getSource().getModelId();
                source = location.getSource().getLocation();
            }
        }

        if (modelId == null) {
            modelId = getModelId();
            source = getSource();
        }

        if (line <= 0 && column <= 0 && cause instanceof ModelParseException) {
            ModelParseException e = (ModelParseException) cause;
            line = e.getLineNumber();
            column = e.getColumnNumber();
        }

        ModelProblem problem =
                new DefaultModelProblem(message, severity, source, line, column,
                        modelId, cause);

        add(problem);
    }

    private String getModelId() {
        return modelId;
    }

    private String getSource() {
        return source;
    }

    private static String toId(Model model) {
        if (model == null)
            return "";

        String groupId = model.getGroupId();
        if (groupId == null && model.getParent() != null) {
            groupId = model.getParent().getGroupId();
        }

        final String artifactId = model.getArtifactId();

        String version = model.getVersion();
        if (version == null && model.getParent() != null) {
            version = model.getParent().getVersion();
        }

        return toId(groupId, artifactId, version);
    }

    /**
     * Creates a user-friendly artifact id from the specified coordinates.
     *
     * @param groupId    The group id, may be {@code null}.
     * @param artifactId The artifact id, may be {@code null}.
     * @param version    The version, may be {@code null}.
     * @return The user-friendly artifact id, never {@code null}.
     */
    private static String toId(
            String groupId,
            String artifactId,
            String version) {

        StringBuilder buffer = new StringBuilder(96);

        buffer.append((groupId != null && groupId.length() > 0)
                ? groupId
                : "[unknown-group-id]");
        buffer.append(':');
        buffer.append((artifactId != null && artifactId.length() > 0)
                ? artifactId
                : "[unknown-artifact-id]");
        buffer.append(':');
        buffer.append((version != null && version.length() > 0)
                ? version
                : "[unknown-version]");

        return buffer.toString();
    }

    private static String toPath(final Model model) {
        if (model != null) {
            File pomFile = model.getPomFile();

            if (pomFile != null) {
                return pomFile.getAbsolutePath();
            }
        }

        return "";
    }

}
