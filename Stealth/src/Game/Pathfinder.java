package Game;

import java.util.*;

public class Pathfinder {
    private static class Node implements Comparable<Node> {
        int x, y;
        double f, g, h;
        Node parent;

        Node(int x, int y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public int compareTo(Node other) {
            return Double.compare(this.f, other.f);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            Node node = (Node) obj;
            return x == node.x && y == node.y;
        }

        @Override
        public int hashCode() {
            return Objects.hash(x, y);
        }
    }

    public static List<int[]> findPath(LevelGenerator level, int startX, int startY, int targetX, int targetY) {
        PriorityQueue<Node> openSet = new PriorityQueue<>();
        Set<Node> closedSet = new HashSet<>();
        Map<Node, Node> cameFrom = new HashMap<>();

        Node startNode = new Node(startX, startY);
        Node targetNode = new Node(targetX, targetY);

        startNode.g = 0;
        startNode.h = heuristic(startNode, targetNode);
        startNode.f = startNode.g + startNode.h;
        openSet.add(startNode);

        while (!openSet.isEmpty()) {
            Node current = openSet.poll();

            if (current.equals(targetNode)) {
                return reconstructPath(cameFrom, current);
            }

            closedSet.add(current);

            for (Node neighbor : getNeighbors(level, current)) {
                if (closedSet.contains(neighbor)) continue;

                double tentativeG = current.g + 1; // Assuming each step costs 1

                if (!openSet.contains(neighbor) || tentativeG < neighbor.g) {
                    neighbor.parent = current;
                    neighbor.g = tentativeG;
                    neighbor.h = heuristic(neighbor, targetNode);
                    neighbor.f = neighbor.g + neighbor.h;

                    if (!openSet.contains(neighbor)) {
                        openSet.add(neighbor);
                    }
                }
            }
        }

        return Collections.emptyList(); // No path found
    }

    private static List<int[]> reconstructPath(Map<Node, Node> cameFrom, Node current) {
        List<int[]> path = new ArrayList<>();
        path.add(new int[]{current.x, current.y});

        while (cameFrom.containsKey(current)) {
            current = cameFrom.get(current);
            path.add(0, new int[]{current.x, current.y});
        }

        return path;
    }

    private static List<Node> getNeighbors(LevelGenerator level, Node node) {
        List<Node> neighbors = new ArrayList<>();
        int[] dx = {-1, 1, 0, 0}; // 4-directional movement
        int[] dy = {0, 0, -1, 1};

        for (int i = 0; i < 4; i++) {
            int nx = node.x + dx[i];
            int ny = node.y + dy[i];

            if (nx >= 0 && nx < LevelGenerator.WIDTH && 
                ny >= 0 && ny < LevelGenerator.HEIGHT && 
                level.isWalkable(nx, ny)) {
                neighbors.add(new Node(nx, ny));
            }
        }

        return neighbors;
    }

    private static double heuristic(Node a, Node b) {
        // Manhattan distance
        return Math.abs(a.x - b.x) + Math.abs(a.y - b.y);
    }
}