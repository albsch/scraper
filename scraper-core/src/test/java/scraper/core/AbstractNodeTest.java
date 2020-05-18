package scraper.core;

import org.junit.Assert;
import org.junit.Test;
import scraper.api.di.DIContainer;
import scraper.api.exceptions.NodeException;
import scraper.api.exceptions.ValidationException;
import scraper.api.flow.FlowMap;
import scraper.api.flow.impl.FlowMapImpl;
import scraper.api.node.container.NodeContainer;
import scraper.api.node.container.NodeLogLevel;
import scraper.api.node.type.Node;
import scraper.api.plugin.ScrapeSpecificationParser;
import scraper.api.specification.ScrapeInstance;
import scraper.api.specification.ScrapeSpecification;
import scraper.api.specification.impl.ScrapeInstaceImpl;
import scraper.util.DependencyInjectionUtil;
import scraper.utils.ClassUtil;

import java.io.IOException;
import java.lang.reflect.ReflectPermission;
import java.net.URL;
import java.nio.file.Path;
import java.security.Permission;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.*;
import static scraper.util.NodeUtil.addressOf;

@SuppressWarnings("rawtypes") // testing abstract node
public class AbstractNodeTest {

    private ScrapeInstaceImpl getInstance(String base, String file) throws IOException, ValidationException {
        URL baseurl = getClass().getResource(base);
        return InstanceHelper.getInstance(baseurl, file);
    }

    @Test
    public void simpleFunctionalNodeTest() throws Exception {
        ScrapeInstaceImpl instance = getInstance("abstract", "job2.jf");

        NodeContainer<? extends Node> node = opt(instance);
        Assert.assertTrue(node instanceof AbstractFunctionalNode);

        FlowMap o = FlowMapImpl.origin();
        o = node.getC().accept(node, o);

        assertTrue(o.get("simple").isPresent());
        assertEquals(true, o.get("simple").get());

        AbstractNode abstractNode = (AbstractNode) node;
        Assert.assertNotNull(abstractNode.getL());
        assertEquals(NodeLogLevel.INFO, abstractNode.getLogLevel());
        assertEquals("SimpleFunctionalNode", abstractNode.getType());
    }

    @Test
    public void globalNodeConfigurationsTest() throws Exception {
        ScrapeInstaceImpl instance = getInstance("abstract", "all-field.jf");

        NodeContainer<? extends Node> node = opt(instance);
        FlowMap o = FlowMapImpl.origin();
        o = node.getC().accept(node, o);

        assertTrue(o.get("simple").isEmpty());
        // local key overwrites all key
        assertTrue(o.get("overwritten").isPresent());
        assertTrue(o.get("goTo").isPresent());
        assertEquals(true, o.get("overwritten").get());
        assertEquals(true, o.get("goTo").get());
    }


    @Test
    public void getService() throws Exception {
        ScrapeInstaceImpl instance = getInstance("abstract", "dummy.jf");

        AbstractNode<? extends Node> node = (AbstractNode<? extends Node>) opt(instance);
        node.service = "newService";

        // service thread group is created only once
        assertEquals(node.getService(), node.getService());

        AtomicBoolean t = new AtomicBoolean(false);
        node.dispatch(() -> { t.set(true); return null; });
        Thread.sleep(50);
        Assert.assertTrue(t.get());
    }

    @Test(expected = ValidationException.class)
    public void badJsonDefaultTest() throws Exception {
        getInstance("abstract", "bad-node.jf");
    }

    @Test
    public void badLogTest() throws Exception {
        ScrapeInstaceImpl instance = getInstance("abstract", "bad-log.jf");

        NodeContainer<? extends Node> node = opt(instance);
        FlowMap o = FlowMapImpl.origin();
        // bad log should never stop the process
        ((AbstractNode<?>) node).start(node, o);
    }

    @Test
    public void fileTest() throws Exception {
        ScrapeInstaceImpl instance = getInstance("abstract", "file-node.jf");

        NodeContainer<? extends Node> node = opt(instance);
        FlowMap o = FlowMapImpl.origin();
        ((AbstractNode<?>) node).start(node, o);
    }

