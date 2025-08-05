package Game;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import java.util.List;
import java.util.ArrayList;
import java.util.Random;

public class GuardVariant {
    public enum GuardType { STANDING, MOVING }

    private double x, y;
    private final double width = 32;
    private final double height = 32;
    private final double standingRadius = 100;
    private final double movingRadius = 60;
    private final double movingSpeed = 1.5;
    
    private static final double FIELD_OF_VIEW = 90; // degrees
    private static final double DIRECT_VIEW_ANGLE = 30; // degrees for full detection
    
    private GuardType type;
    private final LevelGenerator level;
    private final Random rand;
    
    // For moving guards
    private List<int[]> path = new ArrayList<>();
    private int currentPathIndex = 0;
    private long idleUntil = 0;
    
    // Visuals
    private final Image idleImage;
    private final Image runImage;
    private Image currentImage;

    public GuardVariant(LevelGenerator level, GuardType type) {
        this.level = level;
        this.type = type;
        this.rand = new Random();
        
        int[] pos = level.getRandomFloorPosition();
        this.x = pos[0] * LevelGenerator.TILE_SIZE;
        this.y = pos[1] * LevelGenerator.TILE_SIZE;
        
        this.idleImage = new Image(getClass().getResourceAsStream("/sprites/enemyidle.gif"));
        this.runImage = new Image(getClass().getResourceAsStream("/sprites/enemyrun.gif"));
        this.currentImage = idleImage;
    }

    public void update(LevelGenerator level) {
        if (type == GuardType.STANDING) return;
        
        long now = System.currentTimeMillis();
        
        if (now < idleUntil) {
            currentImage = idleImage;
            return;
        }

        if (path == null || path.isEmpty() || currentPathIndex >= path.size()) {
            findNewPath(level);
            idleUntil = now + rand.nextInt(2000) + 1000; // Idle for 1-3 sec before next path
            return;
        }

        int[] targetTile = path.get(currentPathIndex);
        double targetX = targetTile[0] * LevelGenerator.TILE_SIZE;
        double targetY = targetTile[1] * LevelGenerator.TILE_SIZE;

        double dx = targetX - x;
        double dy = targetY - y;
        double dist = Math.sqrt(dx * dx + dy * dy);

        if (dist < 2) {
            currentPathIndex++;
        } else {
            double moveX = movingSpeed * dx / dist;
            double moveY = movingSpeed * dy / dist;

            double newX = x + moveX;
            double newY = y + moveY;

            if (canMoveTo(newX, y, level)) x = newX;
            if (canMoveTo(x, newY, level)) y = newY;

            currentImage = runImage;
        }
    }

    private void findNewPath(LevelGenerator level) {
        int guardTileX = (int) (x / LevelGenerator.TILE_SIZE);
        int guardTileY = (int) (y / LevelGenerator.TILE_SIZE);

        int attempts = 0;
        List<int[]> newPath = null;

        while (attempts < 50) {
            int[] newPos = level.getRandomFloorPosition();
            newPath = Pathfinder.findPath(level, guardTileX, guardTileY, newPos[0], newPos[1]);
            if (!newPath.isEmpty()) break;
            attempts++;
        }

        if (newPath != null && !newPath.isEmpty()) {
            this.path = newPath;
            this.currentPathIndex = 0;
        } else {
            this.path = new ArrayList<>();
        }
    }

    private boolean canMoveTo(double x, double y, LevelGenerator level) {
        if (x < 0 || x + width > level.WIDTH * LevelGenerator.TILE_SIZE ||
            y < 0 || y + height > level.HEIGHT * LevelGenerator.TILE_SIZE) {
            return false;
        }

        int[] checkX = {
            (int) (x / LevelGenerator.TILE_SIZE),
            (int) ((x + width - 1) / LevelGenerator.TILE_SIZE)
        };
        int[] checkY = {
            (int) (y / LevelGenerator.TILE_SIZE),
            (int) ((y + height - 1) / LevelGenerator.TILE_SIZE)
        };

        for (int cx : checkX) {
            for (int cy : checkY) {
                if (!level.isWalkable(cx, cy) && level.getTile(cx, cy) != LevelGenerator.EXIT) {
                    return false;
                }
            }
        }
        return true;
    }

