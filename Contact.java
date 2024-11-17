public class Contact {
    private String contactID;
    private String contactName;

    // Constructor
    public Contact(String contactID, String contactName) {
        this.contactID = contactID;
        this.contactName = contactName;
    }

    // Getters and setters
    public String getContactID() {
        return contactID;
    }

    public void setContactID(String contactID) {
        this.contactID = contactID;
    }

    public String getContactName() {
        return contactName;
    }

    public void setContactName(String contactName) {
        this.contactName = contactName;
    }

    // Overriding toString for better readability
    @Override
    public String toString() {
        return String.format("Contact ID: %s | Name: %s", contactID, contactName);
    }
}
