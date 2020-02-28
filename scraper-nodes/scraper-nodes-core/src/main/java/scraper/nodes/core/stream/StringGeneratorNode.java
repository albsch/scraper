package scraper.nodes.core.stream;

import scraper.annotations.NotNull;
import scraper.annotations.node.FlowKey;
import scraper.annotations.node.NodePlugin;
import scraper.api.exceptions.NodeException;
import scraper.api.flow.FlowMap;
import scraper.api.node.container.StreamNodeContainer;
import scraper.api.node.type.StreamNode;
import scraper.api.template.L;
import scraper.api.template.T;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Generate strings based on a generator string and a map which specifies expressions to apply to T parts.
 *
 * Currently only one key expression is supported. A map with size greater than 1 will throw a RuntimeException.
 *
 * Currently, only the 'TO' expression is implemented.
 *
 * Example:
 *
 *  generator: "page={range}"
 *
 *  keysToExpressions: {"range": "2 TO 300"}
 *
 * will generate a list [{"generated": "page=2", "range": "2"},  ... ,{"generated": "page=300", "range": "300"}]
 * with generated strings and the expression used and the current value.
 *
 * @author Albert Schimpf
 */
@NodePlugin("0.10.0")
public final class StringGeneratorNode implements StreamNode {

    /** String used to generate a list of more strings */
    @FlowKey(mandatory = true) @NotNull
    private final T<String> generator = new T<>(){};

    /** Target key in T to (String) expression map.
     * Expression currently supported: "KEY X TO Y", where X and Y are Integers.
     */
    @FlowKey(mandatory = true)
    private String expression;

    /** Output list key */
    @FlowKey(defaultValue = "\"generated\"") @NotNull
    private L<String> generatedElement = new L<>(){};

    @Override
    public void process(@NotNull StreamNodeContainer n, @NotNull FlowMap o) throws NodeException {
        // parse expression for goTo key
        // only one pattern 'X TO Y' supported, parse directly with regex
        String targetString = expression;
        Pattern p = Pattern.compile("(\\w*)\\s(\\d*) TO (\\d*)", Pattern.DOTALL);
        Matcher m = p.matcher(targetString);

        if(!m.find()) throw new UnsupportedOperationException("Other expressions than 'KEY X TO Y' are not supported: " + targetString);

        String key = String.valueOf(m.group(1));
        int from = Integer.parseInt(m.group(2));
        int to = Integer.parseInt(m.group(3));

        n.collect(o, List.of(String.valueOf(generatedElement.getTerm().getRaw()), key));

        for (int i = from; i <= to; i++) {
            FlowMap copy = o.copy();
            // used to evaluate generator T
            copy.output(key, i);

            String generatedString = copy.eval(generator);
            copy.output(generatedElement, generatedString);

            n.streamFlowMap(o, copy);
        }
    }
}
