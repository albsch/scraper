package scraper.test;

import junitparams.JUnitParamsRunner;
import org.junit.runner.RunWith;
import scraper.addons.PutNode;
import scraper.addons.SimplestNode;
import scraper.nodes.test.annotations.Functional;
import scraper.nodes.test.helper.FunctionalTest;

import java.util.Map;

@RunWith(JUnitParamsRunner.class)
public class SimplestFunctionalTest extends FunctionalTest {
    @Functional(SimplestNode.class)
    public Object[] dummyTest() {
        return new Object[]{
                // node configuration
                Map.of(),
                // input
                Map.of("keyToBeDeleted", "notnull"),
                // output
                Map.of()
        };
    }

    @Functional(PutNode.class)
    public Object[] putTest() {
        return new Object[]{
                // node configuration
                Map.of("toPut", "here"),
                // input
                Map.of("ok", "notnull"),
                // output
                Map.of("hello", "world", "here", "result")
        };
    }
}

