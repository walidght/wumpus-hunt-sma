package eu.su.mas.dedaleEtu.mas.behaviours;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import dataStructures.serializableGraph.SerializableSimpleGraph;
import dataStructures.tuple.Couple;
import eu.su.mas.dedale.env.Location;
import eu.su.mas.dedale.env.Observation;
import eu.su.mas.dedale.env.gs.gsLocation;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;

//import eu.su.mas.dedale.mas.agents.dedaleDummyAgents.Explo.ExploreCoopAgent;
import eu.su.mas.dedaleEtu.mas.agents.dummies.explo.ExploreCoopAgent;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation.MapAttribute;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation;
import eu.su.mas.dedaleEtu.mas.behaviours.ShareMapBehaviour;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.SimpleBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;


public class ShareMapImprovedBehaviour extends SimpleBehaviour {

    private static final long serialVersionUID = 8567689731496787661L;

    private boolean finished = false;

    private final List<String> list_agentNames;

    // a list containing a submap for all the other agents,
    // each time we share a map to an agent we clear its map to avoid
    // sending the same data twice to the same agent
    private ArrayList<MapRepresentation> agents_submaps;

    /**
     *
     * @param myagent reference to the agent we are adding this behaviour to
     * @param agentNames name of the agents to share the map with
     */
    public ShareMapImprovedBehaviour(Agent myagent, List<String> agentNames) {
        super(myagent);
        this.list_agentNames=agentNames;
        //this.agents_submaps=agents_submaps;


    }

    @Override
    public void action() {
        if (agents_submaps == null) {
            agents_submaps = new ArrayList<>();
            for(String agent: list_agentNames){
                agents_submaps.add(new MapRepresentation());
            }
        }

        for(int i=0; i < list_agentNames.size(); i++) {
            ACLMessage msg = new ACLMessage(CustomPerformatives.PING);
            msg.setProtocol("SHARE-TOPO");
            msg.setSender(this.myAgent.getAID());
            msg.addReceiver(new AID(list_agentNames.get(i), AID.ISLOCALNAME));
/*
            SerializableSimpleGraph<String, MapAttribute> sg = this.agents_submaps.get(i).getSerializableGraph();
            try {
                msg.setContentObject(sg);
            } catch (IOException e) {
                e.printStackTrace();
            }*/
            msg.setContent("");
            ((AbstractDedaleAgent) this.myAgent).sendMessage(msg);
           // this.agents_submaps.set(i,new MapRepresentation());

        }


    }

    void setFinished(boolean finished){
        this.finished=finished;
    }
    @Override
    public boolean done() {
        return finished;
    }
}
