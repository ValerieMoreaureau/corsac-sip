package test.unit.gov.nist.javax.sip.stack.dialog.b2bua;

import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Timer;
import java.util.TimerTask;

import javax.sip.ClientTransaction;
import javax.sip.Dialog;
import javax.sip.DialogState;
import javax.sip.DialogTerminatedEvent;
import javax.sip.IOExceptionEvent;
import javax.sip.ListeningPoint;
import javax.sip.RequestEvent;
import javax.sip.ResponseEvent;
import javax.sip.ServerTransaction;
import javax.sip.SipListener;
import javax.sip.SipProvider;
import javax.sip.SipStack;
import javax.sip.TransactionTerminatedEvent;
import javax.sip.address.Address;
import javax.sip.address.AddressFactory;
import javax.sip.address.SipURI;
import javax.sip.header.CSeqHeader;
import javax.sip.header.CallIdHeader;
import javax.sip.header.ContactHeader;
import javax.sip.header.ContentTypeHeader;
import javax.sip.header.FromHeader;
import javax.sip.header.Header;
import javax.sip.header.HeaderFactory;
import javax.sip.header.MaxForwardsHeader;
import javax.sip.header.RequireHeader;
import javax.sip.header.RouteHeader;
import javax.sip.header.ToHeader;
import javax.sip.header.ViaHeader;
import javax.sip.message.MessageFactory;
import javax.sip.message.Request;
import javax.sip.message.Response;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import gov.nist.javax.sip.ResponseEventExt;
import junit.framework.TestCase;
import test.tck.TestHarness;
import test.tck.msgflow.callflows.ProtocolObjects;
import test.tck.msgflow.callflows.TestAssertion;


/**
 * This class is a UAC template. Shootist is the guy that shoots and shootme is
 * the guy that gets shot.
 *
 * @author M. Ranganathan
 */

public class Shootist implements SipListener {

    private ContactHeader contactHeader;

    private ClientTransaction inviteTid;


    private SipProvider sipProvider;

    private String host = "127.0.0.1";

    private int port;

    private String peerHost = "127.0.0.1";

    private int peerPort;

    private ListeningPoint listeningPoint;

    private static String unexpectedException = "Unexpected exception ";

    private static Logger logger = LogManager.getLogger(Shootist.class);

    private HashSet<Dialog> forkedDialogs = new HashSet<Dialog>();

    private Dialog ackedDialog;

    public SipStack sipStack;

    private HashSet<Dialog> canceledDialog = new HashSet<Dialog>();

    private boolean byeResponseSeen;

    private static HeaderFactory headerFactory;

    private static MessageFactory messageFactory;

    private static AddressFactory addressFactory;

    private static final String transport = "udp";

    static boolean callerSendsBye  = true;

    private Timer timer = new Timer();

    private boolean inviteOkSeen;

    public long cancelDelay = -1;

    public int byeDelay = 1000;

    private boolean cancelSent;

    protected boolean inviteErrorResponseSeen;

    public boolean requireReliableProvisionalResponse;

    private boolean prackConfirmed;

    private int numberOfRelResponsesReceived;    

    class CancelTask extends TimerTask {
        public void run() {
            sendCancel();
        }
    }

    class SendBye extends TimerTask {

        private Dialog dialog;
        public SendBye(Dialog dialog ) {
            this.dialog = dialog;
        }
        @Override
        public void run() {
           try {
               TestCase.assertEquals ("Dialog state must be confirmed",
                        DialogState.CONFIRMED,dialog.getState());



               Request byeRequest = dialog.createRequest(Request.BYE);
               ClientTransaction ctx = sipProvider.getNewClientTransaction(byeRequest);
               dialog.sendRequest(ctx);
           } catch (Exception ex) {
               TestCase.fail("Unexpected exception");
           }

        }

    }



    public Shootist(int myPort, int proxyPort) throws Exception {


        this.port = myPort;

        ProtocolObjects sipObjects = new ProtocolObjects("shootist-" + myPort,"gov.nist","udp",true,false, false);
        addressFactory = sipObjects.addressFactory;
        messageFactory = sipObjects.messageFactory;
        headerFactory = sipObjects.headerFactory;
        this.sipStack = sipObjects.sipStack;

        this.peerPort = proxyPort;
        this.createSipProvider();
        this.sipProvider.addSipListener(this);


    }

