package test.unit.gov.nist.javax.sip.stack;


import gov.nist.javax.sip.stack.transports.processors.netty.NettyMessageProcessorFactory;
import gov.nist.javax.sip.stack.transports.processors.nio.NioMessageProcessorFactory;

import java.text.ParseException;
import java.util.Properties;
import java.util.Random;

import javax.sip.ClientTransaction;
import javax.sip.DialogTerminatedEvent;
import javax.sip.IOExceptionEvent;
import javax.sip.InvalidArgumentException;
import javax.sip.ListeningPoint;
import javax.sip.RequestEvent;
import javax.sip.ResponseEvent;
import javax.sip.ServerTransaction;
import javax.sip.SipException;
import javax.sip.SipFactory;
import javax.sip.SipListener;
import javax.sip.SipProvider;
import javax.sip.SipStack;
import javax.sip.TimeoutEvent;
import javax.sip.TransactionTerminatedEvent;
import javax.sip.address.Address;
import javax.sip.address.AddressFactory;
import javax.sip.address.SipURI;
import javax.sip.header.HeaderFactory;
import javax.sip.header.ToHeader;
import javax.sip.header.ViaHeader;
import javax.sip.message.MessageFactory;
import javax.sip.message.Request;
import javax.sip.message.Response;

import junit.framework.TestCase;
import test.tck.msgflow.callflows.AssertUntil;
import test.tck.msgflow.callflows.NetworkPortAssigner;
import test.tck.msgflow.callflows.TestAssertion;

public class NoAutoDialogTest extends TestCase {

    protected boolean testFail = false;

    protected StringBuffer errorBuffer = new StringBuffer();

    protected int myPort;

    protected int remotePort = 5060;

    protected String testProtocol = "udp";

    protected Address remoteAddress;

    protected SipURI requestUri;

    protected MessageFactory messageFactory = null;

    protected HeaderFactory headerFactory = null;

    protected AddressFactory addressFactory = null;

    protected static String host = null;

    protected Client client;

    protected Server server;

    public final int SERVER_PORT = NetworkPortAssigner.retrieveNextPort();

    public final int CLIENT_PORT = NetworkPortAssigner.retrieveNextPort();

    public NoAutoDialogTest() {

    }

    protected void doFail(String message) {
        testFail = true;
        errorBuffer.append(message + "\n");

    }

    public boolean isFailed() {
        return testFail;
    }

    public String generateFromTag() {
        return Integer.valueOf(Math.abs(new Random().nextInt())).toString();
    }

    @Override
    protected void setUp() throws Exception {

        this.client = new Client();
        this.server = new Server();
        Thread.sleep(500);
    }

    @Override
    protected void tearDown() throws Exception {

       this.client.stop();
       this.server.stop();

    }

    protected ViaHeader getLocalVia(SipProvider _provider) throws ParseException,
            InvalidArgumentException {
        return headerFactory.createViaHeader(_provider.getListeningPoint(testProtocol)
                .getIPAddress(), _provider.getListeningPoint(testProtocol).getPort(), _provider
                .getListeningPoint(testProtocol).getTransport(), null);

    }


    public String doMessage(Throwable t) {
        StringBuffer sb = new StringBuffer();
        int tick = 0;
        Throwable e = t;
        do {
            StackTraceElement[] trace = e.getStackTrace();
            if (tick++ == 0)
                sb.append(e.getClass().getCanonicalName() + ":" + e.getLocalizedMessage() + "\n");
            else
                sb.append("Caused by: " + e.getClass().getCanonicalName() + ":"
                        + e.getLocalizedMessage() + "\n");

            for (StackTraceElement ste : trace)
                sb.append("\t" + ste + "\n");

            e = e.getCause();
        } while (e != null);

        return sb.toString();

    }

    public class Server implements SipListener {
        protected SipStack sipStack;

        protected SipFactory sipFactory = null;

        protected SipProvider provider = null;

        private ServerTransaction inviteStx;

