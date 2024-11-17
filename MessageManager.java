import java.util.ArrayList;

public class MessageManager {
    private ArrayList<Message> messagesList;


    public MessageManager() {
        this.messagesList = new ArrayList<>();
    }

    public ArrayList<Message> getMessagesList() {
        return messagesList;
    }

    public void addMessage(Message message) {
        messagesList.add(message);
    }


    public int getMessagesCount() {
        return messagesList.size();
    }


    public void clearAllMessages() {
        messagesList.clear();
    }


    public void searchMessagesBySender(String senderID) {

        for (Message message : messagesList) {
            if (message.getSenderID().equals(senderID)) {
                System.out.println(message);
            }
        }

    }
    public void deleteMessagesByContact(String contactID) {
        messagesList.removeIf(message ->
                message.getSenderID().equals(contactID) || message.getReceiverID().equals(contactID));
        System.out.println("Messages Deleted Successfully");
    }


    public boolean markMessageAsRead(int messageIndex) {
        if (messageIndex >= 0 && messageIndex < messagesList.size()) {
            messagesList.get(messageIndex).setMessageStatus(Status.READ);
            return true;
        }
        return false;
    }
    public void displayAllMessages(){
        for (Message message : messagesList) {
            System.out.println(message);
            message.setMessageStatus(Status.READ);
        }

    }

    public void markMessagesAsRead(){
        for (Message message : messagesList) {
            message.setMessageStatus(Status.READ);
        }
    }
    public void displayMessagesForContact(String contactID) {
        for (Message message : messagesList) {
            if (message.getSenderID().equals(contactID) || message.getReceiverID().equals(contactID)) {
                System.out.println(message);
            }
        }
    }
}