    public void processRequest(RequestEvent requestReceivedEvent) {
        Request request = requestReceivedEvent.getRequest();
        ServerTransaction serverTransactionId = requestReceivedEvent
                .getServerTransaction();

        logger.info("\n\nRequest " + request.getMethod() + " received at "
                +  sipStack.getStackName()
                + " with server transaction id " + serverTransactionId);

        // We are the UAC so the only request we get is the BYE.
        if (request.getMethod().equals(Request.BYE))
            processBye(request, serverTransactionId);
        else
            TestCase.fail("Unexpected request ! : " + request);

    }

    public void processBye(Request request,
            ServerTransaction serverTransactionId) {
        try {
            logger.info("shootist:  got a bye .");
            if (serverTransactionId == null) {
                logger.info("shootist:  null TID.");
                return;
            }
            Dialog dialog = serverTransactionId.getDialog();
            logger.info("Dialog State = " + dialog.getState());
            Response response = messageFactory.createResponse(
                    200, request);
            serverTransactionId.sendResponse(response);
            logger.info("shootist:  Sending OK.");
            logger.info("Dialog State = " + dialog.getState());

        } catch (Exception ex) {
            ex.printStackTrace();
            junit.framework.TestCase.fail("Exit JVM");

        }
    }

    public synchronized void processResponse(ResponseEvent responseReceivedEvent) {
        System.out.println("Got a response " + responseReceivedEvent.getResponse());
        Response response = (Response) responseReceivedEvent.getResponse();
        ClientTransaction tid = responseReceivedEvent.getClientTransaction();
        CSeqHeader cseq = (CSeqHeader) response.getHeader(CSeqHeader.NAME);

        if((!((ResponseEventExt)responseReceivedEvent).isForkedResponse()) && 
                ((ResponseEventExt)responseReceivedEvent).isRetransmission()) {
                    System.out.println("Retransmission detected");
            return;        
        }
        System.out.println("Response received : Status Code = "
                + response.getStatusCode() + " " + cseq);
        System.out.println("Response = " + response + " class=" + response.getClass() );

        Dialog dialog = responseReceivedEvent.getDialog();
        TestCase.assertNotNull( dialog );

        if (tid != null)
            System.out.println("transaction state is " + tid.getState());
        else
            System.out.println("transaction = " + tid);

        System.out.println("Dialog = " + dialog);

        System.out.println("Dialog state is " + dialog.getState());

        this.canceledDialog.add(dialog);
        // Proxy will fork. I will accept the first dialog.
        this.forkedDialogs.add(dialog);

        try {
            if (response.getStatusCode() == Response.OK) {
                if (cseq.getMethod() == Request.PRACK) {
                    prackConfirmed = true;
                } else if (cseq.getMethod().equals(Request.INVITE)) {
                    this.inviteOkSeen = true;
                    TestCase.assertEquals( DialogState.CONFIRMED, dialog.getState() );
                    Request ackRequest = dialog.createAck(cseq
                            .getSeqNumber());

                    TestCase.assertNotNull( ackRequest.getHeader( MaxForwardsHeader.NAME ) );

                    if ( dialog == this.ackedDialog ) {
                        // Thread.sleep(3000);
                        dialog.sendAck(ackRequest);
                        return;
                    }
                    
                    if ( responseReceivedEvent.getClientTransaction() != null ) {
                        logger.info("Sending ACK " + ackRequest);
                        dialog.sendAck(ackRequest);
                        TestCase.assertTrue(
                                "Dialog state should be CONFIRMED", dialog
                                        .getState() == DialogState.CONFIRMED);

                        TestCase.assertTrue(this.ackedDialog == null ||
                                this.ackedDialog == dialog);
                        this.ackedDialog = dialog;

                        if ( callerSendsBye ) {
                            timer.schedule( new SendBye(ackedDialog), byeDelay);
                        }


                    } else {                        
                        // Send ACK to quench re-transmission
                        sipProvider.sendRequest(ackRequest);
                        // Kill the second dialog by sending a bye.
                        SipProvider sipProvider = (SipProvider) responseReceivedEvent
                                .getSource();

                        Request byeRequest = dialog.createRequest(Request.BYE);
                        ClientTransaction ct = sipProvider
                                .getNewClientTransaction(byeRequest);
                        dialog.sendRequest(ct);
                    }


                } else if ( cseq.getMethod().equals(Request.BYE)) {
                    this.byeResponseSeen = true;

                    // if ( dialog == this.ackedDialog) {
                    //     this.byeResponseSeen = true;
                    // }
                } else {
                    logger.info("Response method = " + cseq.getMethod());
                }
            } else if ( response.getStatusCode() == Response.RINGING ) {
                TestHarness.assertEquals( DialogState.EARLY, dialog.getState() );
                if(cancelDelay > 0) {
                    // Cancel the request after 2 seconds
                    this.timer.schedule(new CancelTask(), cancelDelay);
                }                
            } else if ( response.getStatusCode() == Response.SESSION_PROGRESS ) {
                TestHarness.assertEquals( DialogState.EARLY, dialog.getState() );
                RequireHeader requireHeader = (RequireHeader) response.getHeader(RequireHeader.NAME);
                if (requireHeader.getOptionTag().equalsIgnoreCase("100rel")) {
                    numberOfRelResponsesReceived++;                    
                    Request prackRequest = dialog.createPrack(response);
                    // create Request URI
                    // SipURI requestURI = addressFactory.createSipURI(toUser,
                    //         "127.0.0.1:" + port);
                    // prackRequest.setRequestURI(requestURI);
                    ClientTransaction ct = sipProvider.getNewClientTransaction(prackRequest);
                    dialog.sendRequest(ct);
                }
            } else if ( response.getStatusCode() == Response.REQUEST_TERMINATED ) {
                inviteErrorResponseSeen = true;
            }
        } catch (Throwable ex) {
            ex.printStackTrace();
            // junit.framework.TestCase.fail("Exit JVM");
        }

    }