        private Request inviteRequest;

        
        public Server() {
            try {
                final Properties defaultProperties = new Properties();
                host = "127.0.0.1";
                defaultProperties.setProperty("javax.sip.AUTOMATIC_DIALOG_SUPPORT", "off");
                defaultProperties.setProperty("javax.sip.STACK_NAME", "server");
                defaultProperties.setProperty("gov.nist.javax.sip.TRACE_LEVEL", "DEBUG");
                defaultProperties.setProperty("gov.nist.javax.sip.DEBUG_LOG", "target/logs/server_debug_NoAutoDialogTest.txt");
                defaultProperties.setProperty("gov.nist.javax.sip.SERVER_LOG", "server_log_NoAutoDialogTest.txt");
                defaultProperties.setProperty("gov.nist.javax.sip.READ_TIMEOUT", "1000");
                defaultProperties.setProperty("gov.nist.javax.sip.CACHE_SERVER_CONNECTIONS",
                        "false");
                if(System.getProperty("enableNIO") != null && System.getProperty("enableNIO").equalsIgnoreCase("true")) {
                	defaultProperties.setProperty("gov.nist.javax.sip.MESSAGE_PROCESSOR_FACTORY", NioMessageProcessorFactory.class.getName());
                }
                if(System.getProperty("enableNetty") != null && System.getProperty("enableNetty").equalsIgnoreCase("true")) {                    
                    defaultProperties.setProperty("gov.nist.javax.sip.MESSAGE_PROCESSOR_FACTORY", NettyMessageProcessorFactory.class.getName());
                }
                this.sipFactory = SipFactory.getInstance();
                this.sipFactory.resetFactory();
                this.sipFactory.setPathName("gov.nist");
                this.sipStack = this.sipFactory.createSipStack(defaultProperties);
                this.sipStack.start();
                ListeningPoint lp = this.sipStack.createListeningPoint(host, SERVER_PORT, testProtocol);
                this.provider = this.sipStack.createSipProvider(lp);
                headerFactory = this.sipFactory.createHeaderFactory();
                messageFactory = this.sipFactory.createMessageFactory();
                addressFactory = this.sipFactory.createAddressFactory();
                this.provider.addSipListener(this);
            } catch (Exception e) {
                e.printStackTrace();
                fail("unexpected exception ");
            }

        }



        public void stop() {
        	Utils.stopSipStack(this.sipStack);
        }

        public void processDialogTerminated(DialogTerminatedEvent dte) {
            fail("Unexpected Dialog Terminated Event");
        }

        public void processIOException(IOExceptionEvent arg0) {

        }

        public void processRequest(RequestEvent requestEvent) {
            System.out.println("PROCESS REQUEST ON SERVER");
            Request request = requestEvent.getRequest();
            SipProvider provider = (SipProvider) requestEvent.getSource();
            if (request.getMethod().equals(Request.INVITE)) {
                try {
                    System.out.println("Received invite");
                    this.inviteStx = provider
                            .getNewServerTransaction(request);
                    this.inviteRequest = request;
                    Response response = messageFactory.createResponse(100, request);
                    inviteStx.sendResponse(response);

                    response = messageFactory.createResponse(180, request);
                    ((ToHeader) response.getHeader(ToHeader.NAME)).setTag("asdgaeyvewacyta"
                            + Math.random());
                    inviteStx.sendResponse(response);
                    System.out.println("Sent 180:\n" + response);
                } catch (Exception e) {
                    e.printStackTrace();
                    fail("Unexpected exception");
                }

            } else if (request.getMethod().equals(Request.CANCEL)) {

                System.out.println("Received CANCEL");
                try {
                    Response response = messageFactory.createResponse(200, requestEvent
                            .getRequest());
                    requestEvent.getServerTransaction().sendResponse(response);
                    
                    Response inviteResponse =
                        messageFactory.createResponse(Response.REQUEST_TERMINATED,inviteRequest);
                    inviteStx.sendResponse(inviteResponse);

                } catch (Exception e) {
                    e.printStackTrace();
                    fail("Unexpected exception ");
                }

            }

        }

