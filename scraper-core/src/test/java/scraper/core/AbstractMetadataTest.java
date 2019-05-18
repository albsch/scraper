package scraper.core;

import org.junit.Assert;
import org.junit.Test;
import scraper.api.node.Node;

public class AbstractMetadataTest {
    @Test
    public void simpleData() {
        AbstractMetadata data = new AbstractMetadata("node", "1.0.2", "nodecat") {
            @Override public Node getNode() { return null; }
        };

        AbstractMetadata dataa = new AbstractMetadata("node", "1.2.2", "nodecat") {
            @Override public Node getNode() { return null; }
        };

        AbstractMetadata data2 = new AbstractMetadata("node", "2.0.2", "nodecat") {
            @Override public Node getNode() { return null; }
        };

        Assert.assertEquals("nodecat", data.getCategory());

        Assert.assertFalse(data.backwardsCompatible(data2));
        Assert.assertFalse(data2.backwardsCompatible(data));

        Assert.assertFalse(data.backwardsCompatible(dataa));
        Assert.assertTrue(dataa.backwardsCompatible(data));
    }

}