    public SipProvider createSipProvider() {
        try {
            listeningPoint = sipStack.createListeningPoint(
                    host, port, "udp");

            logger.info("listening point = " + host + " port = " + port);
            logger.info("listening point = " + listeningPoint);
            sipProvider = sipStack
                    .createSipProvider(listeningPoint);
            return sipProvider;
        } catch (Exception ex) {
            logger.error(unexpectedException, ex);
            TestCase.fail(unexpectedException);
            return null;
        }

    }
    
    public TestAssertion getAssertion() {
        return new TestAssertion() {
            
            @Override
            public boolean assertCondition() {
                if(cancelDelay > 0) {
                    return inviteErrorResponseSeen && cancelSent;
                } else if(requireReliableProvisionalResponse) {
                    return prackConfirmed && numberOfRelResponsesReceived >= 2 && inviteOkSeen && byeResponseSeen; 
                } else {
                    return byeResponseSeen && inviteOkSeen;
                }
            }
        };
    }

    public void checkState() {
       
       TestCase.assertTrue("Should see BYE response for ACKED Dialog",this.byeResponseSeen);
       TestCase.assertTrue("InviteOK seen", this.inviteOkSeen);

    }

    public void processTimeout(javax.sip.TimeoutEvent timeoutEvent) {

        logger.info("Transaction Time out");
    }

