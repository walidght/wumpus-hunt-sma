package eu.su.mas.dedaleEtu.mas.behaviours;

import java.io.IOException;
import java.util.*;

import dataStructures.serializableGraph.SerializableSimpleGraph;
import dataStructures.tuple.Couple;
import eu.su.mas.dedale.env.Location;
import eu.su.mas.dedale.env.Observation;
import eu.su.mas.dedale.env.gs.gsLocation;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;

import eu.su.mas.dedale.mas.agent.behaviours.platformManagment.startMyBehaviours;
import eu.su.mas.dedaleEtu.mas.agents.dummies.explo.ExploreCoopAgent;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation.MapAttribute;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation;
import eu.su.mas.dedaleEtu.mas.behaviours.ShareMapBehaviour;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.SimpleBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;


/**
 * <pre>
 * This behaviour allows an agent to explore the environment and learn the associated topological map.
 * The algorithm is a pseudo - DFS computationally consuming because its not optimised at all.
 *
 * When all the nodes around him are visited, the agent randomly select an open node and go there to restart its dfs.
 * This (non optimal) behaviour is done until all nodes are explored.
 *
 * Warning, this behaviour does not save the content of visited nodes, only the topology.
 * Warning, the sub-behaviour ShareMap periodically share the whole map
 * </pre>
 * @author hc
 *
 */
public class ExploCoopBehaviour extends SimpleBehaviour {

	private static final long serialVersionUID = 8567689731496787661L;

	private boolean finished = false;

	/**
	 * Current knowledge of the agent regarding the environment
	 */
	private MapRepresentation myMap;

	private List<String> list_agentNames;

	private ArrayList<MapRepresentation> agents_submaps;

	private ShareMapBehaviour shareMapBehaviour;

	/**
	 *
	 * @param myagent reference to the agent we are adding this behaviour to
	 * @param myMap known map of the world the agent is living in
	 * @param agentNames name of the agents to share the map with
	 */
	public ExploCoopBehaviour(final AbstractDedaleAgent myagent, MapRepresentation myMap,List<String> agentNames) {
		super(myagent);
		this.myMap=myMap;
		this.list_agentNames=agentNames;
		this.agents_submaps = new ArrayList<>();
	}

	@Override
	public void action() {

		// if my map is null, create a new empty map
		if(this.myMap==null) {
			this.myMap= new MapRepresentation();
			((ExploreCoopAgent)this.myAgent).setMyMap(this.myMap);
			this.shareMapBehaviour= new ShareMapBehaviour(this.myAgent,500,myMap,list_agentNames,agents_submaps);
			this.myAgent.addBehaviour(this.shareMapBehaviour);
		}

		if(this.agents_submaps.isEmpty()){
			for (String agent : list_agentNames) {
				MapRepresentation emptyMap = new MapRepresentation(false);
				this.agents_submaps.add(emptyMap);
			}
		}

		// get agents current position to explore neighbors
		Location myPosition=((AbstractDedaleAgent)this.myAgent).getCurrentPosition();

		if (myPosition!=null){
			// list of observable from the agent's current position
			List<Couple<Location,List<Couple<Observation,Integer>>>> lobs=((AbstractDedaleAgent)this.myAgent).observe();//myPosition

			try {
				this.myAgent.doWait(500);
			} catch (Exception e) {
				e.printStackTrace();
			}

			// creating temp map for the new explored nodes, it will be updated each time we update myMap
			MapRepresentation tempMap= new MapRepresentation(false);

			// remove the current node from openlist and add it to closedNodes.
			this.myMap.addNode(myPosition.getLocationId(), MapAttribute.closed);
			tempMap.addNode(myPosition.getLocationId(), MapAttribute.closed);


			// get the surrounding nodes and, if not in closedNodes, add them to open nodes.
			String nextNodeId=null;
			Iterator<Couple<Location, List<Couple<Observation, Integer>>>> iter=lobs.iterator();
			while(iter.hasNext()){
				Location accessibleNode=iter.next().getLeft();
				boolean isNewNode=this.myMap.addNewNode(accessibleNode.getLocationId());
				tempMap.addNewNode(accessibleNode.getLocationId());

				// if accessibleNode is not current position we add an edge between them
				if (!Objects.equals(myPosition.getLocationId(), accessibleNode.getLocationId())) {
					this.myMap.addEdge(myPosition.getLocationId(), accessibleNode.getLocationId());
					tempMap.addEdge(myPosition.getLocationId(), accessibleNode.getLocationId());
					if (nextNodeId==null && isNewNode) {
						nextNodeId = accessibleNode.getLocationId();
					}
				}
			}

			// merge new temp map with all agents submaps
			for(MapRepresentation submap: this.agents_submaps){
				submap.mergeMap(tempMap.getSerializableGraph());
			}

			// if no more open node, exploration finished
			if (!this.myMap.hasOpenNode()){
				// before deleting the behaviour close my current position to everyone in range
				MapRepresentation currentPosSM = new MapRepresentation(false);
				currentPosSM.addNode(myPosition.getLocationId(), MapAttribute.closed);
				ACLMessage message = new ACLMessage(CustomPerformatives.SUBMAP);
				try {
					message.setContentObject( currentPosSM.getSerializableGraph());
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
				for (String agentName : list_agentNames) {
					message.addReceiver(new AID(agentName,AID.ISLOCALNAME));
				}
				myAgent.send(message);
				finished=true;
				this.shareMapBehaviour.stop();
				((ExploreCoopAgent) this.myAgent).setMapDiscovered(true);
				logMessage("Exploration successufully done, behaviour removed.");
			}else{
				// selecting next node to visit
				// if no directly accessible node, find the closest one
				if (nextNodeId==null){
					nextNodeId=this.myMap.getShortestPathToClosestOpenNode(myPosition.getLocationId()).get(0);
				}

				// in case next node is blocked by another agent, we try to move to a random node until unblocked
				while(!((AbstractDedaleAgent)this.myAgent).moveTo(new gsLocation(nextNodeId))){
					this.myAgent.doWait(10);
					List<Couple<Location,List<Couple<Observation,Integer>>>> possiblePositions=((AbstractDedaleAgent)this.myAgent).observe();
					Random rand = new Random();
					nextNodeId = possiblePositions.get(rand.nextInt(possiblePositions.size())).getLeft().getLocationId();
				}
			}

		}
	}

	private void logMessage(String message) {
		System.out.println(myAgent.getLocalName() + " - " + message);
	}


	@Override
	public boolean done() {
		return finished;
	}

}
