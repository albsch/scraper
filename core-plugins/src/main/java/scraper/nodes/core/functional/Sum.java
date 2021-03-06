package scraper.nodes.core.functional;

import scraper.annotations.NotNull;
import scraper.annotations.node.FlowKey;
import scraper.annotations.node.NodePlugin;
import scraper.api.flow.FlowMap;
import scraper.api.node.container.FunctionalNodeContainer;
import scraper.api.node.type.FunctionalNode;
import scraper.api.template.L;
import scraper.api.template.T;

/**
 * Sum two integers
 */
@NodePlugin("0.0.1")
public class Sum implements FunctionalNode {

    /** Operand 1 */
    @FlowKey(mandatory = true)
    private final T<Integer> integer = new T<>(){};

    /** Operand 2 */
    @FlowKey(mandatory = true)
    private final T<Integer> integer2 = new T<>(){};

    /** Integer Output */
    @FlowKey(mandatory = true)
    private final L<Integer> result = new L<>(){};

    @Override
    public void modify(@NotNull FunctionalNodeContainer n, @NotNull final FlowMap o) {
        o.output(result, o.eval(integer) + o.eval(integer2));
    }
}
