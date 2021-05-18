package querqy.regex.core;

import querqy.model.BooleanClause;
import querqy.model.BooleanQuery;
import querqy.model.DisjunctionMaxClause;
import querqy.model.DisjunctionMaxQuery;
import querqy.model.ExpandedQuery;
import querqy.model.Node;
import querqy.model.QuerqyQuery;
import querqy.model.Query;
import querqy.model.Term;
import querqy.rewrite.AbstractLoggingRewriter;
import querqy.rewrite.ContextAwareQueryRewriter;
import querqy.rewrite.SearchEngineRequestAdapter;

import java.util.Set;
import java.util.regex.Pattern;

public class RegexFilterRewriter extends AbstractLoggingRewriter implements ContextAwareQueryRewriter {

    /**
     * The pattern to match
     */
    private final Pattern pattern;

    /**
     * Iff true, we'd also match the pattern for query terms that were injected by previous rewriters.
     */
    private final boolean acceptGeneratedTerms;
    /**
     * The filter query to add if the pattern matches
     */
    private final QuerqyQuery<?> filterQuery;

    private boolean matchFound = false;

    // used for info logging
    private Set<String> infoLogMessages;
    private SearchEngineRequestAdapter searchEngineRequestAdapter;

    public RegexFilterRewriter(final Pattern pattern, final boolean acceptGeneratedTerms,
                               final QuerqyQuery<?> filterQuery) {
        this.pattern = pattern;
        this.acceptGeneratedTerms = acceptGeneratedTerms;
        this.filterQuery = filterQuery;
    }

    @Override
    public ExpandedQuery rewrite(final ExpandedQuery query, final SearchEngineRequestAdapter searchEngineRequestAdapter,
                                 final Set<String> infoLogMessages) {

        final QuerqyQuery<?> userQuery = query.getUserQuery();
        if (userQuery instanceof Query) {

            // reset the status flag (should already be false anyway)
            matchFound = false;

            // logging support
            this.infoLogMessages = infoLogMessages;
            this.searchEngineRequestAdapter = searchEngineRequestAdapter;

            // traverse the query tree
            visit((Query) userQuery);

            if (matchFound) {
                // add the filter query if we found the match
                query.addFilterQuery(filterQuery);

            }
        }
        return query;
    }

    @Override
    public Node visit(final Term term) {

        final boolean isTermMatch =
                // deal with generated terms (e.g. synonyms that were injected by previous rewriters

                // if the term wasn't generated or if we inject the filter for generated terms too...
                (((!term.isGenerated()) || acceptGeneratedTerms)
                        // and if the pattern matches -> set matchFound = true
                        && pattern.matcher(term.getValue()).matches());

        if (isTermMatch) {

            // set status
            matchFound = true;

            // and append log message
            if (isInfoLogging(searchEngineRequestAdapter)) {
                infoLogMessages.add(term.getValue().toString());
            }

        }

        return super.visit(term);
    }

    @Override
    public ExpandedQuery rewrite(final ExpandedQuery query) {
        throw new UnsupportedOperationException("This rewriter needs a query context");
    }

    // ************************************************************************
    // What follows below is just a performance optimisation: we override some methods that handle the query tree
    // traversal as we can abort the traversal if we've already matched the pattern


    @Override
    public Node visit(final DisjunctionMaxQuery disjunctionMaxQuery) {

        if (matchFound) {
            return null;
        }

        for (final DisjunctionMaxClause clause : disjunctionMaxQuery.getClauses()) {
            clause.accept(this);
            if (matchFound) {
                return null;
            }
        }

        return null;
    }

    @Override
    public Node visit(final BooleanQuery booleanQuery) {

        if (matchFound) {
            return null;
        }

        for (final BooleanClause clause : booleanQuery.getClauses()) {
            clause.accept(this);
            if (matchFound) {
                return null;
            }
        }

        return null;

    }
}