        public void processResponse(ResponseEvent arg0) {

        }

        public void processTimeout(TimeoutEvent arg0) {

        }

        public void processTransactionTerminated(TransactionTerminatedEvent tte) {
            ClientTransaction ctx = tte.getClientTransaction();
            ServerTransaction stx = tte.getServerTransaction();
            if (ctx != null) {
                String method = ctx.getRequest().getMethod();
                if (method.equals(Request.INVITE)) {
                    System.out.println("Invite term TERM");
                }
            } else {
                String method = stx.getRequest().getMethod();
                if (method.equals(Request.INVITE)) {
                    System.out.println("Invite term TERM");
                }
            }
        }

    }

    public class Client implements SipListener {

        private SipFactory sipFactory;
        private SipStack sipStack;
        private SipProvider provider;
        private boolean o_sentInvite, o_received180, o_sentCancel, o_receiver200Cancel,
                o_inviteTxTerm, o_dialogTerinated;

        public Client() {
            try {
                final Properties defaultProperties = new Properties();
                host = "127.0.0.1";
                defaultProperties.setProperty("javax.sip.AUTOMATIC_DIALOG_SUPPORT", "off");
                defaultProperties.setProperty("javax.sip.STACK_NAME", "client");
                defaultProperties.setProperty("gov.nist.javax.sip.TRACE_LEVEL", "DEBUG");
                defaultProperties.setProperty("gov.nist.javax.sip.DEBUG_LOG", "target/logs/client_debug_NoAutoDialogTest.txt");
                defaultProperties.setProperty("gov.nist.javax.sip.SERVER_LOG", "client_log_NoAutoDialogTest.txt");
                defaultProperties.setProperty("gov.nist.javax.sip.READ_TIMEOUT", "1000");
                defaultProperties.setProperty("gov.nist.javax.sip.CACHE_SERVER_CONNECTIONS",
                        "false");
                if(System.getProperty("enableNIO") != null && System.getProperty("enableNIO").equalsIgnoreCase("true")) {
                	defaultProperties.setProperty("gov.nist.javax.sip.MESSAGE_PROCESSOR_FACTORY", NioMessageProcessorFactory.class.getName());
                }
                if(System.getProperty("enableNetty") != null && System.getProperty("enableNetty").equalsIgnoreCase("true")) {                    
                    defaultProperties.setProperty("gov.nist.javax.sip.MESSAGE_PROCESSOR_FACTORY", NettyMessageProcessorFactory.class.getName());
                }
                this.sipFactory = SipFactory.getInstance();
                this.sipFactory.resetFactory();
                this.sipFactory.setPathName("gov.nist");
                this.sipStack = this.sipFactory.createSipStack(defaultProperties);
                this.sipStack.start();
                ListeningPoint lp = this.sipStack.createListeningPoint(host, CLIENT_PORT, testProtocol);
                this.provider = this.sipStack.createSipProvider(lp);
                headerFactory = this.sipFactory.createHeaderFactory();
                messageFactory = this.sipFactory.createMessageFactory();
                addressFactory = this.sipFactory.createAddressFactory();
                this.provider.addSipListener(this);
            } catch (Exception e) {
                e.printStackTrace();
                fail("unexpected exception ");
            }

        }

        public TestAssertion getAssertion() {
            return new TestAssertion() {
                    @Override
                    public boolean assertCondition() {
                        return o_sentInvite && o_received180 && o_sentCancel && o_receiver200Cancel
                    && o_inviteTxTerm && o_dialogTerinated;
                    }
                };
        }        
        
