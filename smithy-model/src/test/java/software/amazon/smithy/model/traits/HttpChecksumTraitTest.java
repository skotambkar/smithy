/*
 * Copyright 2021 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.smithy.model.traits;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInRelativeOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.HttpChecksumProperties.Location;

public class HttpChecksumTraitTest {

    @Test
    public void loadsTrait() {
        TraitFactory provider = TraitFactory.createServiceFactory();
        ObjectNode requestNode = Node.objectNode()
                .withMember("location", Location.HEADER.toNode())
                .withMember("prefix", Node.from("x-checksum-"))
                .withMember("algorithms", Node.fromStrings("crc32", "crc32c"));

        ObjectNode responseNode = Node.objectNode()
                .withMember("location", Location.HEADER.toNode())
                .withMember("prefix", Node.from("x-checksum-"))
                .withMember("algorithms", Node.fromStrings("crc32", "crc32c"));

        ObjectNode node = Node.objectNode()
                .withMember("request", requestNode)
                .withMember("response", responseNode);

        Optional<Trait> trait = provider.createTrait(
                ShapeId.from("smithy.api#httpChecksum"), ShapeId.from("ns.qux#foo"), node);
        assertTrue(trait.isPresent());
        assertThat(trait.get(), instanceOf(HttpChecksumTrait.class));
        HttpChecksumTrait checksumTrait = (HttpChecksumTrait) trait.get();

        assertThat(checksumTrait.getRequestProperty().getLocation(), equalTo(Location.HEADER));
        assertThat(checksumTrait.getRequestProperty().getPrefix(), equalTo("x-checksum-"));
        assertThat(checksumTrait.getRequestProperty().getAlgorithms(), containsInRelativeOrder(
                "crc32", "crc32c"
        ));
        assertThat(checksumTrait.getResponseProperty().getLocation(), equalTo(Location.HEADER));
        assertThat(checksumTrait.getResponseProperty().getPrefix(), equalTo("x-checksum-"));
        assertThat(checksumTrait.getResponseProperty().getAlgorithms(), containsInRelativeOrder(
                "crc32", "crc32c"
        ));

        assertThat(checksumTrait.getRequestProperty().toNode(), equalTo(requestNode));
        assertThat(checksumTrait.getResponseProperty().toNode(), equalTo(responseNode));
        assertThat(checksumTrait.toNode(), equalTo(node));
        assertThat(checksumTrait.toBuilder().build(), equalTo(checksumTrait));
    }
}
