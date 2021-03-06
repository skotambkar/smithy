/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.smithy.protocoltests.traits;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.OperationIndex;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.NodeValidationVisitor;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.model.validation.node.TimestampValidationStrategy;

abstract class ProtocolTestCaseValidator<T extends Trait> extends AbstractValidator {

    private final Class<T> traitClass;
    private final ShapeId traitId;
    private final String descriptor;

    ProtocolTestCaseValidator(ShapeId traitId, Class<T> traitClass, String descriptor) {
        this.traitId = traitId;
        this.traitClass = traitClass;
        this.descriptor = descriptor;
    }

    @Override
    public List<ValidationEvent> validate(Model model) {
        OperationIndex operationIndex = OperationIndex.of(model);

        return Stream.concat(model.shapes(OperationShape.class), model.shapes(StructureShape.class))
                .flatMap(operation -> Trait.flatMapStream(operation, traitClass))
                .flatMap(pair -> validateOperation(model, operationIndex, pair.left, pair.right).stream())
                .collect(Collectors.toList());
    }

    abstract StructureShape getStructure(Shape shape, OperationIndex operationIndex);

    abstract List<? extends HttpMessageTestCase> getTestCases(T trait);

    boolean isValidatedBy(Shape shape) {
        return shape instanceof OperationShape;
    }

    private List<ValidationEvent> validateOperation(
            Model model,
            OperationIndex operationIndex,
            Shape shape,
            T trait
    ) {
        List<ValidationEvent> events = new ArrayList<>();
        List<? extends HttpMessageTestCase> testCases = getTestCases(trait);

        for (int i = 0; i < testCases.size(); i++) {
            HttpMessageTestCase testCase = testCases.get(i);
            StructureShape struct = getStructure(shape, operationIndex);
            if (struct != null) {
                NodeValidationVisitor validator = createVisitor(testCase.getParams(), model, shape, i);
                events.addAll(struct.accept(validator));
            } else if (!testCase.getParams().isEmpty() && isValidatedBy(shape)) {
                events.add(error(shape, trait, String.format(
                        "Protocol test %s parameters provided for operation with no %s: `%s`",
                        descriptor, descriptor, Node.printJson(testCase.getParams()))));
            }
        }

        return events;
    }

    private NodeValidationVisitor createVisitor(ObjectNode value, Model model, Shape shape, int position) {
        return NodeValidationVisitor.builder()
                .model(model)
                .eventShapeId(shape.getId())
                .value(value)
                .startingContext(traitId + "." + position + ".params")
                .eventId(getName())
                .timestampValidationStrategy(TimestampValidationStrategy.EPOCH_SECONDS)
                .allowBoxedNull(true)
                .build();
    }
}
