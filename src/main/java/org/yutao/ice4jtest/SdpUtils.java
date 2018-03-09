package org.yutao.ice4jtest;

import org.ice4j.Transport;
import org.ice4j.TransportAddress;
import org.ice4j.ice.*;
import org.ice4j.ice.sdp.CandidateAttribute;
import org.ice4j.ice.sdp.IceSdpUtils;
import org.opentelecoms.javax.sdp.NistSdpFactory;

import javax.sdp.*;
import java.util.StringTokenizer;
import java.util.Vector;

public class SdpUtils
{
    public static String createSDPDescription(Agent agent) throws Throwable
    {
        SdpFactory factory = new NistSdpFactory();
        SessionDescription sdess = factory.createSessionDescription();
        IceSdpUtils.initSessionDescription(sdess, agent);
        return sdess.toString();
    }

    public static void parseSDP(Agent localAgent, String sdp)
            throws Exception
    {
        SdpFactory factory = new NistSdpFactory();
        SessionDescription sdess = factory.createSessionDescription(sdp);

        for(IceMediaStream stream : localAgent.getStreams())
        {
            stream.setRemotePassword(sdess.getAttribute("ice-pwd"));
            stream.setRemoteUfrag(sdess.getAttribute("ice-ufrag"));
        }

        Connection globalConn = sdess.getConnection();
        String globalConnAddr = null;
        if(globalConn != null)
            globalConnAddr = globalConn.getAddress();

        Vector<MediaDescription> mdescs = sdess.getMediaDescriptions(true);

        for (MediaDescription desc : mdescs)
        {
            String streamName = desc.getMedia().getMediaType();

            IceMediaStream stream = localAgent.getStream(streamName);

            if(stream == null)
                continue;

            Vector<Attribute> attributes = desc.getAttributes(true);
            for (Attribute attribute : attributes)
            {
                if (attribute.getName().equals(CandidateAttribute.NAME))
                    parseCandidate(attribute, stream);
            }

            //set default candidates
            Connection streamConn = desc.getConnection();
            String streamConnAddr = null;
            if(streamConn != null)
                streamConnAddr = streamConn.getAddress();
            else
                streamConnAddr = globalConnAddr;

            int port = desc.getMedia().getMediaPort();

            TransportAddress defaultRtpAddress =
                    new TransportAddress(streamConnAddr, port, Transport.UDP);

            int rtcpPort = port + 1;
            String rtcpAttributeValue = desc.getAttribute("rtcp");

            if (rtcpAttributeValue != null)
                rtcpPort = Integer.parseInt(rtcpAttributeValue);

            TransportAddress defaultRtcpAddress =
                    new TransportAddress(streamConnAddr, rtcpPort, Transport.UDP);

            Component rtpComponent = stream.getComponent(Component.RTP);
            Component rtcpComponent = stream.getComponent(Component.RTCP);

            Candidate<?> defaultRtpCandidate
                    = rtpComponent.findRemoteCandidate(defaultRtpAddress);
            rtpComponent.setDefaultRemoteCandidate(defaultRtpCandidate);

            if(rtcpComponent != null)
            {
                Candidate<?> defaultRtcpCandidate
                        = rtcpComponent.findRemoteCandidate(defaultRtcpAddress);
                rtcpComponent.setDefaultRemoteCandidate(defaultRtcpCandidate);
            }
        }
    }

    /**
     * Parses the <tt>attribute</tt>.
     *
     * @param attribute the attribute that we need to parse.
     * @param stream the {@link IceMediaStream} that the candidate is supposed
     * to belong to.
     *
     * @return a newly created {@link RemoteCandidate} matching the
     * content of the specified <tt>attribute</tt> or <tt>null</tt> if the
     * candidate belonged to a component we don't have.
     */
    private static RemoteCandidate parseCandidate(Attribute      attribute,
                                                  IceMediaStream stream)
    {
        String value = null;

        try{
            value = attribute.getValue();
        }catch (Throwable t){}//can't happen

        StringTokenizer tokenizer = new StringTokenizer(value);

        //XXX add exception handling.
        String foundation = tokenizer.nextToken();
        int componentID = Integer.parseInt( tokenizer.nextToken() );
        Transport transport = Transport.parse(tokenizer.nextToken());
        long priority = Long.parseLong(tokenizer.nextToken());
        String address = tokenizer.nextToken();
        int port = Integer.parseInt(tokenizer.nextToken());

        TransportAddress transAddr
                = new TransportAddress(address, port, transport);

        tokenizer.nextToken(); //skip the "typ" String
        CandidateType type = CandidateType.parse(tokenizer.nextToken());

        Component component = stream.getComponent(componentID);

        if(component == null)
            return null;

        // check if there's a related address property

        RemoteCandidate relatedCandidate = null;
        if (tokenizer.countTokens() >= 4)
        {
            tokenizer.nextToken(); // skip the raddr element
            String relatedAddr = tokenizer.nextToken();
            tokenizer.nextToken(); // skip the rport element
            int relatedPort = Integer.parseInt(tokenizer.nextToken());

            TransportAddress raddr = new TransportAddress(
                    relatedAddr, relatedPort, Transport.UDP);

            relatedCandidate = component.findRemoteCandidate(raddr);
        }

        RemoteCandidate cand = new RemoteCandidate(transAddr, component, type,
                foundation, priority, relatedCandidate);

        component.addRemoteCandidate(cand);

        return cand;
    }


}
