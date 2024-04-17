package eu.su.mas.dedaleEtu.mas.behaviours;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.io.IOException;
import java.io.Serializable;

public class SendMessageBehaviour  extends OneShotBehaviour {

    private AID recipientAgent;
    private Object messageContent;

    private int performative;

    // Constructor for sending a string message
    public SendMessageBehaviour(AID agent, String messageContent, int performative) {
        this.recipientAgent = agent;
        this.messageContent = messageContent;
        this.performative = performative;
    }

    // Constructor for sending an object message
    public SendMessageBehaviour(AID agent, Object messageContent, int performative) {
        this.recipientAgent = agent;
        this.messageContent = messageContent;
        this.performative = performative;
    }

    public void action() {
        ACLMessage message = new ACLMessage(performative);

        try {
            if (messageContent instanceof String) {
                message.setContent((String) messageContent);
            } else {

                    message.setContentObject((Serializable) messageContent);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        message.addReceiver(recipientAgent);

        myAgent.send(message);
    }
}