    public void render(GraphicsContext gc) {
        double centerX = x + width / 2;
        double centerY = y + height / 2;

        // Draw vision radius
        double radius = (type == GuardType.STANDING) ? standingRadius : movingRadius;
        Color visionColor = (type == GuardType.STANDING) ? 
            Color.rgb(255, 0, 0, 0.2) : Color.rgb(255, 165, 0, 0.2);
        
        gc.setFill(visionColor);
        gc.fillOval(centerX - radius, centerY - radius, radius * 2, radius * 2);

        // Draw guard
        gc.drawImage(currentImage, x, y, width, height);
    }

    public boolean canSee(Player player) {
        if (player.isHidden()) return false;
        
        double guardCenterX = x + width/2;
        double guardCenterY = y + height/2;
        double playerCenterX = player.getX();
        double playerCenterY = player.getY();
        
        double dx = playerCenterX - guardCenterX;
        double dy = playerCenterY - guardCenterY;
        double distanceSquared = dx * dx + dy * dy;
        
        double radius = getVisionRadius();
        if (distanceSquared > radius * radius) {
            return false;
        }
        
        // Check if player is in field of view
        if (getDetectionAngleFactor(player) <= 0) {
            return false;
        }
        
        // More precise line of sight check with smaller increments
        int steps = (int)(Math.sqrt(distanceSquared) / 4); // Check every 4 pixels
        if (steps == 0) steps = 1;
        
        for (int i = 1; i <= steps; i++) {
            double t = (double)i / steps;
            double checkX = guardCenterX + dx * t;
            double checkY = guardCenterY + dy * t;
            
            int tileX = (int)(checkX / LevelGenerator.TILE_SIZE);
            int tileY = (int)(checkY / LevelGenerator.TILE_SIZE);
            
            if (!level.isWalkable(tileX, tileY)) {
                return false;
            }
        }
        
        return true;
    }

    public void setPosition(double x, double y) {
        this.x = x;
        this.y = y;
        if (type == GuardType.MOVING) {
            this.path = new ArrayList<>();
            this.currentPathIndex = 0;
        }
    }

    public GuardType getType() {
        return type;
    }
    
    public double getDetectionAngleFactor(Player player) {
        // Calculate angle between guard's facing direction and player
        double guardCenterX = x + width/2;
        double guardCenterY = y + height/2;
        
        // For standing guards, assume they're facing the direction they're looking
        // For moving guards, use their movement direction
        double facingAngle;
        if (type == GuardType.STANDING) {
            // Standing guards look in random directions (simplified)
            facingAngle = System.currentTimeMillis() % 360; // pseudo-random facing
        } else {
            // Moving guards face their movement direction
            if (path != null && !path.isEmpty() && currentPathIndex < path.size()) {
                int[] target = path.get(currentPathIndex);
                double targetX = target[0] * LevelGenerator.TILE_SIZE;
                double targetY = target[1] * LevelGenerator.TILE_SIZE;
                facingAngle = Math.toDegrees(Math.atan2(targetY - guardCenterY, targetX - guardCenterX));
            } else {
                facingAngle = 0; // default if not moving
            }
        }
        
        // Calculate angle to player
        double angleToPlayer = Math.toDegrees(Math.atan2(
            player.getY() - guardCenterY, 
            player.getX() - guardCenterX
        ));
        
        // Normalize angles
        double angleDiff = Math.abs(normalizeAngle(angleToPlayer - facingAngle));
        
        // Calculate detection factor based on angle
        if (angleDiff > FIELD_OF_VIEW/2) {
            return 0; // outside field of view
        } else if (angleDiff <= DIRECT_VIEW_ANGLE/2) {
            return 1.0; // direct view
        } else {
            // Gradual falloff from center to edge of FOV
            return 1.0 - ((angleDiff - DIRECT_VIEW_ANGLE/2) / (FIELD_OF_VIEW/2 - DIRECT_VIEW_ANGLE/2));
        }
    }
    
    private double normalizeAngle(double angle) {
        angle = angle % 360;
        if (angle > 180) {
            angle -= 360;
        } else if (angle < -180) {
            angle += 360;
        }
        return angle;
    }
    
    public double getVisionRadius() {
        return (type == GuardType.STANDING) ? standingRadius : movingRadius;
    }

	public double getX() {
		// TODO Auto-generated method stub
		return x;
	}

	public double getY() {
		// TODO Auto-generated method stub
		return y;
	}

	public double getWidth() {
		// TODO Auto-generated method stub
		return width;
	}
	public double getHeight() {
		// TODO Auto-generated method stub
		return height;
	}
}