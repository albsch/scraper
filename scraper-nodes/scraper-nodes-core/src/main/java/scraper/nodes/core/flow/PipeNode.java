package scraper.nodes.core.flow;


import scraper.annotations.NotNull;
import scraper.annotations.node.FlowKey;
import scraper.annotations.node.NodePlugin;
import scraper.api.exceptions.NodeException;
import scraper.api.flow.FlowMap;
import scraper.api.node.Address;
import scraper.api.node.container.NodeContainer;
import scraper.api.node.type.Node;
import scraper.api.reflect.T;

import java.util.List;

/**
 * Pipe to goTo nodes and continue
 * @author Albert Schimpf
 */
@NodePlugin("1.0.0")
public final class PipeNode implements Node {

    /** List of goTo labels */
    @FlowKey(mandatory = true)
    private T<List<Address>> pipeTargets = new T<>(){};

    @NotNull
    @Override
    public FlowMap process(NodeContainer<? extends Node> n, @NotNull FlowMap o) throws NodeException {
        FlowMap output = o;

        for (Address label : o.evalIdentity(pipeTargets)) {
            output = n.eval(output, label);
        }

        return n.forward(output);
    }
}
