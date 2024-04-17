package eu.su.mas.dedaleEtu.mas.behaviours;

import dataStructures.serializableGraph.SerializableSimpleGraph;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedaleEtu.mas.agents.dummies.explo.ExploreCoopAgent;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.UnreadableException;

import java.util.List;

public class ReceiveMessagesBehaviour extends CyclicBehaviour {
    public ReceiveMessagesBehaviour(final AbstractDedaleAgent myagent) {
        super(myagent);
    }

    public void action() {
        ACLMessage msg = this.myAgent.receive();

        if (msg != null) {
            String senderLocalName = msg.getSender().getLocalName();
            int senderIndex = ((ExploreCoopAgent)this.myAgent).list_agentNames.indexOf(senderLocalName);

            if (msg.getPerformative() == CustomPerformatives.PING) {
                System.out.println("sending + reseting map to"+senderLocalName);
                MapRepresentation senderSubmap = ((ExploreCoopAgent)myAgent).agents_submaps.get(senderIndex);
                ((ExploreCoopAgent)myAgent).agents_submaps.set(senderIndex,new MapRepresentation());
                myAgent.addBehaviour(new SendMessageBehaviour(new AID(senderLocalName,AID.ISLOCALNAME), senderSubmap,CustomPerformatives.PONG));
            } else if (msg.getPerformative() == CustomPerformatives.PONG) {
                // merge the received submap with your map
                SerializableSimpleGraph<String, MapRepresentation.MapAttribute> sgreceived=null;
                try {
                    sgreceived = (SerializableSimpleGraph<String, MapRepresentation.MapAttribute>)msg.getContentObject();
                } catch (UnreadableException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                ((ExploreCoopAgent)myAgent).myMap.mergeMap(sgreceived);
                System.out.println("merging received map from "+senderLocalName  + " then sending him map");

                // send the sender his corresponding submap
                MapRepresentation senderSubmap = ((ExploreCoopAgent)myAgent).agents_submaps.get(senderIndex);
                ((ExploreCoopAgent)myAgent).agents_submaps.set(senderIndex,new MapRepresentation());
                myAgent.addBehaviour(new SendMessageBehaviour(new AID(senderLocalName,AID.ISLOCALNAME), senderSubmap,CustomPerformatives.SUBMAP));
            } else if (msg.getPerformative() == CustomPerformatives.SUBMAP) {
                // merge the received submap with your map
                SerializableSimpleGraph<String, MapRepresentation.MapAttribute> sgreceived=null;
                try {
                    sgreceived = (SerializableSimpleGraph<String, MapRepresentation.MapAttribute>)msg.getContentObject();
                } catch (UnreadableException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                ((ExploreCoopAgent)myAgent).myMap.mergeMap(sgreceived);
                System.out.println("marging received map from "+senderLocalName);
            }

            else {
                // Otherwise, ignore the message or handle it as needed
                System.out.println("Received message of unknown type or content"+msg.getPerformative());
            }
        } else {
            block();
        }
    }
}