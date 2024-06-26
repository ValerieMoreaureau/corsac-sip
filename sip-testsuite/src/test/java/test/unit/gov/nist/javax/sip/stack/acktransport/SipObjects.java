package test.unit.gov.nist.javax.sip.stack.acktransport;

import gov.nist.javax.sip.stack.transports.processors.netty.NettyMessageProcessorFactory;
import gov.nist.javax.sip.stack.transports.processors.nio.NioMessageProcessorFactory;

import java.util.Properties;

import javax.sip.SipException;
import javax.sip.SipFactory;
import javax.sip.SipStack;
import javax.sip.address.AddressFactory;
import javax.sip.header.HeaderFactory;
import javax.sip.message.MessageFactory;

// import org.sipfoundry.commons.log4j.SipFoundryAppender;
// import org.sipfoundry.commons.log4j.SipFoundryLayout;
// import org.sipfoundry.commons.log4j.SipFoundryLogRecordFactory;

public class SipObjects {
    SipStack sipStack;
    HeaderFactory headerFactory;
    AddressFactory addressFactory;
    MessageFactory messageFactory;

    public SipObjects(int myPort, String stackName, String autoDialogSupport) {
        SipFactory sipFactory = SipFactory.getInstance();
        sipFactory.resetFactory();
        sipFactory.setPathName("gov.nist");
        Properties properties = new Properties();
        String stackname = stackName + myPort;
        properties.setProperty("javax.sip.STACK_NAME", stackname);

        // The following properties are specific to nist-sip
        // and are not necessarily part of any other jain-sip
        // implementation.

        properties.setProperty("javax.sip.AUTOMATIC_DIALOG_SUPPORT", autoDialogSupport);

       /* properties.setProperty("gov.nist.javax.sip.LOG_FACTORY", SipFoundryLogRecordFactory.class
                .getName()); */

        // Set to 0 in your production code for max speed.
        // You need 16 for logging traces. 32 for debug + traces.
        // Your code will limp at 32 but it is best for debugging.

        properties.setProperty("gov.nist.javax.sip.TRACE_LEVEL", "DEBUG");
        String logFile = "target/logs/" + stackname + ".txt";
        properties.setProperty("gov.nist.javax.sip.MAX_FORK_TIME_SECONDS", "12");

        properties.setProperty("gov.nist.javax.sip.DEBUG_LOG", logFile);
        if(System.getProperty("enableNIO") != null && System.getProperty("enableNIO").equalsIgnoreCase("true")) {
        	properties.setProperty("gov.nist.javax.sip.MESSAGE_PROCESSOR_FACTORY", NioMessageProcessorFactory.class.getName());
        }
        if(System.getProperty("enableNetty") != null && System.getProperty("enableNetty").equalsIgnoreCase("true")) {
        	properties.setProperty("gov.nist.javax.sip.MESSAGE_PROCESSOR_FACTORY", NettyMessageProcessorFactory.class.getName());
        }
        // Testing DialogTimeout

        try {
            // Create SipStack object
            sipStack = sipFactory.createSipStack(properties);
            System.out.println("createSipStack " + sipStack);
        } catch (Exception e) {
            // could not find
            // gov.nist.jain.protocol.ip.sip.SipStackImpl
            // in the classpath
            e.printStackTrace();
            System.err.println(e.getMessage());
            throw new RuntimeException("Stack failed to initialize");
        }

        try {
            headerFactory = sipFactory.createHeaderFactory();
            addressFactory = sipFactory.createAddressFactory();
            messageFactory = sipFactory.createMessageFactory();
        } catch (SipException ex) {
            ex.printStackTrace();
            throw new RuntimeException(ex);
        }
    }
}
