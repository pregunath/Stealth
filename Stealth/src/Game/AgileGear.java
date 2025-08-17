// AgileGear.java
package Game;

public class AgileGear extends Gear {
    public AgileGear(String type) {
        switch(type) {
            case "Boots":
                this.name = "Swift Boots";
                this.description = "Increases movement speed by 25% (stacks with class bonus)";
                break;
            case "Gloves":
                this.name = "Grip Gloves";
                this.description = "Allows climbing certain walls";
                break;
        }
    }

    @Override
    public void applyEffect(Player player) {
        // Effects will be applied in Player class
    }
}