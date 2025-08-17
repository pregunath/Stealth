package Game;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

public class GuardVariant {
    public enum GuardType { STANDING, MOVING }

    private double x, y;
    private final double width = 32;
    private final double height = 32;
    private final double standingRadius = 100;
    private final double movingRadius = 60;
    private final double movingSpeed = 1.5;
    
    private double originalX, originalY;
    private boolean isDistracted = false;
    private long distractionStartTime;
    private static final long DISTRACTION_DURATION = 3000; // 3 seconds
    
    
    private static final double FIELD_OF_VIEW = 90;
    private static final double DIRECT_VIEW_ANGLE = 30;
    
    private GuardType type;
    private final LevelGenerator level;
    private final Random rand;
    
    private List<int[]> path = new ArrayList<>();
    private int currentPathIndex = 0;
    private long idleUntil = 0;
    
    private final Image idleImage;
    private final Image runImage;
    private Image currentImage;

    public GuardVariant(LevelGenerator level, GuardType type) {
        this.level = level;
        this.type = type;
        this.rand = new Random();
        
        this.originalX = x;
        this.originalY = y;
        
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
            idleUntil = now + rand.nextInt(2000) + 1000;
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
    
    
    public void distract(double targetX, double targetY) {
        if (type == GuardType.STANDING) {
            this.type = GuardType.MOVING;
            this.isDistracted = true;
            this.distractionStartTime = System.currentTimeMillis();
            
            // Find path to target location
            int startX = (int)(x / LevelGenerator.TILE_SIZE);
            int startY = (int)(y / LevelGenerator.TILE_SIZE);
            int targetTileX = (int)(targetX / LevelGenerator.TILE_SIZE);
            int targetTileY = (int)(targetY / LevelGenerator.TILE_SIZE);
            
            this.path = Pathfinder.findPath(level, startX, startY, targetTileX, targetTileY);
            this.currentPathIndex = 0;
        }
    }

    private void findNewPath(LevelGenerator level) {
        int guardTileX = (int) (x / LevelGenerator.TILE_SIZE);
        int guardTileY = (int) (y / LevelGenerator.TILE_SIZE);

            // Original pathfinding for stealth mode
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

        double radius = (type == GuardType.STANDING) ? standingRadius : movingRadius;
        Color visionColor = (type == GuardType.STANDING) ? 
            Color.rgb(255, 0, 0, 0.2) : Color.rgb(255, 165, 0, 0.2);
        
        gc.setFill(visionColor);
        gc.fillOval(centerX - radius, centerY - radius, radius * 2, radius * 2);

        gc.drawImage(currentImage, x, y, width, height);
    }

    public boolean canSee(Player player, List<CherryBombEffect> cherryBombs) {
        if (player == null || player.isHidden()) return false;
        
        for (CherryBombEffect bomb : cherryBombs) {
            if (bomb.affectsGuard(this)) {
                if (type == GuardType.STANDING) {
                    return false;
                } else {
                    double effectiveRadius = getVisionRadius() * 0.5;
                    double distanceSq = Math.pow(player.getX() - (x + width/2), 2) + 
                                      Math.pow(player.getY() - (y + height/2), 2);
                    if (distanceSq > effectiveRadius * effectiveRadius) {
                        return false;
                    }
                }
            }
        }
        
        double guardCenterX = x + width/2;
        double guardCenterY = y + height/2;
        double playerCenterX = player.getX();
        double playerCenterY = player.getY();
        
        if (level == null) return false;
        
        double dx = playerCenterX - guardCenterX;
        double dy = playerCenterY - guardCenterY;
        double distanceSq = dx * dx + dy * dy;
        
        double radius = getVisionRadius();
        
        if (player.isSneakyClass()) {
            radius *= 0.8;
        }
        
        if (distanceSq > radius * radius) {
            return false;
        }
        
        if (getDetectionAngleFactor(player) <= 0) {
            return false;
        }
        
        int steps = (int)(Math.sqrt(distanceSq) / 4);
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
        if (player == null) return 0;
        
        double guardCenterX = x + width/2;
        double guardCenterY = y + height/2;
        
        double facingAngle;
        if (type == GuardType.STANDING) {
            facingAngle = System.currentTimeMillis() % 360;
        } else {
            if (path != null && !path.isEmpty() && currentPathIndex < path.size()) {
                int[] target = path.get(currentPathIndex);
                double targetX = target[0] * LevelGenerator.TILE_SIZE;
                double targetY = target[1] * LevelGenerator.TILE_SIZE;
                facingAngle = Math.toDegrees(Math.atan2(targetY - guardCenterY, targetX - guardCenterX));
            } else {
                facingAngle = 0;
            }
        }
        
        double angleToPlayer = Math.toDegrees(Math.atan2(
            player.getY() - guardCenterY, 
            player.getX() - guardCenterX
        ));
        
        double angleDiff = Math.abs(normalizeAngle(angleToPlayer - facingAngle));
        
        if (angleDiff > FIELD_OF_VIEW/2) {
            return 0;
        } else if (angleDiff <= DIRECT_VIEW_ANGLE/2) {
            return 1.0;
        } else {
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

    public double getX() { return x; }
    public double getY() { return y; }
    public double getWidth() { return width; }
    public double getHeight() { return height; }
}