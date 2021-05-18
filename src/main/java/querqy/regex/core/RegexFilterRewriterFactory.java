package querqy.regex.core;

import querqy.model.Clause;
import querqy.model.ExpandedQuery;
import querqy.model.QuerqyQuery;
import querqy.model.StringRawQuery;
import querqy.rewrite.QueryRewriter;
import querqy.rewrite.RewriterFactory;
import querqy.rewrite.SearchEngineRequestAdapter;
import querqy.rewrite.commonrules.WhiteSpaceQuerqyParserFactory;

import java.util.regex.Pattern;

public class RegexFilterRewriterFactory extends RewriterFactory {

    protected static final char RAWQUERY = '*';

    /**
     * The pattern to match
     */
    private final Pattern pattern;

    /**
     * Iff true, we'd also match the pattern for query terms that were injected by previous rewriters.
     */
    private final boolean acceptGeneratedTerms;

    /**
     * The filter query to add if the pattern matches.
     */
    private final QuerqyQuery<?> filterQuery;

    /**
     * The regex is applied per query term.
     *
     * The filter query string can either be 1) just one or more terms, possibly preceded by a '-' to signal exclusion,
     * or, 2) a 'raw query' - a query in Solr or ES syntax, which must be preceded by '*' (e.g. '* category:cat_id' or
     * '* -price:[2000 TO *]' for Solr)
     *
     * @param rewriterId The rewriter Id
     * @param patternString The regex pattern
     * @param acceptGeneratedTerms Iff true, we also match terms that were added by previous rewriters
     * @param filterQueryString The filter query.
     */
    public RegexFilterRewriterFactory(final String rewriterId, final String patternString,
                                      final boolean acceptGeneratedTerms,
                                      final String filterQueryString) {
        super(rewriterId);
        this.filterQuery = parseFilterQueryString(filterQueryString);
        this.pattern = Pattern.compile(patternString);
        this.acceptGeneratedTerms = acceptGeneratedTerms;

    }

    @Override
    public QueryRewriter createRewriter(final ExpandedQuery input,
                                        final SearchEngineRequestAdapter searchEngineRequestAdapter) {
        // the rewriter is stateful - create a new instance per request
        return new RegexFilterRewriter(pattern, acceptGeneratedTerms, filterQuery);
    }

    /**
     * See {@link #RegexFilterRewriterFactory(String, String, boolean, String)}
     *
     * @param filterQueryString The filter query string
     * @return The filter query parsed into Querqy's object model
     */
    public static QuerqyQuery<?> parseFilterQueryString(final String filterQueryString) {
        String tmp = filterQueryString.trim();
        if (tmp.isEmpty()) {
            throw new IllegalArgumentException("Filter query must not be empty");
        }
        if (tmp.charAt(0) == RAWQUERY) {
            tmp = tmp.substring(1).trim();
            if (tmp.isEmpty()) {
                throw new IllegalArgumentException("Raw filter query must not be empty");
            }
            return new StringRawQuery(null, tmp, Clause.Occur.MUST, true);
        } else {
            final WhiteSpaceQuerqyParserFactory parserFactory = new WhiteSpaceQuerqyParserFactory();
            return parserFactory.createParser().parse(tmp);
        }
    }

}
