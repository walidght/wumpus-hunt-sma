package eu.su.mas.dedaleEtu.mas.behaviours;

import dataStructures.serializableGraph.SerializableNode;
import eu.su.mas.dedaleEtu.mas.agents.dummies.explo.ExploreCoopAgent;


import java.io.IOException;
import java.util.*;

import dataStructures.serializableGraph.SerializableSimpleGraph;
import dataStructures.tuple.Couple;
import eu.su.mas.dedale.env.Location;
import eu.su.mas.dedale.env.Observation;
import eu.su.mas.dedale.env.gs.gsLocation;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;

import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation.MapAttribute;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation;
import eu.su.mas.dedaleEtu.mas.behaviours.ShareMapBehaviour;

import jade.core.AID;
import jade.core.behaviours.SimpleBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;
import weka.core.pmml.jaxbbindings.False;


public class HuntBehaviour extends SimpleBehaviour {

    private static final long serialVersionUID = 8567689731496787661L;

    private boolean finished = false;

    /**
     * Current knowledge of the agent regarding the environment
     */
    private MapRepresentation myMap;

    private List<String> list_agentNames;

    private boolean blocked = false;

    private String targetNode = "";

    private Location golemPos = null;

    private int iterations = 0;
    private Location suspectedGolemPosition; //this attribute containst the location of the golem when it is detected and sent to other agents

    /**
     * @param myagent    reference to the agent we are adding this behaviour to
     * @param myMap      known map of the world the agent is living in
     * @param agentNames name of the agents to share the map with
     */
    public HuntBehaviour(final AbstractDedaleAgent myagent, MapRepresentation myMap, List<String> agentNames) {
        super(myagent);
        this.myMap = myMap;
        this.list_agentNames = agentNames;
    }

