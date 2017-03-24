/*
* Conditions Of Use
*
* This software was developed by employees of the National Institute of
* Standards and Technology (NIST), an agency of the Federal Government.
* Pursuant to title 15 Untied States Code Section 105, works of NIST
* employees are not subject to copyright protection in the United States
* and are considered to be in the public domain.  As a result, a formal
* license is not needed to use the software.
*
* This software is provided by NIST as a service and is expressly
* provided "AS IS."  NIST MAKES NO WARRANTY OF ANY KIND, EXPRESS, IMPLIED
* OR STATUTORY, INCLUDING, WITHOUT LIMITATION, THE IMPLIED WARRANTY OF
* MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, NON-INFRINGEMENT
* AND DATA ACCURACY.  NIST does not warrant or make any representations
* regarding the use of the software or the results thereof, including but
* not limited to the correctness, accuracy, reliability or usefulness of
* the software.
*
* Permission to use this software is contingent upon your acceptance
* of the terms of this agreement
*
* .
*
*/
/***************************************************************************
 * Product of NIST/ITL Advanced Networking Technologies Division(ANTD).   *
 **************************************************************************/
package gov.nist.core;

import java.net.*;

/*
 * IPv6 Support added by Emil Ivov (emil_ivov@yahoo.com)<br/>
 * Network Research Team (http://www-r2.u-strasbg.fr))<br/>
 * Louis Pasteur University - Strasbourg - France<br/>
 *
 * Frank Feif reported a bug.
 *
 *
 */
/**
 * Stores hostname.
 * @version 1.2
 *
 * @author M. Ranganathan
 * @author Emil Ivov <emil_ivov@yahoo.com> IPV6 Support. <br/>
 *
 *
 *

 * Marc Bednarek <bednarek@nist.gov> (Bugfixes).<br/>
 *
 */
public class Host extends GenericObject {

    /**
     * Determines whether or not we should tolerate and strip address scope
     * zones from IPv6 addresses. Address scope zones are sometimes returned
     * at the end of IPv6 addresses generated by InetAddress.getHostAddress().
     * They are however not part of the SIP semantics so basically this method
     * determines whether or not the parser should be stripping them (as
     * opposed simply being blunt and throwing an exception).
     */
    public static boolean stripAddressScopeZones = false;

    private static final long serialVersionUID = -7233564517978323344L;
    protected static final int HOSTNAME = 1;
    protected static final int IPV4ADDRESS = 2;
    protected static final int IPV6ADDRESS = 3;

    static {
    	stripAddressScopeZones = Boolean.getBoolean("gov.nist.core.STRIP_ADDR_SCOPES");
    }
    
    /** hostName field
     */
    protected String hostname;

    /** address field
     */

    protected int addressType;

    private InetAddress inetAddress;

    /** default constructor
     */
    public Host() {
        addressType = HOSTNAME;
    }

    /** Constructor given host name or IP address.
     */
    public Host(String hostName) throws IllegalArgumentException {
        if (hostName == null)
            throw new IllegalArgumentException("null host name");        

        setHost(hostName, IPV4ADDRESS);
    }

    /** constructor
     * @param name String to set
     * @param addrType int to set
     */
    public Host(String name, int addrType) {
        setHost(name, addrType);
    }

    /**
     * Return the host name in encoded form.
     * @return String
     */
    public String encode() {
        return encode(new StringBuilder()).toString();
    }

    public StringBuilder encode(StringBuilder buffer) {
        if (addressType == IPV6ADDRESS && !isIPv6Reference(hostname)) {
            buffer.append('[').append(hostname).append(']');
        } else {
            buffer.append(hostname);
        }
        return buffer;
    }

    /**
     * Compare for equality of hosts.
     * Host names are compared by textual equality. No dns lookup
     * is performed.
     * @param obj Object to set
     * @return boolean
     */
    public boolean equals(Object obj) {
        if ( obj == null ) return false;
        if (!this.getClass().equals(obj.getClass())) {
            return false;
        }
        Host otherHost = (Host) obj;
        return otherHost.hostname.equals(hostname);

    }

    /** get the HostName field
     * @return String
     */
    public String getHostname() {
        return hostname;
    }

    /** get the Address field
     * @return String
     */
    public String getAddress() {
        return hostname;
    }

