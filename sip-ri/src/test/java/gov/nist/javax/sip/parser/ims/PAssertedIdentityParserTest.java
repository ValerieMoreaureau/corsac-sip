
/*
* Conditions Of Use
*
* This software was developed by employees of the National Institute of
* Standards and Technology (NIST), and others.
* This software is has been contributed to the public domain.
* As a result, a formal license is not needed to use the software.
*
* This software is provided "AS IS."
* NIST MAKES NO WARRANTY OF ANY KIND, EXPRESS, IMPLIED
* OR STATUTORY, INCLUDING, WITHOUT LIMITATION, THE IMPLIED WARRANTY OF
* MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, NON-INFRINGEMENT
* AND DATA ACCURACY.  NIST does not warrant or make any representations
* regarding the use of the software or the results thereof, including but
* not limited to the correctness, accuracy, reliability or usefulness of
* the software.
*
*
*/
/************************************************************************************************
 * PRODUCT OF PT INOVACAO - EST DEPARTMENT and Telecommunications Institute (Aveiro, Portugal)  *
 ************************************************************************************************/

package gov.nist.javax.sip.parser.ims;

import gov.nist.javax.sip.parser.HeaderParser;
import gov.nist.javax.sip.parser.ParserTestCase;

public class PAssertedIdentityParserTest extends ParserTestCase
{
    public void testParser() {
        // TODO Auto-generated method stub

        String[] preferredID =  {
         "P-Asserted-Identity: <sip:bob@atlanta.com>\n",

         "P-Asserted-Identity: <sip:alice@atlanta.com>, <tel:+1-201-555-0123>\n",
         "P-Asserted-Identity: <sip:alice@atlanta.com;user=phone>, <tel:+1-201-555-0123;user=tel>\n",
         "P-Asserted-Identity:sip:33296112233@provider.com;user=phone\n",
         "P-Asserted-Identity:sip:33296112233@provider.com;user=phone,tel:+1-201-555-0123;user=tel\n"

        };

        super.testParser(PAssertedIdentityParser.class,preferredID);
    }
    
    public void testInvalidHeaderParamOnMultiValues() {
    	
        String preferredID =  "P-Asserted-Identity: <sip:alice@atlanta.com>, <tel:+1-201-555-0123>;user=phone\n\n";
        
	    try {  
           HeaderParser hp = createParser(PAssertedIdentityParser.class, preferredID);
           hp.parse();
           fail("Header:"+ preferredID + " cannot be parse, wrong in pattern");
	       
	    } catch (java.text.ParseException ex) {
	       assertTrue(ex.getMessage().contains("This header doesn't allow parameters"));     
	    } 
        
    }

}