    @Override
    public void action() {

        Location myPosition = ((AbstractDedaleAgent) this.myAgent).getCurrentPosition();

        ACLMessage receivedMessage = this.myAgent.receive();
        while (receivedMessage != null) {
            if (receivedMessage.getPerformative() == ACLMessage.INFORM) {
                logMessage("Received a message [INFORM]: "+receivedMessage.getContent());
                if (golemPos != null && receivedMessage.getContent().equals(golemPos.getLocationId())) {
                    logMessage("Reseting golem position to null");
                    golemPos = null;
                }
            }
            else if (receivedMessage.getPerformative() == ACLMessage.REQUEST) {
                logMessage("Received a message [REQUEST]: "+receivedMessage.getContent());
                ACLMessage message = new ACLMessage(CustomPerformatives.SUBMAP);
                AID senderAID = receivedMessage.getSender();
                try {
                    message.setContentObject(this.myMap.getSerializableGraph());

                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                message.addReceiver(senderAID);
                myAgent.send(message);
            }
            else  {
                logMessage("Received a message [UNKNOWN]: "+receivedMessage.getContent());
            }
            receivedMessage = this.myAgent.receive();

        }

        // Start the hunt if the exploration is finished
        if (!((ExploreCoopAgent) this.myAgent).getMapDiscovered()) {
            return;
        }

        logMessage("running action, currently in "+myPosition.getLocationId()+" (iteration="+this.iterations+")");

        iterations++;

        if (golemPos != null && myPosition.getLocationId().equals(golemPos.getLocationId())) {
            golemPos = null;
        }


        ArrayList<Location> possibleGolemPositions = new ArrayList<Location>();

        List<Couple<Location, List<Couple<Observation, Integer>>>> lobs = ((AbstractDedaleAgent) this.myAgent).observe();//myPosition
        Location nextPos;
        possibleGolemPositions = getPossibleGolemPositions(lobs);

        if (possibleGolemPositions.isEmpty()) {

            if (!blocked) {
                if (targetNode.isEmpty() || targetNode.equals(myPosition.getLocationId())) {
                    Random random = new Random();
                    SerializableSimpleGraph graph = this.myMap.getSerializableGraph();
                    Object[] nodes = graph.getAllNodes().toArray();
                    int randomIndex = random.nextInt(nodes.length);
                    do {
                        targetNode = ((SerializableNode<String, Object>) nodes[randomIndex]).getNodeId();
                    } while (targetNode.equals(myPosition.getLocationId()));
                }
                nextPos = new gsLocation(this.myMap.getShortestPath(myPosition.getLocationId(), targetNode).get(0));

            } else {
                Random random = new Random();
                int randomIndex = random.nextInt(lobs.size());
                blocked = false;
                nextPos = lobs.get(randomIndex).getLeft();
            }

        } else {
            Random random = new Random();
            int randomIndex = random.nextInt(possibleGolemPositions.size());
            targetNode = "";
            nextPos = possibleGolemPositions.get(randomIndex);
        }

        boolean moved;

        if (golemPos == null) {
            try {
                moved = ((AbstractDedaleAgent) this.myAgent).moveTo(nextPos);

                if (moved) {
                    blocked = false;
                } else {
                    blocked = true;
                    golemPos = nextPos;
                    ACLMessage message = new ACLMessage(ACLMessage.INFORM);
                    message.setContent(myPosition.getLocationId());
                    for (String agent_name : this.list_agentNames) {
                        message.addReceiver(new AID(agent_name, AID.ISLOCALNAME));
                    }
                    myAgent.send(message);
                    logMessage("Blocked by agent/golem!");
                    logMessage("Sent my position to near agents (going to random target)");
                    logMessage("Currently at iteration: " + iterations);
                }
            } catch (Exception e) {
                logMessage("Couldnt move to next position");
            }


        } else {
            try {
                moved = ((AbstractDedaleAgent) this.myAgent).moveTo(golemPos);

                if (moved) {
                    golemPos = null;
                    blocked = false;
                } else {

                    ACLMessage message = new ACLMessage(ACLMessage.INFORM);
                    message.setContent(myPosition.getLocationId());
                    for (String agent_name : this.list_agentNames) {
                        message.addReceiver(new AID(agent_name, AID.ISLOCALNAME));
                    }
                    myAgent.send(message);
                    logMessage("sent my position to near agents (going to golem position:"+golemPos.getLocationId()+" )");
                    blocked = true;
                }

            } catch (Exception e1) {
                logMessage("Couldnt move to\"golem\" position: " + golemPos.getLocationId());
            }
        }


        try {
            this.myAgent.doWait(500);
            //System.in.read();
        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    private ArrayList<Location> getPossibleGolemPositions(List<Couple<Location, List<Couple<Observation, Integer>>>> lobs) {
        //this function returns a list of position where there is stench
        ArrayList<Location> possibleGolemPos = new ArrayList<Location>();

        Iterator<Couple<Location, List<Couple<Observation, Integer>>>> iter = lobs.iterator();
        while (iter.hasNext()) {
            Couple<Location, List<Couple<Observation, Integer>>> pair = iter.next();
            Location accessibleNode = pair.getLeft();
            List<Couple<Observation, Integer>> obs = pair.getRight();
            //check if there is "Stench on the observations
            for (Couple<Observation, Integer> couple : obs) {
                Observation observation = couple.getLeft();
                if (observation == Observation.STENCH) {
                    //System.out.println("Observed: " + observation.toString() + " at " + accessibleNode.toString());
                    possibleGolemPos.add(accessibleNode);
                }
            }
        }
        return possibleGolemPos;
    }

    public boolean isGolemInLocation(Location nextPos, List<Couple<Location, List<Couple<Observation, Integer>>>> lobs) {
        for (Couple<Location, List<Couple<Observation, Integer>>> couple : lobs) {
            Location location = couple.getLeft();
            if (location.getLocationId().equals(nextPos.getLocationId())) {
                // Found the desired location
                List<Couple<Observation, Integer>> obs = couple.getRight();
                //check if there is "Stench on the observations
                for (Couple<Observation, Integer> locationObservation : obs) {
                    Observation observation = locationObservation.getLeft();
                    logMessage(observation.toString());
                    if (observation == Observation.AGENTNAME) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private void logMessage(String message) {
        System.out.println(myAgent.getLocalName() + " - " + message);
    }

    public void setDiscoveredMap(MapRepresentation map) {
        this.myMap = map;
    }

    @Override
    public boolean done() {
        return finished;
    }

}