    private void sendCancel() {
        try {
            if(!cancelSent) {
                logger.info("Sending cancel");

                Request cancelRequest = inviteTid.createCancel();
                ClientTransaction cancelTid = sipProvider
                        .getNewClientTransaction(cancelRequest);
                cancelTid.sendRequest();
                cancelSent = true;
            } else {
                logger.info("Cancel already sent");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            logger.error(unexpectedException, ex);
            fail(unexpectedException);
        }
    }

    public void sendInvite() {
        try {

         
            String fromName = "BigGuy";
            String fromSipAddress = "here.com";
            String fromDisplayName = "The Master Blaster";

            String toSipAddress = "there.com";
            String toUser = "LittleGuy";
            String toDisplayName = "The Little Blister";

            // create >From Header
            SipURI fromAddress = addressFactory.createSipURI(
                    fromName, fromSipAddress);

            Address fromNameAddress = addressFactory
                    .createAddress(fromAddress);
            fromNameAddress.setDisplayName(fromDisplayName);
            FromHeader fromHeader = headerFactory
                    .createFromHeader(fromNameAddress, "12345");

            // create To Header
            SipURI toAddress = addressFactory.createSipURI(
                    toUser, toSipAddress);
            Address toNameAddress = addressFactory
                    .createAddress(toAddress);
            toNameAddress.setDisplayName(toDisplayName);
            ToHeader toHeader = headerFactory.createToHeader(
                    toNameAddress, null);

            // create Request URI
            String peerHostPort = peerHost + ":" + peerPort;
            SipURI requestURI = addressFactory.createSipURI(
                    toUser, peerHostPort);

            // Create ViaHeaders

            ArrayList<ViaHeader> viaHeaders = new ArrayList<ViaHeader>();
            ViaHeader viaHeader = headerFactory
                    .createViaHeader(host, sipProvider.getListeningPoint(
                            transport).getPort(),
                            transport, null);

            // add via headers
            viaHeaders.add(viaHeader);

            SipURI sipuri = addressFactory.createSipURI(null,
                    host);
            sipuri.setPort(peerPort);
            sipuri.setLrParam();

            RouteHeader routeHeader = headerFactory
                    .createRouteHeader(addressFactory
                            .createAddress(sipuri));

            // Create ContentTypeHeader
            ContentTypeHeader contentTypeHeader = headerFactory
                    .createContentTypeHeader("application", "sdp");

            // Create a new CallId header
            CallIdHeader callIdHeader = sipProvider.getNewCallId();
            // JvB: Make sure that the implementation matches the messagefactory
            callIdHeader = headerFactory
                    .createCallIdHeader(callIdHeader.getCallId());

            // Create a new Cseq header
            CSeqHeader cSeqHeader = headerFactory
                    .createCSeqHeader(1L, Request.INVITE);

            // Create a new MaxForwardsHeader
            MaxForwardsHeader maxForwards = headerFactory
                    .createMaxForwardsHeader(70);

            // Create the request.
            Request request = messageFactory.createRequest(
                    requestURI, Request.INVITE, callIdHeader, cSeqHeader,
                    fromHeader, toHeader, viaHeaders, maxForwards);
            // Create contact headers

            SipURI contactUrl = addressFactory.createSipURI(
                    fromName, host);
            contactUrl.setPort(listeningPoint.getPort());

            // Create the contact name address.
            SipURI contactURI = addressFactory.createSipURI(
                    fromName, host);
            contactURI.setPort(sipProvider.getListeningPoint(
                    transport).getPort());
            contactURI.setTransportParam(transport);

            Address contactAddress = addressFactory
                    .createAddress(contactURI);

            // Add the contact address.
            contactAddress.setDisplayName(fromName);

            contactHeader = headerFactory
                    .createContactHeader(contactAddress);
            request.addHeader(contactHeader);

            // Dont use the Outbound Proxy. Use Lr instead.
            request.setHeader(routeHeader);

            // Add the extension header.
            Header extensionHeader = headerFactory
                    .createHeader("My-Header", "my header value");
            request.addHeader(extensionHeader);

            String sdpData = "v=0\r\n"
                    + "o=4855 13760799956958020 13760799956958020"
                    + " IN IP4  129.6.55.78\r\n" + "s=mysession session\r\n"
                    + "p=+46 8 52018010\r\n" + "c=IN IP4  129.6.55.78\r\n"
                    + "t=0 0\r\n" + "m=audio 6022 RTP/AVP 0 4 18\r\n"
                    + "a=rtpmap:0 PCMU/8000\r\n" + "a=rtpmap:4 G723/8000\r\n"
                    + "a=rtpmap:18 G729A/8000\r\n" + "a=ptime:20\r\n";
            byte[] contents = sdpData.getBytes();

            request.setContent(contents, contentTypeHeader);

            extensionHeader = headerFactory.createHeader(
                    "My-Other-Header", "my new header value ");
            request.addHeader(extensionHeader);

            Header callInfoHeader = headerFactory.createHeader(
                    "Call-Info", "<http://www.antd.nist.gov>");
            request.addHeader(callInfoHeader);

            if(requireReliableProvisionalResponse) {
                /*
                * When the UAC creates a new request, it can insist on reliable
                * delivery of provisional responses for that request. To do that,
                * it inserts a Require header field with the option tag 100rel into
                * the request.
                */
                RequireHeader requireHeader = headerFactory
                        .createRequireHeader("100rel");
                request.addHeader(requireHeader);
            }
            // Create the client transaction.
            inviteTid = sipProvider.getNewClientTransaction(request);
            Dialog dialog = inviteTid.getDialog();

            TestCase.assertTrue("Initial dialog state should be null",
                    dialog.getState() == null);

            // send the request out.
            inviteTid.sendRequest();

        } catch (Exception ex) {
            logger.error(unexpectedException, ex);
            TestCase.fail(unexpectedException);

        }
    }

    public void processIOException(IOExceptionEvent exceptionEvent) {
        logger.error("IOException happened for " + exceptionEvent.getHost()
                + " port = " + exceptionEvent.getPort());
        TestCase.fail("Unexpected exception");

    }

    public void processTransactionTerminated(
            TransactionTerminatedEvent transactionTerminatedEvent) {
        logger.info("Transaction terminated event recieved");
    }

    public void processDialogTerminated(
            DialogTerminatedEvent dialogTerminatedEvent) {
        TestCase.assertTrue("DTE dialog must be one of those we canceled",
                this.canceledDialog.contains((Dialog)dialogTerminatedEvent.getDialog() ));

    }

    public void stop() {
      this.sipStack.stop();
    }
}
