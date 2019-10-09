package scraper.api.specification.impl;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import scraper.api.node.NodeAddress;
import scraper.api.node.impl.NodeAddressImpl;

import java.io.IOException;

public class NodeAddressDeserializer extends StdDeserializer<NodeAddress> {
 
    public NodeAddressDeserializer() {
        this(null); 
    } 
 
    public NodeAddressDeserializer(Class<?> vc) {
        super(vc); 
    }
 
    @Override
    public NodeAddress deserialize(JsonParser jp, DeserializationContext ctxt)
      throws IOException {
        JsonNode node = jp.getCodec().readTree(jp);

        if ((node.getNodeType().compareTo(JsonNodeType.STRING) == 0)) {
            return new NodeAddressImpl(node.asText());
        } else {
            throw new IOException("Expected string, got " + node.getNodeType());
        }
    }
}