package scraper.addons;

import scraper.annotations.NotNull;
import scraper.annotations.node.NodePlugin;
import scraper.api.flow.FlowMap;
import scraper.api.node.container.NodeContainer;
import scraper.api.node.type.Node;

@NodePlugin(value = "0.1.0", deprecated = true)
public final class ComplexFlowNode implements Node {
    @NotNull @Override public FlowMap process(NodeContainer<? extends Node> n, @NotNull FlowMap o) { return o; }
}