    @Test
    public void fileWithTemplateTest() throws Exception {
        ScrapeInstaceImpl instance = getInstance("abstract", "file-template.jf");

        NodeContainer<? extends Node> node = opt(instance);
        FlowMap o = FlowMapImpl.origin();
        o.output("path-template", "/tmp/scraper-ok");
        ((AbstractNode<?>) node).start(node, o);
    }

    @Test
    public void allLogLevelsTest() throws Exception {
        ScrapeInstaceImpl instance = getInstance("abstract", "log-levels.jf");

        NodeContainer<? extends Node> node = opt(instance);
        FlowMap o = FlowMapImpl.origin();

        //trace
        node.getC().accept(node, o);

        Assert.assertTrue(node.getKeySpec("logLevel").isPresent());
        Assert.assertEquals("TRACE", node.getKeySpec("logLevel").get());

    }


    @Test
    public void goodListGoToTest() throws Exception {
        getInstance("abstract", "good-list-goto.jf");
    }

    @Test
    public void goToNodeTest() throws Exception {
        ScrapeInstaceImpl instance = getInstance("abstract", "goto.jf");

        NodeContainer<? extends Node> node = opt(instance);
        FlowMap o = FlowMapImpl.origin();
        FlowMap o2 = node.forward(o);

        FlowMap o3 = node.eval(o2, addressOf("oor"));
        assertNotNull(o3.get("y"));
    }

    @Test
    public void impliedGoToTest() throws Exception {
        ScrapeInstaceImpl instance = getInstance("abstract", "implied-goto.jf");

        NodeContainer<? extends Node> node = opt(instance);
        FlowMap o = FlowMapImpl.origin();
        FlowMap o2 = node.forward(o);

        Assert.assertEquals(1, o2.size());
    }

    @Test
    public void functionalNodeWithGotoTest() throws Exception {
        ScrapeInstaceImpl instance = getInstance("abstract", "func-goto.jf");

        NodeContainer<? extends Node> node = opt(instance);
        FlowMap o = FlowMapImpl.origin();
        o = node.forward(o);

        assertTrue(o.get("simple").isPresent());
        assertEquals(true, o.get("simple").get());
    }

    // inject IllegalAccessException on reflection access to get code coverage
    @Test(expected = ValidationException.class)
    public void reflectionCodeCoverageTest() throws Exception {
        SecurityManager sm = System.getSecurityManager();
        System.setSecurityManager(new SecurityManager() {
            @Override
            public void checkPermission(Permission perm) {
                if (perm instanceof ReflectPermission && "suppressAccessChecks".equals(perm.getName())) {
                    for (StackTraceElement elem : Thread.currentThread().getStackTrace()) {
                        if ("scraper.util.NodeUtil".equals(elem.getClassName()) && "initField".equals(elem.getMethodName())) {
                            ClassUtil.sneakyThrow(new IllegalAccessException("Illegal Access!"));
                        }
                    }
                }
            }
        });

        try {
            getInstance("abstract", "func-goto.jf");
        } finally {
            System.setSecurityManager(sm);
        }
    }

    // inject IllegalAccessException on reflection access to get code coverage
    @Test(expected = RuntimeException.class)
    public void reflectionCodeCoverageTestStart() throws Exception {
        SecurityManager sm = System.getSecurityManager();

        try {
            ScrapeInstaceImpl instance = getInstance("abstract", "file-node.jf");

            System.setSecurityManager(new SecurityManager() {
                @Override
                public void checkPermission(Permission perm) {
                    if (perm instanceof ReflectPermission && "suppressAccessChecks".equals(perm.getName())) {
                        for (StackTraceElement elem : Thread.currentThread().getStackTrace()) {
                            if ("scraper.core.AbstractNode".equals(elem.getClassName()) && "start".equals(elem.getMethodName())) {
                                ClassUtil.sneakyThrow(new IllegalAccessException("Illegal Access!"));
                            }
                        }
                    }
                }
            });

            NodeContainer<? extends Node> node = opt(instance);
            FlowMap o = FlowMapImpl.origin();
            ((AbstractNode<?>) node).start(node, o);
        } finally {
            System.setSecurityManager(sm);
        }
    }

