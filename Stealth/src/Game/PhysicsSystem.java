package Game;

import java.util.Map;

import javafx.scene.input.KeyCode;

public interface PhysicsSystem {
    void update(Player player, LevelGenerator level);
    void handleInput(Player player, Map<KeyCode, Boolean> keys, LevelGenerator level);
}