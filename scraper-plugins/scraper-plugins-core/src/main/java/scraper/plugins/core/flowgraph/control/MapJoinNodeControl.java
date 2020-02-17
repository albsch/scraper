package scraper.plugins.core.flowgraph.control;


import scraper.annotations.NotNull;
import scraper.api.node.Node;
import scraper.api.specification.ScrapeInstance;
import scraper.plugins.core.flowgraph.api.ControlFlowEdge;
import scraper.plugins.core.flowgraph.api.Version;

import java.util.List;

/**
 *
 */
public final class MapJoinNodeControl {
    @Version("0.1.0") @NotNull
    public static List<ControlFlowEdge> getOutput(List<ControlFlowEdge> ignore, Node node, ScrapeInstance spec) {
        // TODO implement
        throw new IllegalStateException("Not implemented");
    }
}

// old api version reference
//
//    public List<ControlFlowEdge> getOutput() {
//        ControlFlowEdge e = new ControlFlowEdgeImpl(getAddress(), getJobPojo().getNode(NodeUtil.addressOf(mapTarget)).getAddress(),"mapJoin", true, true);
//        return Stream.concat(super.getOutput().stream(), List.of(e).stream()).collect(Collectors.toList());
//    }
