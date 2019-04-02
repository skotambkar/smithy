package software.amazon.smithy.aws.traits.apigateway;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;

import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.TraitFactory;

public class AuthorizersTraitTest {
    @Test
    public void registersTrait() {
        TraitFactory factory = TraitFactory.createServiceFactory();
        var id = ShapeId.from("smithy.example#Foo");
        var node = Node.objectNodeBuilder()
                .withMember("aws.v4", Node.objectNodeBuilder()
                        .withMember("clientType", "awsSigV4")
                        .withMember("type", "request")
                        .withMember("uri", "arn:foo:baz")
                        .withMember("credentials", "arn:foo:bar")
                        .withMember("identitySource", "mapping.expression")
                        .withMember("identityValidationExpression", "[A-Z]+")
                        .withMember("resultTtlInSeconds", 100)
                        .build())
                .build();
        var trait = factory.createTrait(AuthorizersTrait.NAME, id, node).get();

        assertThat(trait, instanceOf(AuthorizersTrait.class));
        assertThat(factory.createTrait(AuthorizersTrait.NAME, id, trait.toNode()).get(), equalTo(trait));
    }
}