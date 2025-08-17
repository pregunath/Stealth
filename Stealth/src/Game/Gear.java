// Gear.java
package Game;

public abstract class Gear {
    protected String name;
    protected String description;
    
    public abstract void applyEffect(Player player);
    
    public String getName() {
        return name;
    }
    
    public String getDescription() {
        return description;
    }
}