package scraper.nodes.core.functional;

import scraper.annotations.NotNull;
import scraper.annotations.node.Argument;
import scraper.annotations.node.FlowKey;
import scraper.annotations.node.NodePlugin;
import scraper.api.flow.FlowMap;
import scraper.api.node.container.FunctionalNodeContainer;
import scraper.api.node.type.FunctionalNode;
import scraper.api.reflect.T;

import java.util.List;
import java.util.Map;

/**
 * Can modify the current argument map.
 */
@NodePlugin("1.1.0")
public class EchoNode implements FunctionalNode {

    /** Multiple put operations can be specified in this map at once */
    @FlowKey(defaultValue = "{}") @NotNull
    private final T<Map<String, Object>> puts = new T<>(){};

    /** All keys specified in this list will be removed from the FlowMap */
    @FlowKey(defaultValue = "[]") @Argument
    private List<String> remove;

    @Override
    public void modify(@NotNull FunctionalNodeContainer n, @NotNull final FlowMap o) {
        Map<String, Object> puts = o.eval(this.puts);

        // put multiple objects/strings
        for (String key : puts.keySet()) {
            o.put(key, puts.get(key));
        }

        // remove keys
        for (String key : remove) {
            o.remove(key);
        }
    }
}
