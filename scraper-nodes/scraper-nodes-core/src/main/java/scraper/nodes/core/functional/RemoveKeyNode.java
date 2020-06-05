package scraper.nodes.core.functional;

import scraper.annotations.NotNull;
import scraper.annotations.node.FlowKey;
import scraper.annotations.node.NodePlugin;
import scraper.api.flow.FlowMap;
import scraper.api.node.container.FunctionalNodeContainer;
import scraper.api.node.type.FunctionalNode;
import scraper.api.template.L;

/**
 * Can remove a key of the current flow map.
 * <p>
 * Example
 * <pre>
 * type: RemoveKeyNode
 * remove: mykey
 * </pre>
 */
@NodePlugin("1.0.0")
public class RemoveKeyNode implements FunctionalNode {

    /** Removes a single key */
    @FlowKey(mandatory = true)
    private final L<Void> remove = new L<>(){};

    @Override
    public void modify(@NotNull FunctionalNodeContainer n, @NotNull final FlowMap o) {
        String remove = this.remove.getLocation().eval(o);
        // remove key
        o.remove(remove);
    }
}
