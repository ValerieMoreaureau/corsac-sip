package gov.nist.javax.sip.parser.extensions;


import gov.nist.javax.sip.parser.ParserTestCase;

public class MinSEParserTest extends ParserTestCase {

    public void testParser() {

        String to[] =
        {   "Min-SE: 30\n",
            "Min-SE: 45;some-param=somevalue\n",
        };

        super.testParser(MinSEParser.class,to);

    }

}
