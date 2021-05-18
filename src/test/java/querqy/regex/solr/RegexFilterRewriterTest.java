package querqy.regex.solr;

import static querqy.solr.QuerqyQParserPlugin.PARAM_REWRITERS;
import static querqy.solr.QuerqyRewriterRequestHandler.ActionParam.SAVE;

import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.params.DisMaxParams;
import org.apache.solr.common.util.ContentStreamBase;
import org.apache.solr.core.SolrCore;
import org.apache.solr.request.LocalSolrQueryRequest;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.request.SolrRequestHandler;
import org.apache.solr.request.SolrRequestInfo;
import org.apache.solr.response.SolrQueryResponse;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import querqy.solr.QuerqyDismaxParams;

import java.util.Arrays;
import java.util.Collections;

@SolrTestCaseJ4.SuppressSSL
public class RegexFilterRewriterTest extends SolrTestCaseJ4 {


    @BeforeClass
    public static void beforeTests() throws Exception {
        initCore("solrconfig.xml", "schema.xml");
        withRegexFilterRewriter("regex_general", "\\d+[Xx-]\\d+[Xx-]\\d+", "* cat:lumber", null);
        withRegexFilterRewriter("regex_screw", "[Mm]\\d+[Xx-]\\d+[Xx-]\\d+", "* cat:screws", null);

    }

    public void index() {

        assertU(adoc("id", "1", "f1", "a b c", "cat", "lumber"));
        assertU(adoc("id", "2", "f1", "a k l", "cat", "screws"));
        assertU(adoc("id", "3", "f1", "a x y"));
        SolrInputDocument doc = new SolrInputDocument();
        doc.setField("id", "4");
        doc.setField("f1", "a g h");
        doc.setField("cat", Arrays.asList("screws", "lumber"));
        assertU(adoc(doc));
        assertU(commit());
    }

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        clearIndex();
        index();
    }

    @Test
    public void testThatQueryWithoutPatternReturnsAllDocs() {
        try (final SolrQueryRequest req = req("q", "a",
                DisMaxParams.QF, "f1",
                "defType", "querqy",
                "debugQuery", "true",
                PARAM_REWRITERS, "regex_general,regex_screw"
        )) {
            assertQ("All docs",
                    req,
                    "//result[@name='response' and @numFound='4']"
            );
        }
    }

    @Test
    public void testSinglePatternMatch() {
        try (final SolrQueryRequest req = req("q", "a 12x7x9",
                DisMaxParams.QF, "f1",
                DisMaxParams.MM, "1", // ignore the pattern in matching
                "defType", "querqy",
                "debugQuery", "true",
                PARAM_REWRITERS, "regex_general,regex_screw"
        )) {
            assertQ("1 category filter",
                    req,
                    "//result[@name='response' and @numFound='2']",
                    "//str[@name='id'][text()='1']",
                    "//str[@name='id'][text()='4']"
            );
        }

        try (final SolrQueryRequest req = req("q", "a M8x50x3",
                DisMaxParams.QF, "f1",
                DisMaxParams.MM, "1", // ignore the pattern in matching
                "defType", "querqy",
                "debugQuery", "true",
                PARAM_REWRITERS, "regex_general,regex_screw"
        )) {
            assertQ("1 category filter",
                    req,
                    "//result[@name='response' and @numFound='2']",
                    "//str[@name='id'][text()='2']",
                    "//str[@name='id'][text()='4']"
            );
        }
    }

    @Test
    public void testTwoPatternMatch() {
        try (final SolrQueryRequest req = req("q", "a 12x7x9 M8x50x3",
                DisMaxParams.QF, "f1",
                DisMaxParams.MM, "1", // ignore the pattern in matching
                "defType", "querqy",
                "debugQuery", "true",
                PARAM_REWRITERS, "regex_general,regex_screw"
        )) {
            assertQ("2 category filter",
                    req,
                    "//result[@name='response' and @numFound='1']",
                    "//str[@name='id'][text()='4']"
            );
        }

    }

    @Test
    public void testInfoLogging() {
        try (final SolrQueryRequest req = req("q", "a M8x50x3",
                DisMaxParams.QF, "f1",
                DisMaxParams.MM, "1", // ignore the pattern in matching
                QuerqyDismaxParams.INFO_LOGGING, "on", // turn on info logging
                "defType", "querqy",
                "debugQuery", "true",
                PARAM_REWRITERS, "regex_general,regex_screw"
        )) {
            assertQ("Info logging",
                    req,
                    "//result[@name='response' and @numFound='2']",
                    "//lst[@name='querqy.infoLog']/arr[@name='regex_screw']/lst/arr[@name='APPLIED_RULES']/" +
                            "str[text()='M8x50x3']"
            );
        }
    }


    static void withRegexFilterRewriter(final String rewriterId, final String pattern, final String filter,
                                        final Boolean acceptGeneratedTerms) {
        final RegexFilterConfigRequestBuilder builder = new RegexFilterConfigRequestBuilder()
                .pattern(pattern)
                .filter(filter);
        if (acceptGeneratedTerms != null) {
            builder.acceptGeneratedTerms(acceptGeneratedTerms);
        }

        SolrCore core = h.getCore();
        SolrRequestHandler handler = core.getRequestHandler("/querqy/rewriter/" + rewriterId);

        final LocalSolrQueryRequest req = new LocalSolrQueryRequest(core, SAVE.params());
        // System.out.println(builder.buildJson());
        req.setContentStreams(Collections.singletonList(new ContentStreamBase.StringStream(builder.buildJson())));
        req.getContext().put("httpMethod", "POST");

        final SolrQueryResponse rsp = new SolrQueryResponse();
        SolrRequestInfo.setRequestInfo(new SolrRequestInfo(req, rsp));
        try {
            core.execute(handler, req, rsp);
        } finally {
            SolrRequestInfo.clearRequestInfo();
            req.close();
        }

    }

}