// SneakyGear.java
package Game;

public class SneakyGear extends Gear {
    public SneakyGear(String type) {
        switch(type) {
            case "Cloak":
                this.name = "Shadow Cloak";
                this.description = "Reduces visibility range by 20%";
                break;
            case "Boots":
                this.name = "Silent Boots";
                this.description = "Makes footsteps completely silent, and allows the user to stay in the guard's cone of vision for a certain time.";
                break;
            case "Gloves":
                this.name = "Thief's Gloves";
                this.description = "Allows faster hiding/unhiding";
                break;
        }
    }

    @Override
    public void applyEffect(Player player) {
        // Effects will be applied in Player class
    }
}