        public void checkState() {
            if (!o_sentInvite || !o_received180 || !o_sentCancel || !o_receiver200Cancel
                    || !o_inviteTxTerm || !o_dialogTerinated) {
                fail("FAILED o_sentInvite[" + o_sentInvite + "] o_received180[" + o_received180
                        + "] o_sentCancel[" + o_sentCancel + "] o_receiver200Cancel["
                        + o_receiver200Cancel + "] o_inviteTxTerm[" + o_inviteTxTerm
                        + "] o_dialogTerinated[" + o_dialogTerinated + "]  ");

            }
        }


        public void stop() {
        	Utils.stopSipStack(this.sipStack);
        }

        public void processDialogTerminated(DialogTerminatedEvent arg0) {
            System.out.println("Dialog term TERM");
            o_dialogTerinated = true;

        }

        public void processIOException(IOExceptionEvent ioexception) {
            fail("Unexpected IO Exception");

        }

        public void processRequest(RequestEvent arg0) {

        }

        public void processResponse(ResponseEvent responseEvent) {
            Response response = responseEvent.getResponse();
            int code = response.getStatusCode();
            if (code == 180) {
                try {

                    o_received180 = true;
                    Request cancel = responseEvent.getClientTransaction().createCancel();
                    ClientTransaction cancelTX = provider.getNewClientTransaction(cancel);
                    cancelTX.sendRequest();
                    System.out.println("Send CANCEL:\n" + cancel);
                    o_sentCancel = true;

                } catch (SipException e) {
                    e.printStackTrace();
                    doFail(doMessage(e));
                }
            } else if (code == 200) {
                System.out.println("Receive Cancel200");
                o_receiver200Cancel = true;
            }
        }

        public void processTimeout(TimeoutEvent arg0) {

        }

        public void processTransactionTerminated(TransactionTerminatedEvent arg0) {
            ClientTransaction ctx = arg0.getClientTransaction();
            String method = ctx.getRequest().getMethod();
            if (method.equals(Request.INVITE)) {
                System.out.println("Invite TERM");
                o_inviteTxTerm = true;

            }

        }



        protected void sendLocalInvite() throws Exception {

            Request inviteRequest = messageFactory.createRequest(null);
            String _host = this.provider.getListeningPoint(testProtocol).getIPAddress();
            SipURI requestUri = addressFactory.createSipURI(null, _host);
            requestUri.setPort(SERVER_PORT);
            inviteRequest.setMethod(Request.INVITE);
            inviteRequest.setRequestURI(requestUri);

            SipURI _remoteUri = addressFactory.createSipURI(null, "there.com");
            Address _remoteAddress = addressFactory.createAddress(_remoteUri);


            inviteRequest.addHeader(provider.getNewCallId());
            inviteRequest.addHeader(headerFactory.createCSeqHeader((long) 1, Request.INVITE));

            SipURI _localUri = addressFactory.createSipURI(null, "here.com");
            Address localAddress = addressFactory.createAddress(_localUri);

            inviteRequest.addHeader(headerFactory.createFromHeader(localAddress,
                    generateFromTag()));

            inviteRequest.addHeader(headerFactory.createToHeader(_remoteAddress, null));

            SipURI contactUri = addressFactory.createSipURI(null, _host);
            contactUri.setPort(CLIENT_PORT);
            inviteRequest.addHeader(headerFactory.createContactHeader(addressFactory.createAddress(contactUri)));

            inviteRequest.addHeader(getLocalVia(provider));

            // create and add the Route Header
            headerFactory.createRouteHeader(_remoteAddress);

            inviteRequest.setMethod(Request.INVITE);
            inviteRequest.addHeader(headerFactory.createMaxForwardsHeader(5));
            ClientTransaction inviteTransaction = this.provider
                    .getNewClientTransaction(inviteRequest);
            provider.getNewDialog(inviteTransaction);

            inviteTransaction.sendRequest();
            System.out.println("Sent INVITE:\n" + inviteRequest);
            o_sentInvite = true;

        }

    }

    public void testSendInvite() throws InterruptedException, Exception {
        this.client.sendLocalInvite();
        AssertUntil.assertUntil(client.getAssertion(), 60000);
        this.client.checkState();

    }

}