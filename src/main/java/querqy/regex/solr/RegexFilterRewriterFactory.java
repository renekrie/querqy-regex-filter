package querqy.regex.solr;

import querqy.rewrite.RewriterFactory;
import querqy.solr.SolrRewriterFactoryAdapter;
import querqy.solr.utils.ConfigUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * RegexFilterRewriter factory for Solr.
 *
 * Example JSON (remember to escape pattern strings for JSON:
 * <pre>
 *     {
 *          "class":"querqy.regex.solr.RegexFilterRewriterFactory",
 *          "config":{
 *              "filter":"* cat:lumber",
 *              "regex":"\\d+[Xx-]\\d+[Xx-]\\d+"
 *          }
 *     }
 * </pre>
 *
 * See {@link RegexFilterConfigRequestBuilder} for Java client support.
 *
 */
public class RegexFilterRewriterFactory extends SolrRewriterFactoryAdapter {

    public static final String CONF_REGEX_PATTERN = "regex";
    public static final String CONF_FILTER_QUERY = "filter";
    public static final String CONF_ACCEPT_GENERATED_TERMS = "acceptGeneratedTerms";

    querqy.regex.core.RegexFilterRewriterFactory rewriterFactory = null;

    public RegexFilterRewriterFactory(final String rewriterId) {
        super(rewriterId);
    }

    @Override
    public void configure(final Map<String, Object> config) {
        rewriterFactory = new querqy.regex.core.RegexFilterRewriterFactory(rewriterId,
                ConfigUtils.getStringArg(config, CONF_REGEX_PATTERN)
                        .orElseThrow(() -> new IllegalArgumentException("Missing " + CONF_REGEX_PATTERN)),
                ConfigUtils.getBoolArg(config, CONF_ACCEPT_GENERATED_TERMS, false),
                ConfigUtils.getStringArg(config, CONF_FILTER_QUERY)
                        .orElseThrow(() -> new IllegalArgumentException("Missing " + CONF_FILTER_QUERY))
        );


    }

    @Override
    public List<String> validateConfiguration(final Map<String, Object> config) {
        final List<String> errors = new ArrayList<>();
        final String patternString = ConfigUtils.getStringArg(config, CONF_REGEX_PATTERN, null);
        if (patternString == null) {
            errors.add("Pattern expected in config property: " + CONF_REGEX_PATTERN);
        } else {
            final String pattern = patternString.trim();
            if (pattern.isEmpty()) {
                errors.add("config property '" + CONF_REGEX_PATTERN + "' must not be empty");
            } else {
                try {
                    Pattern.compile(patternString.trim());
                } catch (final PatternSyntaxException e) {
                    errors.add("Invalid RegEx in config property: " + CONF_REGEX_PATTERN);
                }
            }
        }


        final String filterQueryString = ConfigUtils.getStringArg(config, CONF_FILTER_QUERY, null);
        if (filterQueryString == null) {
            errors.add("Query expected in config property: " + CONF_FILTER_QUERY);
        } else {
            try {
                querqy.regex.core.RegexFilterRewriterFactory.parseFilterQueryString(filterQueryString);
            } catch (final Exception e) {
                errors.add("Illegal filter query in '" + CONF_FILTER_QUERY + "': " + e.getMessage());
            }
        }

        try {
            ConfigUtils.getBoolArg(config, CONF_ACCEPT_GENERATED_TERMS, true);
        } catch (final Exception e) {
            errors.add("Could not read boolean property: " + CONF_ACCEPT_GENERATED_TERMS);
        }

        return errors;
    }

    @Override
    public RewriterFactory getRewriterFactory() {
        return rewriterFactory;
    }
}
