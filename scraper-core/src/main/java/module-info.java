import scraper.core.AbstractMetadata;
import scraper.api.node.Node;

open module scraper.core {
    uses AbstractMetadata;
    uses scraper.api.plugin.Addon;
    uses Node;

    exports scraper.core;
    exports scraper.core.template;
    exports scraper.util;
    exports scraper.api.flow.impl;
    exports scraper.api.specification.impl;

    requires scraper.annotations;
    requires scraper.api;
    requires scraper.utils;

    requires jackson.databind;
    requires jackson.annotations;
    requires spring.core;
    requires io.github.classgraph;
    requires spring.plugin.core;
    requires spring.plugin.metadata;
    requires com.google.common;
    requires java.net.http;

    requires org.slf4j;

    requires com.fasterxml.jackson.dataformat.yaml;
    requires antlr4.runtime;
}