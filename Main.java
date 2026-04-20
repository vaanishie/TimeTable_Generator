/* Main file : to be run to start the program
 
 The only job of this class is to hand control over to the 
 UI on Swing's Event Dispatch Thread (EDT).  EDT wo thread hai jo pura GUI handle karta hai 
 aur user ke actions process karta hai without breaking the app.
 All UI work must happen on the EDT to avoid race conditions.
 */

public class Main {
    public static void main(String[] args) {
        javax.swing.SwingUtilities.invokeLater(() -> new UI().show());
    }
}
