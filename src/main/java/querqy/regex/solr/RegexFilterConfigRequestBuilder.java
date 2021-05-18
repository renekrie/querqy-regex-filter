package querqy.regex.solr;


import querqy.solr.RewriterConfigRequestBuilder;
import querqy.solr.SolrRewriterFactoryAdapter;

import java.util.HashMap;
import java.util.Map;

/**
 * Solr Java client support for configuring {@link querqy.regex.core.RegexFilterRewriter}s.
 */
public class RegexFilterConfigRequestBuilder extends RewriterConfigRequestBuilder {

    private String patternString;
    private String filterQueryString;
    private Boolean acceptGeneratedTerms;

    public RegexFilterConfigRequestBuilder() {
        super(RegexFilterRewriterFactory.class);
    }

    public RegexFilterConfigRequestBuilder pattern(final String pattern) {
        if (pattern == null || pattern.trim().isEmpty()) {
            throw new IllegalArgumentException("Pattern must not be empty");
        }
        this.patternString = pattern;
        return this;
    }

    public RegexFilterConfigRequestBuilder filter(final String filter) {
        if (filter == null || filter.trim().isEmpty()) {
            throw new IllegalArgumentException("Filter must not be empty");
        }
        this.filterQueryString = filter;
        return this;
    }

    public RegexFilterConfigRequestBuilder acceptGeneratedTerms(final boolean accept) {
        this.acceptGeneratedTerms = accept;
        return this;
    }

    @Override
    public Map<String, Object> buildConfig() {
        if (patternString == null) {
            throw new IllegalStateException("Pattern required");
        }
        if (filterQueryString == null) {
            throw new IllegalStateException("Filter required");
        }

        final Map<String, Object> config = new HashMap<>();
        config.put(RegexFilterRewriterFactory.CONF_REGEX_PATTERN, patternString);
        config.put(RegexFilterRewriterFactory.CONF_FILTER_QUERY, filterQueryString);
        if (acceptGeneratedTerms != null) {
            config.put(RegexFilterRewriterFactory.CONF_ACCEPT_GENERATED_TERMS, acceptGeneratedTerms);
        }
        return config;
    }
}