    @Test(expected = NodeException.class)
    public void badEnsureFileTest() throws Exception {
        ScrapeInstaceImpl instance = getInstance("abstract", "file-bad-node.jf");

        NodeContainer<? extends Node> node = opt(instance);
        FlowMap o = FlowMapImpl.origin();
        node.getC().accept(node, o);
    }

    @Test
    public void nullEnsureFileTest() throws Exception {
        ScrapeInstaceImpl instance = getInstance("abstract", "file-notwanted.jf");

        NodeContainer<? extends Node> node = opt(instance);
        FlowMap o = FlowMapImpl.origin();
        node.getC().accept(node, o);
    }

    @Test(expected = ValidationException.class)
    public void badNodeTest() throws Exception {
        getInstance("abstract", "bad-node-2.jf");
    }


    @Test
    public void indexAndLabelTest() throws Exception {
        ScrapeInstaceImpl instance = getInstance("addressing", "debug.yf");
        NodeContainer<? extends Node> node = opt(instance);
        Assert.assertEquals(addressOf("debug.start.startingnode"), node.getAddress());
    }

    @Test
    public void onlyIndexTest() throws Exception {
        ScrapeInstaceImpl instance = getInstance("addressing", "debug.yf");

        Optional<NodeContainer<? extends Node>> node = instance.getNode(addressOf("debug.start.1"));
        Assert.assertTrue(node.isPresent());
        Assert.assertEquals("<debug.start.1>", node.get().getAddress().toString());
    }

    @Test
    public void secondGraphMixedTest() throws Exception {
        ScrapeInstaceImpl instance = getInstance("addressing", "debug.yf");

        // address target -> address expected output
        Map.of("0", "<debug.testing.0>",
                "hellonode", "<debug.testing.hellonode:1>",
                "1", "<debug.testing.hellonode:1>",
                "2", "<debug.testing.2>"
        ).forEach((target, expected) -> {
            Assert.assertTrue(instance.getNode(addressOf("debug.testing."+target)).isPresent());
            Assert.assertEquals(expected, instance.getNode(addressOf("debug.testing."+target)).get().toString());
        });
    }

    @Test
    public void graphAddressTest() throws Exception {
        ScrapeInstaceImpl instance = getInstance("addressing", "debug.yf");
        // address target -> address expected output
        Map.of("testing", "<debug.testing.0>",
                "start", "<debug.start.startingnode:0>"
        ).forEach((target, expected) -> {
            Assert.assertTrue(instance.getNode(addressOf("debug."+target)).isPresent());
            Assert.assertEquals(expected, instance.getNode(addressOf("debug."+target)).get().toString());
        });
    }


    @Test
    public void instanceAddressTest() throws Exception {
        ScrapeInstaceImpl instance = getInstance("addressing", "debug.yf");
        // address target -> address expected output
        Map.of("debug", "<debug.start.startingnode:0>" )
                .forEach((target, expected) -> {
            Assert.assertTrue(instance.getNode(addressOf(target)).isPresent());
            Assert.assertEquals(expected, instance.getNode(addressOf(target)).get().toString());
        });
    }

    @Test(expected = ValidationException.class)
    public void tooManyKeysTest() throws Exception {
        getInstance("abstract", "field-missing-only-warning.jf");
    }

    @Test(expected = ValidationException.class)
    public void badGoToTest() throws Exception {
        getInstance("abstract", "bad-goto.jf");
    }

    @Test(expected = ValidationException.class)
    public void badForwardTest() throws Exception {
        getInstance("abstract", "bad-forward.jf");
    }

    @Test(expected = ValidationException.class)
    public void badListGoToTest() throws Exception {
        getInstance("abstract", "bad-list-goto.jf");
    }

    @Test(expected = ValidationException.class)
    public void badFieldTest() throws Exception {
        getInstance("abstract", "bad-field.jf");
    }

    private NodeContainer<? extends Node> opt(ScrapeInstance i) {
        Optional<NodeContainer<? extends Node>> e = i.getEntry();
        Assert.assertTrue(e.isPresent());
        return e.get();
    }
}