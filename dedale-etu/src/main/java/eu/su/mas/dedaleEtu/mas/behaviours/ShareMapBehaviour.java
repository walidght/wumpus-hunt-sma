package eu.su.mas.dedaleEtu.mas.behaviours;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import dataStructures.serializableGraph.SerializableSimpleGraph;

import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedaleEtu.mas.agents.dummies.explo.ExploreCoopAgent;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation.MapAttribute;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.TickerBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;

/**
 * The agent periodically share its map.
 * It blindly tries to send all its graph to its friend(s)
 * If it was written properly, this sharing action would NOT be in a ticker behaviour and only a subgraph would be shared.
 *
 * @author hc
 */
public class ShareMapBehaviour extends TickerBehaviour {

    private List<String> receivers;

    private MapRepresentation myMap;
    private ArrayList<MapRepresentation> agents_submaps;

    /**
     * The agent periodically share its map.
     * It blindly tries to send all its graph to its friend(s)
     * If it was written properly, this sharing action would NOT be in a ticker behaviour and only a subgraph would be shared.
     *
     * @param a              the agent
     * @param period         the periodicity of the behaviour (in ms)
     * @param myMap          references our map, to merge it with the received submaps
     * @param receivers      the list of agents to send the map to
     * @param agents_submaps the list of submaps to share
     */
    public ShareMapBehaviour(Agent a, long period, MapRepresentation myMap, List<String> receivers, ArrayList<MapRepresentation> agents_submaps) {
        super(a, period);
        this.receivers = receivers;
        this.myMap = myMap;
        this.agents_submaps = agents_submaps;
    }

    /**
     *
     */
    private static final long serialVersionUID = -568863390879327961L;

    @Override
    protected void onTick() {
        // at each step, we send a ping to all the other agents in communication area
        ACLMessage msg = new ACLMessage(CustomPerformatives.PING);
        msg.setProtocol("SHARE-TOPO");
        msg.setSender(this.myAgent.getAID());
        for (String agentName : receivers) {
            msg.addReceiver(new AID(agentName, AID.ISLOCALNAME));
        }
        msg.setContent("");
        ((AbstractDedaleAgent) this.myAgent).sendMessage(msg);


        // we check if we received a message from another agent
        ACLMessage receivedMessage = this.myAgent.receive();

        if (receivedMessage != null) {
            // getting the index of the sender to retrieve corresponding submap
            String senderLocalName = receivedMessage.getSender().getLocalName();
            int senderIndex = this.receivers.indexOf(senderLocalName);

            if (receivedMessage.getPerformative() == CustomPerformatives.PING) {
				logMessage("pong: sending submap to " + senderLocalName);
                MapRepresentation senderSubmap = this.agents_submaps.get(senderIndex);
                // send a pong + submap
                ACLMessage message = new ACLMessage(CustomPerformatives.PONG);
                try {
                    message.setContentObject(senderSubmap.getSerializableGraph());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                message.addReceiver(new AID(senderLocalName, AID.ISLOCALNAME));
                myAgent.send(message);
                // reset the sender's submap
                this.agents_submaps.set(senderIndex, new MapRepresentation(false));
            } else if (receivedMessage.getPerformative() == CustomPerformatives.PONG) {
                // merge the received submap with your map
                SerializableSimpleGraph<String, MapRepresentation.MapAttribute> sgreceived = null;
                try {
                    sgreceived = (SerializableSimpleGraph<String, MapRepresentation.MapAttribute>) receivedMessage.getContentObject();
                } catch (UnreadableException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                this.myMap.mergeMap(sgreceived);
				logMessage("merging received map from " + senderLocalName + " then sending him map");
                // send the sender his corresponding submap
                MapRepresentation senderSubmap = this.agents_submaps.get(senderIndex);
                // send submap
                ACLMessage message = new ACLMessage(CustomPerformatives.SUBMAP);
                try {
                    message.setContentObject(senderSubmap.getSerializableGraph());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                message.addReceiver(new AID(senderLocalName, AID.ISLOCALNAME));
                myAgent.send(message);
                // reset the sender's submap
                this.agents_submaps.set(senderIndex, new MapRepresentation(false));
            } else if (receivedMessage.getPerformative() == CustomPerformatives.SUBMAP) {
                // merge the received submap with your map
                SerializableSimpleGraph<String, MapRepresentation.MapAttribute> sgreceived = null;
                try {
                    sgreceived = (SerializableSimpleGraph<String, MapRepresentation.MapAttribute>) receivedMessage.getContentObject();
                } catch (UnreadableException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                this.myMap.mergeMap(sgreceived);
				logMessage("merging received map from " + senderLocalName);
            } else if (receivedMessage.getPerformative() == ACLMessage.INFORM) {
				logMessage("received location of " + senderLocalName);

                ACLMessage message = new ACLMessage(ACLMessage.INFORM);
                message.setContent(((AbstractDedaleAgent) this.myAgent).getCurrentPosition().getLocationId());
                message.addReceiver(new AID(senderLocalName, AID.ISLOCALNAME));
                myAgent.send(message);
                ACLMessage message2 = new ACLMessage(ACLMessage.REQUEST);
                message2.setContent("");
                message2.addReceiver(new AID(senderLocalName, AID.ISLOCALNAME));
                myAgent.send(message2);
            } else {
                // Otherwise, ignore the message or handle it as needed
				logMessage("Received message of unknown type or content" + receivedMessage.getPerformative());
            }
        } else {
            block();
        }


    }

	private void logMessage(String message) {
		System.out.println(myAgent.getLocalName() + " - " + message);
	}


}