    /**
     * Convenience function to get the raw IP destination address
     * of a SIP message as a String.
     * @return String
     */
    public String getIpAddress() {
        String rawIpAddress = null;
        if (hostname == null)
            return null;
        if (addressType == HOSTNAME) {
            try {
                if (inetAddress == null)
                    inetAddress = InetAddress.getByName(hostname);
                rawIpAddress = inetAddress.getHostAddress();
            } catch (UnknownHostException ex) {
                dbgPrint("Could not resolve hostname " + ex);
            }
        } else {
            if (addressType == IPV6ADDRESS){
                try {
                    String ipv6FullForm = getInetAddress().toString();
                    int slashIndex = ipv6FullForm.indexOf("/");
                    if (slashIndex != -1) {
                        ipv6FullForm = ipv6FullForm.substring(++slashIndex, ipv6FullForm.length());
                    }
                    if (hostname.startsWith("[")) {
                        rawIpAddress = '[' + ipv6FullForm + ']';
                    } else {
                        rawIpAddress = ipv6FullForm;
                    }
                } catch (UnknownHostException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            } else {
                rawIpAddress = hostname;
            }
        }
        return rawIpAddress;
    }

    /**
     * Set the hostname member.
     * @param h String to set
     */
    public void setHostname(String h) {
        setHost(h, HOSTNAME);
    }

    /** Set the IP Address.
     *@param address is the address string to set.
     */
    public void setHostAddress(String address) {
        setHost(address, IPV4ADDRESS);
    }

    /**
     * Sets the host address or name of this object.
     *
     * @param host that host address/name value
     * @param type determines whether host is an address or a host name
     */
    private void setHost(String host, int type){
        //set inetAddress to null so that it would be reinited
        //upon next call to getInetAddress()
        inetAddress = null;

        if (isIPv6Address(host))
            addressType = IPV6ADDRESS;
        else
            addressType = type;

        // Null check bug fix sent in by jpaulo@ipb.pt
        if (host != null){
            hostname = host.trim();

            //if this is an FQDN, make it lowercase to simplify processing
            if(addressType == HOSTNAME)
                hostname = hostname.toLowerCase();

            //remove address scope zones if this is an IPv6 address as they
            //are not allowed by the RFC
            int zoneStart = -1;
            if(addressType == IPV6ADDRESS
                && stripAddressScopeZones
                && (zoneStart = hostname.indexOf('%'))!= -1){

                hostname = hostname.substring(0, zoneStart);

                //if the above was an IPv6 literal, then we would need to
                //restore the closing bracket
                if( hostname.startsWith("[") && !hostname.endsWith("]"))
                    hostname += ']';
            }
        }
    }

    /**
     * Set the address member
     * @param address address String to set
     */
    public void setAddress(String address) {
        this.setHostAddress(address);
    }

    /** Return true if the address is a DNS host name
     *  (and not an IPV4 address)
     *@return true if the hostname is a DNS name
     */
    public boolean isHostname() {
        return addressType == HOSTNAME;
    }

    /** Return true if the address is a DNS host name
     *  (and not an IPV4 address)
     *@return true if the hostname is host address.
     */
    public boolean isIPAddress() {
        return addressType != HOSTNAME;
    }

    /** Get the inet address from this host.
     * Caches the inet address returned from dns lookup to avoid
     * lookup delays.
     *
     *@throws UnkownHostexception when the host name cannot be resolved.
     */
    public InetAddress getInetAddress() throws java.net.UnknownHostException {
        if (hostname == null)
            return null;
        if (inetAddress != null)
            return inetAddress;
        inetAddress = InetAddress.getByName(hostname);
        return inetAddress;

    }

    //----- IPv6
    /**
     * Verifies whether the <code>address</code> could
     * be an IPv6 address
     */
    private boolean isIPv6Address(String address) {
        return (address != null && address.indexOf(':') != -1);
    }

    /**
     * Verifies whether the ipv6reference, i.e. whether it enclosed in
     * square brackets
     */
    public static boolean isIPv6Reference(String address) {
        return address.charAt(0) == '['
            && address.charAt(address.length() - 1) == ']';
    }

    @Override
    public int hashCode() {
        return this.getHostname().hashCode();

    }
}
