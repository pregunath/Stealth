package Game;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class CherryBombEffect {
    private double x, y;
    private long startTime;
    private boolean active = true;
    private double radius;
    
    public CherryBombEffect(double x, double y) {
        this.x = x;
        this.y = y;
        this.startTime = System.currentTimeMillis();
        this.radius = 100;
    }
    
    public void update() {
        if (System.currentTimeMillis() - startTime > 5000) {
            active = false;
        }
    }
    
    public void render(GraphicsContext gc) {
        double progress = (System.currentTimeMillis() - startTime) / 5000.0;
        double alpha = 0.6 * (1 - progress);
        
        gc.setFill(Color.rgb(255, 100, 100, alpha));
        gc.fillOval(x - radius, y - radius, radius * 2, radius * 2);
        
        gc.setStroke(Color.rgb(200, 50, 50, alpha * 0.8));
        gc.setLineWidth(1);
        gc.strokeOval(x - radius, y - radius, radius * 2, radius * 2);
    }
    
    public boolean isActive() { 
        return active; 
    }
    
    public boolean affectsGuard(GuardVariant guard) {
        if (!active) return false;
        
        double dx = guard.getX() - x;
        double dy = guard.getY() - y;
        double distanceSq = dx * dx + dy * dy;
        
        return distanceSq <= (radius * radius);
    }
}