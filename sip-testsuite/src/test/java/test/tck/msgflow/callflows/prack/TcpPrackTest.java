package test.tck.msgflow.callflows.prack;

/**
 * Shootist sends INVITE
 * Shootme receives INVITE and send Trying, then 183 with 100Rel
 * Shootist receives 183 and sends PRACK 
 * Shootme receives PRACK and sends 200 OK to PRACK then 200 OK to INVITE
 * Shootist receives 200 OK PRACK then 200 OK to INVITE and sends ACK
 * Shootme receives ACK and sends BYE
 * Shootist receives BYE and sends 200 OK to BYE
 */
public class TcpPrackTest extends AbstractPrackTestCase {
    boolean myFlag;

    public void setUp() throws Exception {
        super.testedImplFlag = !myFlag;
        myFlag = !super.testedImplFlag;
        super.transport = "tcp";
        super.setUp();
    }
    
    public void testPrack() {
        this.shootist.sendInvite();

    }

    public void testPrack2() {
        this.shootist.sendInvite();
    }
}
